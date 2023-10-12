param (
    [string]$p, [string]$path = $null,
	[switch]$w, [switch]$war = $false,
	[switch]$h, [switch]$help = $false
)

$APP_HOME=(Split-Path -parent $PSCommandPath)
$ACA_COMMON_SCRIPT=(Join-Path $APP_HOME 'aca_common.ps1')
$ALG="RSA" # or "ECC"
$GRADLE_WRAPPER='./gradlew'
$DEPLOYED_WAR=$null

# Load other scripts
. $ACA_COMMON_SCRIPT

# Set up log
set_up_log

# Read aca.properties
read_aca_properties $global:HIRS_DATA_ACA_PROPERTIES_FILE

echo "-----------------------------------------------------------" | WriteAndLog
echo ("Running with these arguments: "+($PSBoundParameters | Out-String)) | WriteAndLog

# Parameter Consolidation
if ($p -and $path -and ($p -ne $path)) {
	"-p and --path were given different paths. Use only one." | WriteAndLog
	$help=$true
}
if ($p) {
	$path = $p
}
$war = $w -or $war
$help = $h -or $help

if(!(New-Object Security.Principal.WindowsPrincipal(
		[Security.Principal.WindowsIdentity]::GetCurrent())
	).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
	echo "This script requires root.  Please run as root" 
	exit 1
}

if ($help) {
  echo "  Setup script for the HIRS ACA"
  echo "  Syntax: powershell -ExecutionPolicy Bypass aca_bootRun.ps1 [-h|p|w|--path|--war]"
  echo "  options:"
  echo "     -p  | --path   Path to the HIRS_AttestationCAPortal.war file"
  echo "     -w  | --war    Use deployed war file"
  echo "     -h  | --help   Print this help"
  exit 1
}

if ($path) {
    $DEPLOYED_WAR = $path
}

if ($ALG -eq "RSA") {
   $CERT_CHAIN=(Join-Path $global:HIRS_DATA_CERTIFICATES_HIRS_RSA_PATH 'HIRS_rsa_3k_sha384_Cert_Chain.pem')
   $CLIENT_DB_P12=(Join-Path $global:HIRS_DATA_CERTIFICATES_HIRS_RSA_PATH 'HIRS_db_client_rsa_3k_sha384.p12')
   $ALIAS="hirs_aca_tls_rsa_3k_sha384"
} elseif ($ALG -eq "ECC") {
   $CERT_CHAIN=(Join-Path $global:HIRS_DATA_CERTIFICATES_HIRS_ECC_PATH 'HIRS_ecc_512_sha384_Cert_Chain.pem')
   $CLIENT_DB_P12=(Join-Path $global:HIRS_DATA_CERTIFICATES_HIRS_ECC_PATH 'HIRS_db_client_ecc_512_sha384.p12')
   $ALIAS="hirs_aca_tls_ecc_512_sha384"
}

if (![System.IO.Directory]::Exists($global:HIRS_DATA_CERTIFICATES_HIRS_DIR)) {
     echo "$global:HIRS_DATA_CERTIFICATES_HIRS_DIR directory does not exist. Please run aca_setup\.ps1 and try again."
     exit 1;
}

if (!$DEPLOYED_WAR) {
    if (![System.IO.File]::Exists($GRADLE_WRAPPER)) {
        echo 'This script is expected to be run from the HIRS top level project directory. Exiting.'
        exit 1;   	
    }
	$DEPLOYED_WAR='./HIRS_AttestationCAPortal/build/libs/HIRS_AttestationCAPortal.war'
}

# Run the embedded tomcat server with Web TLS enabled and database client TLS enabled by overrding critical parameters
# Note "&" is a sub parameter continuation, space represents a new parameter. Spaces and quotes matter.
# hibernate.connection.url is used for the DB connector which established DB TLS connectivity
# server.ssl arguments support the embeded tomcats use of TLS for the ACA Portal

$CONNECTOR_PARAMS="--hibernate.connection.url=jdbc:mariadb://localhost:3306/hirs_db?autoReconnect=true&user="+$global:ACA_PROPERTIES.'hirs_db_username'+"&password="+$global:ACA_PROPERTIES.'hirs_db_password'+"&sslMode=VERIFY_CA&serverSslCert=$CERT_CHAIN&keyStoreType=PKCS12&keyStorePassword="+$global:ACA_PROPERTIES.'hirs_pki_password'+"&keyStore=$CLIENT_DB_P12" | ChangeBackslashToForwardSlash

$WEB_TLS_PARAMS="--server.ssl.key-store-password="+$global:ACA_PROPERTIES.'hirs_pki_password'+ 
" --server.ssl.trust-store-password="+$global:ACA_PROPERTIES.'hirs_pki_password' | ChangeBackslashToForwardSlash
echo $CONNECTOR_PARAMS
echo $WEB_TLS_PARAMS
if ($w -or $war) {
	echo "Booting the ACA from a war file..." | WriteAndLog
	java -jar $DEPLOYED_WAR --spring.config.location=$SPRING_PROP_FILE
} else  {
    echo "Booting the ACA from local build..." | WriteAndLog
	./gradlew bootRun --args="--spring.config.location=$SPRING_PROP_FILE"
}
