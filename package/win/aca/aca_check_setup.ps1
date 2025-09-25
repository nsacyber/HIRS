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

if(!(New-Object Security.Principal.WindowsPrincipal(
		[Security.Principal.WindowsIdentity]::GetCurrent())
	).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
	Write-Host "This script requires root.  Please run as root" 
	exit 1
}

Write-Host "----------------------------------------------------------------------"
Write-Host ""
Write-Host "Checking HIRS ACA setup on this device..."

$ACA_SCRIPTS_HOME=(Split-Path -parent $PSCommandPath)
$ACA_COMMON_SCRIPT=(Join-Path $ACA_SCRIPTS_HOME 'aca_common.ps1')

# Load other scripts
. $ACA_COMMON_SCRIPT
. $global:HIRS_REL_WIN_DB_MYSQL_UTIL

$global:ALL_CHECKS_PASSED=$true
$global:ALL_CERTS_PASSED=$true

Function populate_aca_properties_table{
    if(-not (Test-Path $global:HIRS_DATA_ACA_PROPERTIES_FILE)){
        Write-Host "The ACA property files does not exist. Have you run the aca_setup.ps1 script?"
        return
    }

    # Convert the contents of the aca properties file into a hash table
    return (Get-Content -Path $global:HIRS_DATA_ACA_PROPERTIES_FILE -Raw | ConvertFrom-StringData)
}

