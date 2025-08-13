#This script will check to see if the ACA has already been setup in a Windows environment

param (
    [switch]$v, [switch]$verbose = $false,
	[switch]$h, [switch]$help = $false
)

# Parameter Consolidation
$verbose=$v -or $verbose
$help = $h -or $help

if ($help) {
    Write-Output "  aca check setup script for the HIRS ACA on Windows. Script checks if everything has been setup properly after the user has run the aca_setup.ps1 script."
    Write-Output "  Syntax: .\aca_check_setup.ps1 [-v|-h|-verbose|-help]"
    Write-Output "  Flag Options:"
    Write-Output "     [-v  | -verbose]  Enables verbose output"
    Write-Output "     [-h  | -help]   Prints this help message."
	exit 1
}

# if(!(New-Object Security.Principal.WindowsPrincipal(
# 		[Security.Principal.WindowsIdentity]::GetCurrent())
# 	).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
# 	Write-Host "This script requires root.  Please run as root" 
# 	exit 1
# }

$APP_HOME=(Split-Path -parent $PSCommandPath)
$MYSQL_UTIL_SCRIPT= (Resolve-Path ([System.IO.Path]::Combine($APP_HOME, '..',"db", 'mysql_util.ps1'))).Path

Write-Host $MYSQL_UTIL_SCRIPT

# Load other scripts
. $MYSQL_UTIL_SCRIPT

$global:ALL_CHECKS_PASSED=$true
$global:ALL_CERTS_PASSED=$true

$global:LOG_FILE=""
$global:ACA_PROPERTIES_PATH="C:\ProgramData\hirs\aca\aca.properties"
$global:CERT_PATH="C:\ProgramData\hirs\certificates\HIRS\"
$global:RSA_PATH="rsa_3k_sha384_certs"
$global:ECC_PATH="ecc_512_sha384_certs"

$global:RSA_HIRS_ROOT="HIRS_root_ca_rsa_3k_sha384.pem"
$global:RSA_HIRS_INTERMEDIATE="HIRS_intermediate_ca_rsa_3k_sha384.pem"
$global:RSA_HIRS_CA1="HIRS_leaf_ca1_rsa_3k_sha384.pem"
$global:RSA_HIRS_CA2="HIRS_leaf_ca2_rsa_3k_sha384.pem"
$global:RSA_HIRS_CA3="HIRS_leaf_ca3_rsa_3k_sha384.pem"
$global:RSA_TRUST_STORE="HIRS_rsa_3k_sha384_Cert_Chain.pem"
$global:RSA_RIM_SIGNER="HIRS_rim_signer_rsa_3k_sha384.pem"
$global:RSA_DB_CLIENT_CERT="HIRS_db_client_rsa_3k_sha384.pem"
$global:RSA_DN_SRV_CERT="HIRS_db_srv_rsa_3k_sha384.pem"
$global:RSA_WEB_TLS_CERT="HIRS_aca_tls_rsa_3k_sha384.pem"

$global:ECC_HIRS_ROOT="HIRS_root_ca_ecc_512_sha384.pem"
$global:ECC_HIRS_INTERMEDIATE="HIRS_intermediate_ca_ecc_512_sha384.pem"
$global:ECC_HIRS_CA1="HIRS_leaf_ca1_ecc_512_sha384.pem"
$global:ECC_HIRS_CA2="HIRS_leaf_ca2_ecc_512_sha384.pem"
$global:ECC_HIRS_CA3="HIRS_leaf_ca3_ecc_512_sha384.pem"

$global:ECC_TRUST_STORE="HIRS_ecc_512_sha384_Cert_Chain.pem"
$global:ECC_RIM_SIGNER="HIRS_rim_signer_ecc_512_sha384.pem"
$global:ECC_DB_CLIENT_CERT="HIRS_db_client_ecc_512_sha384.pem"
$global:ECC_DN_SRV_CERT="HIRS_db_srv_ecc_512_sha384.pem"
$global:ECC_WEB_TLS_CERT="HIRS_aca_tls_ecc_512_sha384.pem"

$global:DB_CONF=(Resolve-Path ([System.IO.Path]::Combine($Env:ProgramFiles, 'MariaDB 11.1', 'data', 'my.ini'))).Path

Write-Host "Checking HIRS ACA setup on this device..."

