param (
    [switch]$sd, [switch]${skip-db} = $false,
	[switch]$sp, [switch]${skip-pki} = $false,
	[switch]$u, [switch]$unattended = $false,
	[switch]$h, [switch]$help = $false
)

$APP_HOME=(Split-Path -parent $PSCommandPath)
$ACA_COMMON_SCRIPT=(Join-Path $APP_HOME 'aca_common.ps1')
$COMP_JSON=(Join-Path $APP_HOME '..' '..' '..' 'HIRS_AttestationCA' 'src' 'main' 'resources' 'component-class.json')
$VENDOR_TABLE=(Join-Path $APP_HOME '..' '..' '..' 'HIRS_Utils' 'src' 'main' 'resources' 'vendor-table.json')

# Load other scripts
. $ACA_COMMON_SCRIPT

# Set up log
set_up_log
echo "-----------------------------------------------------------" | WriteAndLog
echo "ACA setup log file is $global:LOG_FILE" | WriteAndLog
echo ("Running with these arguments: "+($PSBoundParameters | Out-String)) | WriteAndLog

# Read aca.properties
mkdir -F -p $global:HIRS_CONF_DIR 2>&1 > $null
mkdir -F -p $global:HIRS_CONF_DEFAULT_PROPERTIES_DIR 2>&1 > $null
mkdir -F -p $global:HIRS_DATA_LOG_DIR 2>&1 > $null
cp $COMP_JSON $global:HIRS_CONF_DEFAULT_PROPERTIES_DIR
cp $VENDOR_TABLE $global:HIRS_CONF_DEFAULT_PROPERTIES_DIR
touch $global:HIRS_DATA_ACA_PROPERTIES_FILE  # create it, if it doesn't exist
read_aca_properties $global:HIRS_DATA_ACA_PROPERTIES_FILE

# Read spring application.properties
touch $global:HIRS_DATA_SPRING_PROP_FILE  # create it, if it doesn't exist
read_spring_properties $global:HIRS_DATA_SPRING_PROP_FILE

# Parameter Consolidation
$skipdb=$sd -or ${skip-db}
$skippki=$sp -or ${skip-pki}
$unattended=$u -or $unattended
$help = $h -or $help

if ($help) {
    echo "  Setup script for the HIRS ACA"
    echo "  Syntax: sh aca_setup.sh [-u|h|sd|sp|--skip-db|--skip-pki]"
    echo "  options:"
    echo "     -u  | --unattended   Run unattended"
    echo "     -h  | --help   Print this Help."
    echo "     -sp | --skip-pki run the setup without pki setup."
    echo "     -sb | --skip-db run the setup without database setup."
	exit 1
}

if(!(New-Object Security.Principal.WindowsPrincipal(
		[Security.Principal.WindowsIdentity]::GetCurrent())
	).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
	echo "This script requires root.  Please run as root" 
	exit 1
}



echo "HIRS ACA Setup initiated on $(date +%Y-%m-%d)" | WriteAndLog

if (!$skippki) {
	if (!$Env:HIRS_PKI_PWD) {
		$HIRS_PKI_PWD=(create_random)
		# NOTE: Writing to the environment variable did not work within the container
		# This password will be stored in the ACA properties file.
		echo "Using randomly generated password for the PKI key password" | WriteAndLog
		Write-Host "NOT LOGGED: Using pki password=$HIRS_PKI_PWD"
	} else {
		$HIRS_PKI_PWD=$Env:HIRS_PKI_PWD
		echo "Using system supplied password for the PKI key password" | WriteAndLog
	}
	pwsh -ExecutionPolicy Bypass $global:HIRS_REL_WIN_PKI_SETUP -LOG_FILE:"$global:LOG_FILE" -PKI_PASS:"$HIRS_PKI_PWD" -UNATTENDED:"$unattended"
    if ($LastExitCode -eq 0) { 
        echo "ACA PKI  setup complete" | WriteAndLog
    } else {
        echo "Error setting up ACA PKI" | WriteAndLog
        exit 1
    }
} else {
    echo ("ACA PKI setup not run due to presence of command line argument: "+($PSBoundParameters.Keys | grep -E "skip-pki|sp")) | WriteAndLog
}

if (!$skipdb) {
	pwsh -ExecutionPolicy Bypass $global:HIRS_REL_WIN_DB_CREATE -LOG_FILE:"$global:LOG_FILE" -UNATTENDED:"$unattended"
    if ($LastExitCode -eq 0) { 
        echo "ACA database setup complete" | WriteAndLog
    } else {
        echo "Error setting up ACA DB" | WriteAndLog
        exit 1
    }
} else {
    echo ("ACA Database setup not run due to command line argument: "+($PSBoundParameters.Keys | grep -E "skip-db|sd")) | WriteAndLog
}

echo "ACA setup complete" | WriteAndLog