param (
    [Alias("sd", "skip-db")][switch]$skipdb = $false,
    [Alias("sp", "skip-pki")][switch]$skippki = $false,
    [Alias("u", "unattended")][switch]$IsUnattended = $false,
    [Alias("h", "help")][switch]$ShowHelp = $false,
    [Alias("aa", "aca-alg")][string]$AcaAlg,
    [Alias("ta", "ta-alg")][string]$TlsAlg,
    [Alias("da", "db-alg")][string]$DbAlg
)

if ($ShowHelp) {
    Write-Host "  Setup script for the HIRS ACA on Windows"
    Write-Host "  Syntax (short form): .\aca_setup.ps1 [-u|-h|-sp|-sb|-aa|-ta|-da]"
    Write-Host "  Syntax (long form): .\aca_setup.ps1 [--unattended|--help|--skip-db|--skip-pki|--aca-alg|--tls-alg|--db-alg]"
    Write-Host "  Flag options:"
    Write-Host "     [-u  | -unattended] Runs the script unattended."
    Write-Host "     [-h  | -help]   Prints this help message."
    Write-Host "     [-sp | -skip-pki] Skips the pki setup of the setup script."
    Write-Host "     [-sb | -skip-db] Skips the database setup of the setup script."
    Write-Host "     [-aa | -aca-alg] Sets the ACA's default algorithm (rsa, ecc, or mldsa) for Attestation Certificates."
    Write-Host "     [-ta | -tls-alg] Sets the ACA's default algorithm (rsa, ecc, or mldsa) for TLS on the ACA portal."
    Write-Host "     [-da | -db-alg] Sets the ACA's default algorithm (rsa, ecc, or mldsa) for use with MariaDB."
    exit 1
}

if (!(New-Object Security.Principal.WindowsPrincipal(
[Security.Principal.WindowsIdentity]::GetCurrent())
).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
    Write-Host "This script requires root.  Please run as root"
    exit 1
}

# Set default algorithms to "RSA" if none provided
if (-not $AcaAlg) {
    $AcaAlg = "rsa"
    Write-Host "Using default algorithm ($AcaAlg) for Attestation Certs."
}

if (-not $TlsAlg) {
    $TlsAlg = "rsa"
    Write-Host "Using default algorithm ($TlsAlg) for the ACA portal."
}

if (-not $DbAlg) {
    $DbAlg = "rsa"
    Write-Host "Using default algorithm ($DbAlg) for the Database."
}

# Define valid options
$validAlgs = @("rsa", "ecc")

# Check for valid algorithms
if ($validAlgs -notcontains $AcaAlg) {
    Write-Host "Invalid ACA algorithm $AcaAlg specified. Valid options are rsa or ecc."
    exit 1
}

if ($validAlgs -notcontains $TlsAlg) {
    Write-Host "Invalid TLS algorithm $TlsAlg specified. Valid options are rsa or ecc."
    exit 1
}

if ($validAlgs -notcontains $DbAlg) {
    Write-Host "Invalid DB algorithm $DbAlg specified. Valid options are rsa or ecc."
    exit 1
}

$ACA_SCRIPTS_HOME = (Split-Path -parent $PSCommandPath)
$ACA_COMMON_SCRIPT = (Join-Path $ACA_SCRIPTS_HOME 'aca_common.ps1')
$COMP_JSON = (Resolve-Path ([System.IO.Path]::Combine(
        $ACA_SCRIPTS_HOME, '..', '..', '..', 'HIRS_AttestationCA', 'src', 'main', 'resources', 'component-class.json'))).Path
$VENDOR_TABLE = (Resolve-Path ([System.IO.Path]::Combine(
        $ACA_SCRIPTS_HOME, '..', '..', '..', 'HIRS_Utils', 'src', 'main', 'resources', 'vendor-table.json'))).Path
$SPRING_PROPERTIES_FILE = (Resolve-Path ([System.IO.Path]::Combine(
        $ACA_SCRIPTS_HOME, '..', '..', '..', 'HIRS_AttestationCAPortal', 'src', 'main', 'resources', 'application.win.properties'))).Path

# Load other scripts
. $ACA_COMMON_SCRIPT