Function populate_aca_properties_table{
    if(-not (Test-Path $global:ACA_PROPERTIES_PATH)){
        Write-Host "The ACA property files [$global:CERT_PATH] does not exist. Have you run the aca_setup.ps1 script?"
        return
    }

    # Convert the contents of the aca properties file into a hash table
    return (Get-Content -Path $global:ACA_PROPERTIES_PATH -Raw | ConvertFrom-StringData)
}

Function check_pwds() {
    $PWDS_PRESENT = $true

    $aca_prop_table = populate_aca_properties_table

    if(-not $aca_prop_table){
        Write-Host "The ACA properties file does not exist. There are no passwords set for this setup"
        $PWDS_PRESENT=$false
    }else{
        if (-not $aca_prop_table.ContainsKey("hirs_pki_password") -or $null -eq $aca_prop_table["hirs_pki_password"]) {
            Write-Host "ACA pki password not set"
            $PWDS_PRESENT = $false
        }

        if (-not $aca_prop_table.ContainsKey("hirs_db_username") -or $null -eq $aca_prop_table["hirs_db_username"]) {
            Write-Host "hirs_db username not set"
            $PWDS_PRESENT = $false
        }

        if (-not $aca_prop_table.ContainsKey("hirs_db_password") -or $null -eq $aca_prop_table["hirs_db_password"]){
            Write-Host "hirs_db user password not set"
            $PWDS_PRESENT = $false
        }
    }

    if ($PWDS_PRESENT) {
        Write-Host "All ACA passwords were found"
    } else {
        Write-Host "Error finding the necessary ACA passwords"
        $global:ALL_CHECKS_PASSED=$false
    }
}

Function check_pki() {
    Write-Host "Checking ACA PKI certificates..."

    $aca_prop_table = populate_aca_properties_table

    if(-not (Test-Path $global:CERT_PATH)){
        Write-Host "Directory for pki certificate [$global:CERT_PATH] does not exist. Have you run the aca_setup.ps1 script?"
        $global:ALL_CHECKS_PASSED=$false
        return
    }
    
    Push-Location (Join-Path $global:CERT_PATH $global:RSA_PATH) | Out-Null
    check_cert -TrustStore:$global:RSA_TRUST_STORE -Cert:$global:RSA_HIRS_ROOT
    check_cert -TrustStore:$global:RSA_TRUST_STORE -Cert:$global:RSA_HIRS_INTERMEDIATE
    check_cert -TrustStore:$global:RSA_TRUST_STORE -Cert:$global:RSA_HIRS_CA1
    check_cert -TrustStore:$global:RSA_TRUST_STORE -Cert:$global:RSA_HIRS_CA2
    check_cert -TrustStore:$global:RSA_TRUST_STORE -Cert:$global:RSA_HIRS_CA3
    check_cert -TrustStore:$global:RSA_TRUST_STORE -Cert:$global:RSA_RIM_SIGNER
    check_cert -TrustStore:$global:RSA_TRUST_STORE -Cert:$global:RSA_DN_SRV_CERT
    check_cert -TrustStore:$global:RSA_TRUST_STORE -Cert:$global:RSA_DB_CLIENT_CERT
    check_cert -TrustStore:$global:RSA_TRUST_STORE -Cert:$global:RSA_WEB_TLS_CERT

    Pop-Location | Out-Null
    Push-Location (Join-Path $global:CERT_PATH $global:ECC_PATH) | Out-Null
    check_cert -TrustStore:$global:ECC_TRUST_STORE -Cert:$global:ECC_HIRS_ROOT
    check_cert -TrustStore:$global:ECC_TRUST_STORE -Cert:$global:ECC_HIRS_INTERMEDIATE
    check_cert -TrustStore:$global:ECC_TRUST_STORE -Cert:$global:ECC_HIRS_CA1
    check_cert -TrustStore:$global:ECC_TRUST_STORE -Cert:$global:ECC_HIRS_CA2
    check_cert -TrustStore:$global:ECC_TRUST_STORE -Cert:$global:ECC_HIRS_CA3
    check_cert -TrustStore:$global:ECC_TRUST_STORE -Cert:$global:ECC_RIM_SIGNER
    check_cert -TrustStore:$global:ECC_TRUST_STORE -Cert:$global:ECC_DN_SRV_CERT
    check_cert -TrustStore:$global:ECC_TRUST_STORE -Cert:$global:ECC_DB_CLIENT_CERT
    check_cert -TrustStore:$global:ECC_TRUST_STORE -Cert:$global:ECC_WEB_TLS_CERT
    Pop-Location | Out-Null

    # if the aca properties file does not exist
    if(-not $aca_prop_table){
        Write-Host "Unable to verify the certificates validity using the pki password since the aca properties file does not exist"
        $global:ALL_CHECKS_PASSED=$false
    }else{
        # verify that the hirs_pki_password and assocaited value exist in the aca properties file
        if ($aca_prop_table.ContainsKey("hirs_pki_password") -and $aca_prop_table["hirs_pki_password"]) {
            # retrieve the hirs pki password
            $pkiPassword = $aca_prop_table["hirs_pki_password"]

            # store the path to the trust store
            $keyStorePath = (Join-Path $global:CERT_PATH "TrustStore.jks")

            # Verify that pki password works with the keystore
            keytool -list -keystore $keyStorePath -storepass $pkiPassword | Out-Null

            if($LASTEXITCODE -eq 0){
                Write-Host "The provided HIRS PKI password is correct for the JKS Trust Store File [$keyStorePath]"
            }else{
                Write-Host "The provided HIRS PKI password was not correct for the JKS Trust Store File [$keyStorePath]"
                $global:ALL_CERTS_PASSED = $false
            }
        }
    }

    if($global:ALL_CERTS_PASSED){
        Write-Host "All RSA and ECC certificates under the certificates directory [$global:CERT_PATH] are valid"
    } else{
        Write-Host "Error: There was an error while trying to verify the validity of the RSA and ECC certificates under the certificates directory [$global:CERT_PATH]"
        $global:ALL_CHECKS_PASSED = $false
    }
    
}

