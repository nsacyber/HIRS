# This script is intended to only be run from within a cloned copy of the HIRS repository from GitHub. It is not meant to be deployed.
# This script swaps configuration files for Windows into place.

$ACA_SCRIPTS_HOME=(Split-Path -parent $PSCommandPath)
$ACA_COMMON_SCRIPT=(Join-Path $ACA_SCRIPTS_HOME 'aca_common.ps1')

# load common script
. $ACA_COMMON_SCRIPT

Write-Host "----------------------------------------------------------------------"
Write-Host ""
Write-Host "Running the win config script"
Write-Host "Changing configuration files so that they may be used in a Windows system."

# Back up linux configuration files
if (-not (Test-Path $global:HIRS_REL_PORTAL_LOG4J_SPRING_LINUX_XML)) {
    Move-Item $global:HIRS_REL_PORTAL_LOG4J_SPRING_XML $global:HIRS_REL_PORTAL_LOG4J_SPRING_LINUX_XML
} else {
    Write-Output "Destination already exists: $global:HIRS_REL_PORTAL_LOG4J_SPRING_LINUX_XML"
}

if (-not (Test-Path $global:HIRS_REL_PORTAL_APPLICATION_LINUX_SPRING_PROPERTIES)) {
    Move-Item $global:HIRS_REL_PORTAL_APPLICATION_SPRING_PROPERTIES $global:HIRS_REL_PORTAL_APPLICATION_LINUX_SPRING_PROPERTIES
} else {
    Write-Output "Destination already exists: $global:HIRS_REL_PORTAL_APPLICATION_LINUX_SPRING_PROPERTIES"
}

# Copy windows configuration files in place
Copy-Item $global:HIRS_REL_PORTAL_LOG4J_SPRING_WIN_XML $global:HIRS_REL_PORTAL_LOG4J_SPRING_XML
Copy-Item $global:HIRS_REL_PORTAL_APPLICATION_WIN_SPRING_PROPERTIES $global:HIRS_REL_PORTAL_APPLICATION_SPRING_PROPERTIES

# Make a copy of the ca.conf file local to the windows scripts
Copy-Item $global:HIRS_REL_SCRIPTS_PKI_CA_CONF $global:HIRS_REL_WIN_PKI_CA_CONF

Write-Host "Finished setting up the Windows configuration files"
Write-Host "----------------------------------------------------------------------"
Write-Host ""