# Set up log
set_up_log

Write-Output "HIRS ACA Setup initiated on $( Get-Date -Format 'yyyy-MM-dd' )" | WriteAndLog

Write-Output "-----------------------------------------------------------" | WriteAndLog
Write-Output "ACA setup log file is $global:LOG_FILE" | WriteAndLog
Write-Output ("Running with these arguments: " + ($PSBoundParameters | Out-String)) | WriteAndLog

Write-Output "Setting up the VERSION file that the bootRun can use" | WriteAndLog

# Check if git is available
if (Get-Command git -ErrorAction SilentlyContinue) {
    # Check if we're inside a git working tree
    $isInsideWorkTree = git rev-parse --is-inside-work-tree 2> $null

    if ($LASTEXITCODE -eq 0 -and $isInsideWorkTree.Trim() -eq "true") {
        # Read the VERSION file
        $version = Get-Content $global:HIRS_RELEASE_VERSION_FILE -Raw

        # Get the current Unix timestamp
        $timestamp = [int][DateTimeOffset]::UtcNow.ToUnixTimeSeconds()

        $gitHash = git rev-parse --short HEAD

        $jarVersion = "$($version.Trim() ).$timestamp.$($gitHash.Trim() )"

        if (-not (Test-Path $global:HIRS_DATA_WIN_VERSION_FILE)) {
            New-Item -ItemType File -Path $global:HIRS_DATA_WIN_VERSION_FILE -Force | Out-Null
        }

        $jarVersion | Set-Content $global:HIRS_DATA_WIN_VERSION_FILE
    }
}

# Read aca.properties
New-Item -ItemType Directory -Path $global:HIRS_CONF_DIR -Force | Out-Null
New-Item -ItemType Directory -Path $global:HIRS_CONF_DEFAULT_PROPERTIES_DIR -Force | Out-Null
New-Item -ItemType Directory -Path $global:HIRS_DATA_LOG_DIR -Force | Out-Null
Copy-Item $COMP_JSON $global:HIRS_CONF_DEFAULT_PROPERTIES_DIR
Copy-Item $VENDOR_TABLE $global:HIRS_CONF_DEFAULT_PROPERTIES_DIR
Copy-Item $SPRING_PROPERTIES_FILE $global:HIRS_DATA_SPRING_PROP_FILE

# create it, if it doesn't exist
if (-not (Test-Path $global:HIRS_DATA_ACA_PROPERTIES_FILE)) {
    New-Item -ItemType File -Path $global:HIRS_DATA_ACA_PROPERTIES_FILE
}
else {
    Write-Output "File already exists: $global:HIRS_DATA_ACA_PROPERTIES_FILE" | WriteAndLog
}

read_aca_properties $global:HIRS_DATA_ACA_PROPERTIES_FILE

# Read spring application.properties

# create it, if it doesn't exist
if (-not (Test-Path $global:HIRS_DATA_SPRING_PROP_FILE)) {
    New-Item -ItemType File -Path $global:HIRS_DATA_SPRING_PROP_FILE
}
else {
    Write-Output "File already exists: $global:HIRS_DATA_SPRING_PROP_FILE" | WriteAndLog
}

read_spring_properties -file "$global:HIRS_DATA_SPRING_PROP_FILE"

# Runs the pki_setup script (along with the other scripts under the PKI folder)
if (!$skippki) {
    if (!$Env:HIRS_PKI_PWD) {
        $HIRS_PKI_PWD = (create_random)
        # NOTE: Writing to the environment variable did not work within the container
        # This password will be stored in the ACA properties file.
        Write-Output "Using randomly generated password for the PKI key password" | WriteAndLog
        Write-Host "NOT LOGGED: Using pki password=$HIRS_PKI_PWD"
    }
    else {
        $HIRS_PKI_PWD = $Env:HIRS_PKI_PWD
        Write-Output "Using system supplied password for the PKI key password" | WriteAndLog
    }
    pwsh -ExecutionPolicy Bypass $global:HIRS_REL_WIN_PKI_SETUP -LOG_FILE "$global:LOG_FILE" -PKI_PASS "$HIRS_PKI_PWD"
    if ($LastExitCode -eq 0) {
        Write-Output "ACA PKI setup complete" | WriteAndLog
    }
    else {
        Write-Output "Error setting up ACA PKI" | WriteAndLog
        exit 1
    }
}
else {
    Write-Output ("ACA PKI setup cannot be run because there are command line argument(s): " + ($PSBoundParameters.Keys | Where-Object { $_ -match 'skip-pki|sp' })) | WriteAndLog
}

