#!/bin/bash
#####################################################################################
#
# Script to remove ACA setup files and database items.
# 
#
#####################################################################################

SCRIPT_DIR=$( dirname -- "$( readlink -f -- "$0"; )"; )
LOG_FILE=/dev/null

# Check for Admin privileges
if [ "$EUID" -ne 0 ]; then
      echo "This script requires root.  ACA setup not removed. Please run as root."
      exit 1
fi
if [ ! -f /etc/hirs/aca/aca.properties ]; then
      echo "aca.properties does not exist, aborting."
      exit 1
fi

# remove the hrs-db and hirs_db user
pushd $SCRIPT_DIR/../db/
sh db_drop.sh
popd

# remove pki files and config files
echo "Removing certificates and config files..."
rm -rf /etc/hirs

# Note add check for ACA running and stop the process if it is 

echo "ACA setup removal complete."