Function check_cert() {
    param (
        [Parameter(Mandatory=$true)]
        [string]$TrustStore,
        [Parameter(Mandatory=$true)]
        [string]$Cert
    )

    $result = openssl verify -CAfile $TrustStore $Cert

    if($LASTEXITCODE -ne 0){
        $global:ALL_CHECKS_PASSED=$false
        $global:ALL_CERTS_PASSED=$false
    }

    if($verbose){
        Write-Host "$result"
    }
}

Function check_mysql_setup() {
    # check if mysql is running
    $DB_STATUS = check_mysql

    if(-not $DB_STATUS){
      Write-Host "MySQL is not currently running. Please start MariaDB Service before running this script."
      $global:ALL_CHECKS_PASSED = $false
      return
    }

    $aca_prop_table = populate_aca_properties_table

    if(-not $aca_prop_table){
        Write-Host "The ACA properties file does not exist. There are no passwords set for this setup"
        $global:ALL_CHECKS_PASSED = $false
        return
    }

    # Check DB server/client TLS setup
    if((Select-String -Path $DB_CONF -Pattern "HIRS").Count -lt 1){
        Write-Host "Mysql server [$DB_CONF] is NOT configured for Server Side TLS"
    }else{
        Write-Host "Mysql server [$DB_CONF] is configured for Server Side TLS"
    }

    if($aca_prop_table.ContainsKey("mysql_admin_password") -and $aca_prop_table["mysql_admin_password"]){
        $mysqlPwd = ""

        $mysqlPwd = $aca_prop_table["mysql_admin_password"]

        mysql -u root --password=$mysqlPwd -e "STATUS;" | Out-Null

        if($LASTEXITCODE -eq 0){
            Write-Host "Mysql admin password for the root user has been verified"
        } else {
            Write-Host "Mysql admin password for the root user verification failed!"
            $global:ALL_CHECKS_PASSED = $false
        }
    
    }else{
        Write-Host "Unable to log into mysql since the aca.properties file does not contain the value associated with the mysql_admin_password key"
        $global:ALL_CHECKS_PASSED = $false
    }
}

Function check_db() {
    Write-Host "Checking DB server TLS configuration..."

}

Function check_fips() {
    Write-Host "Checking FIPS mode on this device..."
    #todo
}

# Check if the aca passwords are stored in the aca.properties file
check_pwds

# Check if the pki certificates for both rsa and ecc are valid and located under the C:\ProgramData\hirs\aca\certificates directory
check_pki

# Check if mysql/mariadb is all setup
check_mysql_setup

# Check if the hirs_db has been setup properly
check_db

# Check for fips
check_fips



if($global:ALL_CHECKS_PASSED -eq $true){
    Write-Host "ACA setup checks on Windows have passed!"
} else {
    Write-Host "ACA setup checks on Windows have failed."
}