# Runs the create_db script (along with the other scripts under the DB folder)
if (!$skipdb) {
    pwsh -ExecutionPolicy Bypass $global:HIRS_REL_WIN_DB_CREATE -LOG_FILE "$global:LOG_FILE" -UNATTENDED:$IsUnattended -DB_ALG "$DbAlg"
    if ($LastExitCode -eq 0) {
        Write-Output "ACA database setup complete" | WriteAndLog
    }
    else {
        Write-Output "Error setting up ACA DB" | WriteAndLog
        exit 1
    }
}
else {
    Write-Output ("ACA Database setup cannot be run because there are command line argument(s): " + ($PSBoundParameters.Keys | Where-Object { $_ -match 'skip-db|sd' })) | WriteAndLog
}

Function setup_aca_public_key_algorithm() {
    param(
        [Parameter(Mandatory = $true)]
        [string]$acaAlg
    )

    if (-not $acaAlg) {
        Write-Output "Exiting script while attempting to set the ACA's public key algorithm since the provided ACA Public Key Algorithm
         do not exist/have not been supplied" | WriteAndLog
        exit 1
    }

    $aca_leaf_cert_val = $null
    $aca_intermediate_cert_val = $null
    $aca_root_cert_val = $null

    if ($acaAlg -eq "rsa") {
        $aca_leaf_cert_val = "HIRS_leaf_ca3_rsa_3k_sha384"
        $aca_intermediate_cert_val = "HIRS_intermediate_ca_rsa_3k_sha384"
        $aca_root_cert_val = "HIRS_root_ca_rsa_3k_sha384"
    }
    elseif($acaAlg -eq "ecc") {
        $aca_leaf_cert_val = "HIRS_leaf_ca3_ecc_512_sha384"
        $aca_intermediate_cert_val = "HIRS_intermediate_ca_ecc_512_sha384"
        $aca_root_cert_val = "HIRS_root_ca_ecc_512_sha384"
    }

    remove_spring_property_value_pair -file "$global:HIRS_DATA_SPRING_PROP_FILE" -key "$global:SPRING_PROPERTIES_ACA_LEAF_CERTIFICATE_ALIAS_NAME"
    remove_spring_property_value_pair -file "$global:HIRS_DATA_SPRING_PROP_FILE" -key "$global:SPRING_PROPERTIES_ACA_INTERMEDIATE_CERTIFICATE_ALIAS_NAME"
    remove_spring_property_value_pair -file "$global:HIRS_DATA_SPRING_PROP_FILE" -key "$global:SPRING_PROPERTIES_ACA_ROOT_CERTIFICATE_ALIAS_NAME"

    if (-not (find_property_value -file "$global:HIRS_DATA_SPRING_PROP_FILE" -key "$global:SPRING_PROPERTIES_ACA_LEAF_CERTIFICATE_ALIAS_NAME")) {
        add_new_spring_property -file "$global:HIRS_DATA_SPRING_PROP_FILE" -newKeyAndValue "$global:SPRING_PROPERTIES_ACA_LEAF_CERTIFICATE_ALIAS_NAME=$aca_leaf_cert_val"
        Write-Output "Stored the $acaAlg ACA Leaf certificate alias property name and value in the Spring properties file [$global:HIRS_DATA_SPRING_PROP_FILE]" | WriteAndLog
    }

    if (-not (find_property_value -file "$global:HIRS_DATA_SPRING_PROP_FILE" -key "$global:SPRING_PROPERTIES_ACA_INTERMEDIATE_CERTIFICATE_ALIAS_NAME")) {
        add_new_spring_property -file "$global:HIRS_DATA_SPRING_PROP_FILE" -newKeyAndValue "$global:SPRING_PROPERTIES_ACA_INTERMEDIATE_CERTIFICATE_ALIAS_NAME=$aca_intermediate_cert_val"
        Write-Output "Stored the $acaAlg ACA Intermediate certificate alias property name and value in the Spring properties file [$global:HIRS_DATA_SPRING_PROP_FILE]" | WriteAndLog
    }

    if (-not (find_property_value -file "$global:HIRS_DATA_SPRING_PROP_FILE" -key "$global:SPRING_PROPERTIES_ACA_ROOT_CERTIFICATE_ALIAS_NAME")) {
        add_new_spring_property -file "$global:HIRS_DATA_SPRING_PROP_FILE" -newKeyAndValue "$global:SPRING_PROPERTIES_ACA_ROOT_CERTIFICATE_ALIAS_NAME=$aca_root_cert_val"
        Write-Output "Stored the $acaAlg ACA Root certificate alias property name and value in the Spring properties file [$global:HIRS_DATA_SPRING_PROP_FILE]" | WriteAndLog
    }
}

