param (
    [string]$p, [string]$path = $null,
	[switch]$w, [switch]$war = $false,
	[switch]$d, [switch]$debug = $false,
	[switch]$h, [switch]$help = $false
)

$APP_HOME=(Split-Path -parent $PSCommandPath)
$ACA_COMMON_SCRIPT=(Join-Path $APP_HOME 'aca_common.ps1')
$ALG="RSA" # or "ECC"
$GRADLE_WRAPPER='./gradlew'
$DEPLOYED_WAR=$null
$DEBUG_OPTIONS='-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:9123'

# Load other scripts
. $ACA_COMMON_SCRIPT

# Set up log
set_up_log

# Read aca.properties
read_aca_properties $global:HIRS_DATA_ACA_PROPERTIES_FILE

# Read spring application.properties
read_spring_properties $global:HIRS_DATA_SPRING_PROP_FILE

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
$debug = $d -or $debug
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
  echo "     -d  | --debug  Launch the JVM with a debug port open"
  echo "     -h  | --help   Print this help"
  exit 1
}

if ($path) {
    $DEPLOYED_WAR = $path
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

$SPRING_PROP_FILE_FORWARDSLASHES=($global:HIRS_DATA_SPRING_PROP_FILE | ChangeBackslashToForwardSlash)
if ($w -or $war) {
	echo "Booting the ACA from a war file..." | WriteAndLog
    if ($d -or $debug) {
        echo "... in debug"
        java $DEBUG_OPTIONS -jar $DEPLOYED_WAR --spring.config.location=$SPRING_PROP_FILE_FORWARDSLASHES
    } else {
	    java -jar $DEPLOYED_WAR --spring.config.location=$SPRING_PROP_FILE_FORWARDSLASHES
    }
} else  {
    echo "Booting the ACA from local build..." | WriteAndLog
    if ($d -or $debug) {
        echo "... in debug"
        ./gradlew bootRun --args="--spring.config.location=$SPRING_PROP_FILE_FORWARDSLASHES" -Pdebug="$$DEBUG_OPTIONS"
    } else {
	    ./gradlew bootRun --args="--spring.config.location=$SPRING_PROP_FILE_FORWARDSLASHES"
    }
	
}
