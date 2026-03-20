param (
    [Alias("p", "path")][string]$PathToWarFile,
    [Alias("w", "war")][switch]$UseWarFile = $false,
    [Alias("d", "debug")][switch]$EnableDebugMode = $false,
    [Alias("h", "help")][switch]$ShowHelp = $false
)

if ($ShowHelp) {
    Write-Output "  Bootrun script for the HIRS ACA"
    Write-Output "  Syntax: powershell -ExecutionPolicy Bypass aca_bootRun.ps1 [-h|-p|-d|-w|-path|-war|-debug|-help]"
    Write-Output "  Flag options:"
    Write-Output "     [ -p  | -path]   Path to the HIRS_AttestationCAPortal.war file"
    Write-Output "     [ -w  | -war ]   Use deployed war file"
    Write-Output "     [ -d  | -debug ]  Launch the JVM with a debug port open"
    Write-Output "     [ -h  | -help ]   Print this help menu"
    exit 1
}

if (!(New-Object Security.Principal.WindowsPrincipal(
[Security.Principal.WindowsIdentity]::GetCurrent())
).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
    Write-Output "This script requires root.  Please run as root"
    exit 1
}

$ACA_SCRIPTS_HOME = (Split-Path -parent $PSCommandPath)
$ACA_COMMON_SCRIPT = (Join-Path $ACA_SCRIPTS_HOME 'aca_common.ps1')
$GRADLE_WRAPPER = './gradlew'
$DEPLOYED_WAR = $null
$DEBUG_OPTIONS = '-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:9123'

# Load other scripts
. $ACA_COMMON_SCRIPT

# Set up log
set_up_log

# Read aca.properties
read_aca_properties -file "$global:HIRS_DATA_ACA_PROPERTIES_FILE"

# Read spring application.properties
read_spring_properties -file "$global:HIRS_DATA_SPRING_PROP_FILE"

Write-Output "-----------------------------------------------------------" | WriteAndLog
Write-Output ("Running with these arguments: " + ($PSBoundParameters | Out-String)) | WriteAndLog

if ($PathToWarFile) {
    $DEPLOYED_WAR = $PathToWarFile
}

if (![System.IO.Directory]::Exists($global:HIRS_DATA_CERTIFICATES_HIRS_DIR)) {
    Write-Output "$global:HIRS_DATA_CERTIFICATES_HIRS_DIR directory does not exist. Please run .\aca_setup.ps1 and try again."
    exit 1;
}

if (!$DEPLOYED_WAR) {
    if (-not (Test-Path -Path $GRADLE_WRAPPER -PathType Leaf)) {
        Write-Output 'This script is expected to be run from the HIRS top level project directory. Exiting.'
        exit 1;
    }
    $DEPLOYED_WAR = './HIRS_AttestationCAPortal/build/libs/HIRS_AttestationCAPortal.war'
}

$SPRING_PROP_FILE_FORWARDSLASHES = ($global:HIRS_DATA_SPRING_PROP_FILE | ChangeBackslashToForwardSlash)

if ($UseWarFile) {
    Write-Output "Booting the ACA from a war file..." | WriteAndLog
    if ($EnableDebugMode) {
        Write-Output "... in debug"
        java $DEBUG_OPTIONS -jar $DEPLOYED_WAR --spring.config.location=$SPRING_PROP_FILE_FORWARDSLASHES
    } else {
        java -jar $DEPLOYED_WAR --spring.config.location=$SPRING_PROP_FILE_FORWARDSLASHES
    }
} else {
    Write-Output "Booting the ACA from local build..." | WriteAndLog
    if ($EnableDebugMode) {
        Write-Output "... in debug"
        ./gradlew bootRun --args="--spring.config.location=$SPRING_PROP_FILE_FORWARDSLASHES" -Pdebug="$DEBUG_OPTIONS"
    } else {
        ./gradlew bootRun --args="--spring.config.location=$SPRING_PROP_FILE_FORWARDSLASHES"
    }
}
