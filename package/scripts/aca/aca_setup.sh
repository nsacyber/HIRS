#!/bin/bash
# Capture location of the script to allow from invocation from any location
SCRIPT_DIR=$( dirname -- "$( readlink -f -- "$0"; )"; )
PROP_FILE='../../../HIRS_AttestationCAPortal/src/main/resources/application.properties'
CONF_DIR=/etc/hirs/aca
LOG_FILE_NAME="hirs_aca_install_"$(date +%Y-%m-%d).log 
LOG_DIR="/opt/embeddedtomcat/logs/"
LOG_FILE="$LOG_DIR$LOG_FILE_NAME"
echo "LOG_FILE is $LOG_FILE"

if [ "$EUID" -ne 0 ]
      then echo "The first time this script is run, this script requires root.  Please run as root"
      exit 1
fi

echo "HIRS ACA Setup initiated on $(date +%Y-%m-%d)" > "$LOG_FILE"

mkdir -p $CONF_DIR  $LOG_DIR

pushd $SCRIPT_DIR

# If setup for development start with basic spring config
if [ -f  $PROP_FILE ]; then
   cp $PROP_FILE $CONF_DIR/.
fi

sh ../db/db_create.sh $LOG_FILE
if [ $? -eq 0 ]; then
    echo "ACA database setup complete" | tee -a "$LOG_FILE"
  else
    echo "Error setting up ACA DB" | tee -a "$LOG_FILE"
    exit 1
fi
sh ../pki/pki_setup.sh $LOG_FILE
if [ $? -eq 0 ]; then 
      echo "ACA PKI  setup complete" | tee -a "$LOG_FILE"
  else
    echo "Error setting up ACA PKI" | tee -a "$LOG_FILE"
    exit 1
fi

 echo "ACA setup complete" | tee -a "$LOG_FILE"

popd