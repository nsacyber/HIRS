#!/bin/bash
#####################################################################################
#
# Script to remove the ACA service when running as a service if not running in a container
#
#####################################################################################

SCRIPT_DIR=$( dirname -- "$( readlink -f -- "$0"; )"; )
pushd $SCRIPT_DIR > /dev/null
source ../db/mysql_util.sh

check_systemd
 if [ $SYSD_SERVICE = true ]; then
    systemctl stop hirs-aca
    systemctl disable hirs-aca.service
    systemctl reset-failed hirs-aca
 fi

 popd > /dev/null