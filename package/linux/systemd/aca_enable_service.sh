#!/bin/bash
#####################################################################################
#
# Script to add the ACA service when running as a service if not running in a container
#
#####################################################################################

SCRIPT_DIR=$( dirname -- "$( readlink -f -- "$0"; )"; )
pushd $SCRIPT_DIR > /dev/nill
source ../db/mysql_util.sh

check_systemd
 if [ $SYSD_SERVICE = true ]; then
    echo "Starting the ACA as a service..."
    systemctl enable /opt/hirs/aca/scripts/systemd/hirs-aca.service
    systemctl start hirs-aca
 else 
    echo "Starting the ACA via Springboot..."
    bash /opt/hirs/aca/scripts/aca/aca_bootRun.sh -w &
 fi

 popd > /dev/null