Function setup_tls_config_aliases() {
    param(
        [Parameter(Mandatory = $true)]
        [string]$tlsAlg
    )

    if (-not $tlsAlg) {
        Write-Output "Exiting script while attempting to set the config file lines for tomcat ssl aliases since the provided TLS
         do not exist/have not been supplied" | WriteAndLog
        exit 1
    }

    $server_ssl_trust_alias_val = $null
    $server_ssl_key_alias_val = $null

    if ($tlsAlg -eq "rsa") {
        $server_ssl_trust_alias_val = "hirs_aca_tls_rsa_3k_sha384"
        $server_ssl_key_alias_val = "hirs_aca_tls_rsa_3k_sha384"
    }
    elseif($tlsAlg -eq "ecc") {
        $server_ssl_trust_alias_val = "hirs_aca_tls_ecc_512_sha384"
        $server_ssl_key_alias_val = "hirs_aca_tls_ecc_512_sha384"
    }

    # remove default SSL config lines
    remove_spring_property_value_pair -file "$global:HIRS_DATA_SPRING_PROP_FILE" -key "$global:SPRING_PROPERTIES_SSL_TRUST_ALIAS_PROPERTY_NAME"
    remove_spring_property_value_pair -file "$global:HIRS_DATA_SPRING_PROP_FILE" -key "$global:SPRING_PROPERTIES_SSL_KEY_ALIAS_PROPERTY_NAME"

    if (-not (find_property_value -file "$global:HIRS_DATA_SPRING_PROP_FILE" -key "$global:SPRING_PROPERTIES_SSL_TRUST_ALIAS_PROPERTY_NAME")) {
        add_new_spring_property -file "$global:HIRS_DATA_SPRING_PROP_FILE" -newKeyAndValue "$global:SPRING_PROPERTIES_SSL_TRUST_ALIAS_PROPERTY_NAME=$server_ssl_trust_alias_val"
        Write-Output "Stored the $tlsAlg SSL Trust Alias property name and value in the Spring properties file [$global:HIRS_DATA_SPRING_PROP_FILE]" | WriteAndLog
    }

    if (-not (find_property_value -file "$global:HIRS_DATA_SPRING_PROP_FILE" -key "$global:SPRING_PROPERTIES_SSL_KEY_ALIAS_PROPERTY_NAME")) {
        add_new_spring_property -file "$global:HIRS_DATA_SPRING_PROP_FILE" -newKeyAndValue "$global:SPRING_PROPERTIES_SSL_KEY_ALIAS_PROPERTY_NAME=$server_ssl_key_alias_val"
        Write-Output "Stored the $tlsAlg SSL Key alias property name and value in the Spring properties file [$global:HIRS_DATA_SPRING_PROP_FILE]" | WriteAndLog
    }
}

# Update properties file based upon algorithm choices
Write-Host "Setting public key algorithm for TLS and ACA..."

# setup the tls configuration using the provided public key algorithm
setup_tls_config_aliases -tlsAlg "$TlsAlg"

# setup the aca using the provided public key algorithm
setup_aca_public_key_algorithm -acaAlg "$AcaAlg"

Write-Output "ACA setup complete" | WriteAndLog
Write-Host "----------------------------------------------------------------------"
Write-Host ""