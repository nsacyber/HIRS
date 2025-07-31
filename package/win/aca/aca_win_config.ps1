# This script is intended to only be run from within a cloned copy of the HIRS repository from GitHub. It is not meant to be deployed.
# This script swaps configuration files for Windows into place.

$APP_HOME=(Split-Path -parent $PSCommandPath)
$ACA_COMMON_SCRIPT=(Join-Path $APP_HOME 'aca_common.ps1')

# Load other scripts
. $ACA_COMMON_SCRIPT

# Set up paths to configuration files
$BASE = Join-Path -Path $global:HIRS_REL_PACKAGE_HOME -ChildPath ".."
$PORTAL_PATH = Join-Path -Path $BASE -ChildPath "HIRS_AttestationCAPortal\src\main\resources"

$global:HIRS_REL_PORTAL_LOG4J_SPRING_XML = Join-Path $PORTAL_PATH "log4j2-spring.xml"
$global:HIRS_REL_PORTAL_APPLICATION_PROPERTIES = Join-Path $PORTAL_PATH "application.properties"
$global:HIRS_REL_PORTAL_LOG4J_SPRING_LINUX_XML = Join-Path $PORTAL_PATH "log4j2-spring.linux.xml"
$global:HIRS_REL_PORTAL_APPLICATION_LINUX_PROPERTIES = Join-Path $PORTAL_PATH "application.linux.properties"
$global:HIRS_REL_PORTAL_LOG4J_SPRING_WIN_XML = Join-Path $PORTAL_PATH "log4j2-spring.win.xml"
$global:HIRS_REL_PORTAL_APPLICATION_WIN_PROPERTIES = Join-Path $PORTAL_PATH "application.win.properties"
$global:HIRS_REL_WIN_PKI_CA_CONF = Join-Path $global:HIRS_REL_WIN_PKI_HOME "ca.conf"

# Back up linux configuration files
if (-not (Test-Path $global:HIRS_REL_PORTAL_LOG4J_SPRING_LINUX_XML)) {
    Move-Item $global:HIRS_REL_PORTAL_LOG4J_SPRING_XML $global:HIRS_REL_PORTAL_LOG4J_SPRING_LINUX_XML
} else {
    Write-Output "Destination already exists: $global:HIRS_REL_PORTAL_LOG4J_SPRING_LINUX_XML"
}

if (-not (Test-Path $global:HIRS_REL_PORTAL_APPLICATION_LINUX_PROPERTIES)) {
    Move-Item $global:HIRS_REL_PORTAL_APPLICATION_PROPERTIES $global:HIRS_REL_PORTAL_APPLICATION_LINUX_PROPERTIES
} else {
    Write-Output "Destination already exists: $global:HIRS_REL_PORTAL_APPLICATION_LINUX_PROPERTIES"
}

# Copy windows configuration files in place
Copy-Item "$global:HIRS_REL_PORTAL_LOG4J_SPRING_WIN_XML" "$global:HIRS_REL_PORTAL_LOG4J_SPRING_XML"
Copy-Item "$global:HIRS_REL_PORTAL_APPLICATION_WIN_PROPERTIES" "$global:HIRS_REL_PORTAL_APPLICATION_PROPERTIES"

# Make a copy of the ca.conf file local to the windows scripts
Copy-Item "$global:HIRS_REL_SCRIPTS_PKI_CA_CONF" "$global:HIRS_REL_WIN_PKI_CA_CONF"
