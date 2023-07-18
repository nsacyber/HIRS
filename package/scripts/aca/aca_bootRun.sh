#!/bin/bash
#####################################################################################
#
# Script to run ACA using the gradle spring pluing bootRun command with password set
#
#
####################################################################################

CONFIG_FILE="/etc/hirs/aca/application.properties"

echo "Starting HIRS ACA on https://localhost:8443/HIRS_AttestationCAPortal/portal/index"

./gradlew bootRun --args="--spring.config.location=$CONFIG_FILE"