############################################################################################
# Creates Certificate Chains for the ACA:
# 1 RSA 3K SHA 384
# 2 ECC 512 SHA 384
# 3 ML-DSA-44
# 4 ML-DSA-65
# 5 ML-DSA-87
############################################################################################

param (
    [Parameter(Mandatory = $true)]
    [string]$LOG_FILE,
    [Parameter(Mandatory = $true)]
    [string]$PKI_PASS
)

$ACA_SCRIPTS_HOME = (Split-Path -Parent $PSCommandPath)
$ACA_COMMON_SCRIPT = Join-Path $ACA_SCRIPTS_HOME '..' 'aca' 'aca_common.ps1'

# Load other scripts
. $ACA_COMMON_SCRIPT

# Read aca.properties
read_aca_properties -file "$global:HIRS_DATA_ACA_PROPERTIES_FILE"

# Read spring application.properties
read_spring_properties -file "$global:HIRS_DATA_SPRING_PROP_FILE"

# Parameter check
if (-not (Test-Path -Path $LOG_FILE)) {
    New-Item -ItemType File -Path $LOG_FILE | Out-Null
    $global:LOG_FILE = $LOG_FILE
} else {
    set_up_log
}

if (!$PKI_PASS) {
    if ($Env:HIRS_PKI_PWD) {
        $PKI_PASS = $Env:HIRS_PKI_PWD
    } else {
        $PKI_PASS = (create_random)
        Write-Output "Using randomly generated password for the PKI key password" | WriteAndLog
    }
}

New-Item -ItemType Directory -Path $global:HIRS_CONF_DIR -Force | Out-Null
Write-Output "APP_HOME is $ACA_SCRIPTS_HOME" | WriteAndLog

# Check for root user
if (!(New-Object Security.Principal.WindowsPrincipal(
    [Security.Principal.WindowsIdentity]::GetCurrent())
).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
    Write-Output "This script requires root.  Please run as root" | WriteAndLog
    exit 1
}

# Create Cert Chains
if (![System.IO.Directory]::Exists($global:HIRS_DATA_CERTIFICATES_DIR)) {
    if ([System.IO.Directory]::Exists($global:HIRS_REL_WIN_PKI_HOME)) {
        $PKI_SETUP_DIR = $global:HIRS_REL_WIN_PKI_HOME
    } else {
        $PKI_SETUP_DIR = $ACA_SCRIPTS_HOME
    }
    Write-Output "PKI_SETUP_DIR is $PKI_SETUP_DIR" | WriteAndLog

    New-Item -ItemType Directory -Path $global:HIRS_DATA_CERTIFICATES_DIR -Force | Out-Null
    Copy-Item (Join-Path $PKI_SETUP_DIR 'ca.conf') $global:HIRS_DATA_CERTIFICATES_DIR -ErrorAction Stop

    $CHAIN_GEN_SCRIPT = Join-Path $PKI_SETUP_DIR 'pki_chain_gen.ps1'
    $CHAINS = @(
        @('HIRS', 'rsa',   '3072', 'sha384'),
        @('HIRS', 'ecc',   '512',  'sha384'),
        @('HIRS', 'mldsa', '44',   'none'),
        @('HIRS', 'mldsa', '65',   'none'),
        @('HIRS', 'mldsa', '87',   'none')
    )

    foreach ($chain in $CHAINS) {
        Write-Output "Generating $($chain[1]) $($chain[2]) certificate chain..." | WriteAndLog
        & pwsh -ExecutionPolicy Bypass -File $CHAIN_GEN_SCRIPT $chain[0] $chain[1] $chain[2] $chain[3] $PKI_PASS $global:LOG_FILE
        if ($LASTEXITCODE -ne 0) {
            Write-Output "Certificate chain generation failed for $($chain[1]) $($chain[2]) (exit code $LASTEXITCODE). Aborting PKI setup." | WriteAndLog
            exit 1
        }
    }

    # Save the password to the ACA properties file.
    add_new_aca_property -file "$global:HIRS_DATA_ACA_PROPERTIES_FILE" -newKeyAndValue "$global:ACA_PROPERTIES_PKI_PWD_PROPERTY_NAME=$PKI_PASS"

    # Save connector information to the application properties file.
    add_new_spring_property -file "$global:HIRS_DATA_SPRING_PROP_FILE" -newKeyAndValue "$global:SPRING_PROPERTIES_SSL_KEY_STORE_PWD_PROPERTY_NAME=$PKI_PASS"
    add_new_spring_property -file "$global:HIRS_DATA_SPRING_PROP_FILE" -newKeyAndValue "$global:SPRING_PROPERTIES_SSL_KEY_TRUST_STORE_PWD_PROPERTY_NAME=$PKI_PASS"
} else {
    Write-Output "$global:HIRS_DATA_CERTIFICATES_DIR exists, skipping" | WriteAndLog
}