Function check_pwds() {
    $PWDS_PRESENT = $true

    $aca_prop_table = populate_aca_properties_table

    if(-not $aca_prop_table){
        Write-Host "The ACA properties file does not exist. There are no passwords set for this setup"
        $PWDS_PRESENT=$false
    }else{
        if (-not $aca_prop_table.ContainsKey($global:ACA_PROPERTIES_PKI_PWD_PROPERTY_NAME) -or $null -eq $aca_prop_table[$global:ACA_PROPERTIES_PKI_PWD_PROPERTY_NAME]) {
            Write-Host "ACA pki password not set"
            $PWDS_PRESENT = $false
        }

        if (-not $aca_prop_table.ContainsKey($global:ACA_PROPERTIES_HIRS_DB_USERNAME_PROPERTY_NAME) -or $null -eq $aca_prop_table[$global:ACA_PROPERTIES_HIRS_DB_USERNAME_PROPERTY_NAME]) {
            Write-Host "hirs_db username not set"
            $PWDS_PRESENT = $false
        }

        if (-not $aca_prop_table.ContainsKey($global:ACA_PROPERTIES_HIRS_DB_PWD_PROPERTY_NAME) -or $null -eq $aca_prop_table[$global:ACA_PROPERTIES_HIRS_DB_PWD_PROPERTY_NAME]){
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

    if(-not (Test-Path $global:HIRS_DATA_CERTIFICATES_HIRS_DIR)){
        Write-Host "Directory for pki certificate [$global:HIRS_DATA_CERTIFICATES_HIRS_DIR] does not exist. Have you run the aca_setup.ps1 script?"
        $global:ALL_CHECKS_PASSED=$false
        return
    }
    
    Push-Location $global:HIRS_DATA_CERTIFICATES_HIRS_RSA_PATH | Out-Null
    check_cert -TrustStore:$global:SSL_DB_RSA_CLIENT_CHAIN -Cert:$global:RSA_HIRS_ROOT
    check_cert -TrustStore:$global:SSL_DB_RSA_CLIENT_CHAIN -Cert:$global:RSA_HIRS_INTERMEDIATE
    check_cert -TrustStore:$global:SSL_DB_RSA_CLIENT_CHAIN -Cert:$global:RSA_HIRS_CA1
    check_cert -TrustStore:$global:SSL_DB_RSA_CLIENT_CHAIN -Cert:$global:RSA_HIRS_CA2
    check_cert -TrustStore:$global:SSL_DB_RSA_CLIENT_CHAIN -Cert:$global:RSA_HIRS_CA3
    check_cert -TrustStore:$global:SSL_DB_RSA_CLIENT_CHAIN -Cert:$global:RSA_RIM_SIGNER
    check_cert -TrustStore:$global:SSL_DB_RSA_CLIENT_CHAIN -Cert:$global:SSL_DB_RSA_SRV_CERT
    check_cert -TrustStore:$global:SSL_DB_RSA_CLIENT_CHAIN -Cert:$global:SSL_DB_RSA_CLIENT_CERT
    check_cert -TrustStore:$global:SSL_DB_RSA_CLIENT_CHAIN -Cert:$global:RSA_WEB_TLS_CERT

    Pop-Location | Out-Null
    Push-Location $global:HIRS_DATA_CERTIFICATES_HIRS_ECC_PATH | Out-Null
    check_cert -TrustStore:$global:SSL_DB_ECC_CLIENT_CHAIN -Cert:$global:ECC_HIRS_ROOT
    check_cert -TrustStore:$global:SSL_DB_ECC_CLIENT_CHAIN -Cert:$global:ECC_HIRS_INTERMEDIATE
    check_cert -TrustStore:$global:SSL_DB_ECC_CLIENT_CHAIN -Cert:$global:ECC_HIRS_CA1
    check_cert -TrustStore:$global:SSL_DB_ECC_CLIENT_CHAIN -Cert:$global:ECC_HIRS_CA2
    check_cert -TrustStore:$global:SSL_DB_ECC_CLIENT_CHAIN -Cert:$global:ECC_HIRS_CA3
    check_cert -TrustStore:$global:SSL_DB_ECC_CLIENT_CHAIN -Cert:$global:ECC_RIM_SIGNER
    check_cert -TrustStore:$global:SSL_DB_ECC_CLIENT_CHAIN -Cert:$global:SSL_DB_ECC_SRV_CERT
    check_cert -TrustStore:$global:SSL_DB_ECC_CLIENT_CHAIN -Cert:$global:SSL_DB_ECC_CLIENT_CERT
    check_cert -TrustStore:$global:SSL_DB_ECC_CLIENT_CHAIN -Cert:$global:ECC_WEB_TLS_CERT
    Pop-Location | Out-Null

    # if the aca properties file does not exist
    if(-not $aca_prop_table){
        Write-Host "Unable to verify the certificates validity using the pki password since the aca properties file does not exist"
        $global:ALL_CHECKS_PASSED=$false
    }else{
        # verify that the hirs_pki_password and assocaited value exist in the aca properties file
        if ($aca_prop_table.ContainsKey($global:ACA_PROPERTIES_PKI_PWD_PROPERTY_NAME) -and $aca_prop_table[$global:ACA_PROPERTIES_PKI_PWD_PROPERTY_NAME]) {
            # retrieve the hirs pki password
            $pkiPassword = $aca_prop_table[$global:ACA_PROPERTIES_PKI_PWD_PROPERTY_NAME]

            # store the path to the trust store
            $keyStorePath = (Join-Path $global:HIRS_DATA_CERTIFICATES_HIRS_DIR "TrustStore.jks")

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
        Write-Host "All RSA and ECC certificates under the certificates directory [$global:HIRS_DATA_CERTIFICATES_HIRS_DIR] are valid"
    } else{
        Write-Host "Error: There was an error while trying to verify the validity of the RSA and ECC certificates under the certificates directory [$global:HIRS_DATA_CERTIFICATES_HIRS_DIR]"
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

    if($aca_prop_table.ContainsKey($global:ACA_PROPERTIES_MYSQL_ADMIN_PWD_PROPERTY_NAME) -and $aca_prop_table[$global:ACA_PROPERTIES_MYSQL_ADMIN_PWD_PROPERTY_NAME]){
        $mysqlPwd = ""

        $mysqlPwd = $aca_prop_table[$global:ACA_PROPERTIES_MYSQL_ADMIN_PWD_PROPERTY_NAME]

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

    $aca_prop_table = populate_aca_properties_table

    if(-not $aca_prop_table){
		Write-Host "Unable to create a hash table using the provided aca properties file."
		$global:ALL_CHECKS_PASSED = $false
        return
	}

	if($aca_prop_table.ContainsKey($global:ACA_PROPERTIES_MYSQL_ADMIN_PWD_PROPERTY_NAME) -and $aca_prop_table[$global:ACA_PROPERTIES_MYSQL_ADMIN_PWD_PROPERTY_NAME]){
		$mysql_admin_password = $aca_prop_table[$global:ACA_PROPERTIES_MYSQL_ADMIN_PWD_PROPERTY_NAME]

        # Check if MySQL server-side TLS is enabled
        $sslResult = mysql -u root --password=$mysql_admin_password -e "SHOW VARIABLES LIKE '%have_ssl%'" | Select-String -Pattern "YES"

        if ($sslResult) {
            Write-Host "MySQL Server side TLS is enabled"
        } else {
            Write-Host "Error: MySQL Server side TLS is NOT enabled:"
            $global:ALL_CHECKS_PASSED = $false
        }
    }

    # todo unsure as to why this section is not working properly. The user and password exist in the mysql db

    # if($aca_prop_table.ContainsKey($global:ACA_PROPERTIES_HIRS_DB_PWD_PROPERTY_NAME) -and $aca_prop_table[$global:ACA_PROPERTIES_HIRS_DB_PWD_PROPERTY_NAME]) {
    #     $hirs_db_password = $aca_prop_table[$global:ACA_PROPERTIES_HIRS_DB_PWD_PROPERTY_NAME]

    #     # Check if the hirs_db is visible to the hirs_db user
    #     $dbResult = mysqlshow --user="hirs_db" --password=$hirs_db_password hirs_db -h localhost | Select-String -Pattern "hirs_db"

    #     if ($dbResult) {
    #         Write-Host "The hirs_db database is visible by the hirs_db user"
    #     } else {
    #         Write-Host "Error: The hirs_db database is NOT visible by the hirs_db user"
    #         $global:ALL_CHECKS_PASSED = $false
    #     }
	# }
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

Write-Host "----------------------------------------------------------------------"
Write-Host ""
