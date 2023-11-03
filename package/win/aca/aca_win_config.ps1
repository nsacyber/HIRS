# This script is intended to only be run from within a cloned copy of the HIRS repository from GitHub. It is not meant to be deployed.
# This script swaps configuration files for Windows into place.

$APP_HOME=(Split-Path -parent $PSCommandPath)
$ACA_COMMON_SCRIPT=(Join-Path $APP_HOME 'aca_common.ps1')

# Load other scripts
. $ACA_COMMON_SCRIPT

# Set up paths to configuration files
$global:HIRS_REL_PORTAL_LOG4J_SPRING_XML=(Join-Path -Resolve $global:HIRS_REL_PACKAGE_HOME .. HIRS_AttestationCAPortal src main resources log4j2-spring.xml)
$global:HIRS_REL_PORTAL_APPLICATION_PROPERTIES=(Join-Path -Resolve $global:HIRS_REL_PACKAGE_HOME .. HIRS_AttestationCAPortal src main resources application.properties)
$global:HIRS_REL_PORTAL_LOG4J_SPRING_LINUX_XML=(Join-Path $global:HIRS_REL_PACKAGE_HOME .. HIRS_AttestationCAPortal src main resources log4j2-spring.linux.xml)
$global:HIRS_REL_PORTAL_APPLICATION_LINUX_PROPERTIES=(Join-Path $global:HIRS_REL_PACKAGE_HOME .. HIRS_AttestationCAPortal src main resources application.linux.properties)
$global:HIRS_REL_PORTAL_LOG4J_SPRING_WIN_XML=(Join-Path -Resolve $global:HIRS_REL_PACKAGE_HOME .. HIRS_AttestationCAPortal src main resources log4j2-spring.win.xml)
$global:HIRS_REL_PORTAL_APPLICATION_WIN_PROPERTIES=(Join-Path -Resolve $global:HIRS_REL_PACKAGE_HOME .. HIRS_AttestationCAPortal src main resources application.win.properties)

# Back up linux configuration files
mv $global:HIRS_REL_PORTAL_LOG4J_SPRING_XML $global:HIRS_REL_PORTAL_LOG4J_SPRING_LINUX_XML
mv $global:HIRS_REL_PORTAL_APPLICATION_PROPERTIES $global:HIRS_REL_PORTAL_APPLICATION_LINUX_PROPERTIES

# Copy windows configuration files in place
cp $global:HIRS_REL_PORTAL_LOG4J_SPRING_WIN_XML $global:HIRS_REL_PORTAL_LOG4J_SPRING_XML
cp $global:HIRS_REL_PORTAL_APPLICATION_WIN_PROPERTIES $global:HIRS_REL_PORTAL_APPLICATION_PROPERTIES
