param (
    [switch]$sd, [switch]${skip-db} = $false,
	[switch]$sp, [switch]${skip-pki} = $false,
	[switch]$u, [switch]$unattended = $false,
	[switch]$h, [switch]$help = $false
)

# Parameter Consolidation
$skipdb=$sd -or ${skip-db}
$skippki=$sp -or ${skip-pki}
$unattended=$u -or $unattended
$help = $h -or $help

if ($help) {
    Write-Host "  Setup script for the HIRS ACA on Windows"
    Write-Host "  Syntax: .\aca_setup.ps1 [-u|-h|-sd|-sp|-skip-db|-skip-pki|-unattended|-help]"
    Write-Host "  Flag options:"
    Write-Host "     [-u  | -unattended] Runs the script unattended."
    Write-Host "     [-h  | -help]   Prints this help message."
    Write-Host "     [-sp | -skip-pki] Skips the pki setup of the setup script."
    Write-Host "     [-sb | -skip-db] Skips the database setup of the setup script."
	exit 1
}

if(!(New-Object Security.Principal.WindowsPrincipal(
		[Security.Principal.WindowsIdentity]::GetCurrent())
	).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
	Write-Host "This script requires root.  Please run as root" 
	exit 1
}

$APP_HOME=(Split-Path -parent $PSCommandPath)
$ACA_COMMON_SCRIPT=(Join-Path $APP_HOME 'aca_common.ps1')
$COMP_JSON = (Resolve-Path ([System.IO.Path]::Combine(
    $APP_HOME, '..', '..', '..', 'HIRS_AttestationCA', 'src', 'main', 'resources', 'component-class.json'))).Path
$VENDOR_TABLE=(Resolve-Path ([System.IO.Path]::Combine(
    $APP_HOME, '..', '..', '..', 'HIRS_Utils', 'src', 'main', 'resources', 'vendor-table.json'))).Path
$SPRING_PROPERTIES_FILE=(Resolve-Path ([System.IO.Path]::Combine(
    $APP_HOME, '..', '..', '..', 'HIRS_AttestationCAPortal', 'src', 'main', 'resources', 'application.win.properties'))).Path

# Load other scripts
. $ACA_COMMON_SCRIPT

# Set up log
set_up_log

Write-Output "HIRS ACA Setup initiated on $(Get-Date -Format 'yyyy-MM-dd')" | WriteAndLog

Write-Output "-----------------------------------------------------------" | WriteAndLog
Write-Output "ACA setup log file is $global:LOG_FILE" | WriteAndLog
Write-Output ("Running with these arguments: "+($PSBoundParameters | Out-String)) | WriteAndLog

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
} else {
    Write-Output "File already exists: $global:HIRS_DATA_ACA_PROPERTIES_FILE" | WriteAndLog
}  

read_aca_properties $global:HIRS_DATA_ACA_PROPERTIES_FILE

# Read spring application.properties

# create it, if it doesn't exist
if (-not (Test-Path $global:HIRS_DATA_SPRING_PROP_FILE)) {
    New-Item -ItemType File -Path $global:HIRS_DATA_SPRING_PROP_FILE
} else {
    Write-Output "File already exists: $global:HIRS_DATA_SPRING_PROP_FILE" | WriteAndLog
}
  
read_spring_properties $global:HIRS_DATA_SPRING_PROP_FILE

# Runs the pki_setup script (along with the other scripts under the PKI folder)
if (!$skippki) {
	if (!$Env:HIRS_PKI_PWD) {
		$HIRS_PKI_PWD=(create_random)
		# NOTE: Writing to the environment variable did not work within the container
		# This password will be stored in the ACA properties file.
		Write-Output "Using randomly generated password for the PKI key password" | WriteAndLog
		Write-Host "NOT LOGGED: Using pki password=$HIRS_PKI_PWD"
	} else {
		$HIRS_PKI_PWD=$Env:HIRS_PKI_PWD
		Write-Output "Using system supplied password for the PKI key password" | WriteAndLog
	}
	pwsh -ExecutionPolicy Bypass $global:HIRS_REL_WIN_PKI_SETUP -LOG_FILE:"$global:LOG_FILE" -PKI_PASS:"$HIRS_PKI_PWD" -UNATTENDED:"$unattended"
    if ($LastExitCode -eq 0) { 
        Write-Output "ACA PKI setup complete" | WriteAndLog
    } else {
        Write-Output "Error setting up ACA PKI" | WriteAndLog
        exit 1
    }
} else {
    Write-Output ("ACA PKI setup cannot be run because there are command line argument(s): "+($PSBoundParameters.Keys | Where-Object { $_ -match 'skip-pki|sp' } )) | WriteAndLog
}

# Runs the create_db script (along with the other scripts under the DB folder)
if (!$skipdb) {
	pwsh -ExecutionPolicy Bypass $global:HIRS_REL_WIN_DB_CREATE -LOG_FILE:"$global:LOG_FILE" -UNATTENDED:"$unattended"
    if ($LastExitCode -eq 0) { 
        Write-Output "ACA database setup complete" | WriteAndLog
    } else {
        Write-Output "Error setting up ACA DB" | WriteAndLog
        exit 1
    }
} else {
    Write-Output ("ACA Database setup cannot be run because there are command line argument(s): "+($PSBoundParameters.Keys | Where-Object { $_ -match 'skip-db|sd'})) | WriteAndLog
}

Write-Output "ACA setup complete" | WriteAndLog