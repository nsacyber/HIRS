#!/bin/bash
# Capture location of the script to allow from invocation from any location
SCRIPT_DIR=$( dirname -- "$( readlink -f -- "$0"; )"; )
PROP_FILE='../../../HIRS_AttestationCAPortal/src/main/resources/application.properties'

if [ "$EUID" -ne 0 ]
      then echo "The first time this script is run, this script requires root.  Please run as root"
      exit 1
fi

mkdir -p /etc/hirs/aca/

pushd $SCRIPT_DIR

# If setup for development start with basic spring config
if [ -f  $PROP_FILE ]; then
   cp $PROP_FILE /etc/hirs/aca/.
fi

sh ../db/db_create.sh
if [ $? -eq 0 ]; then
    echo "ACA database setup complete"
  else
    echo "Error setting up ACA DB"
    exit 1
fi
sh ../pki/pki_setup.sh
if [ $? -eq 0 ]; then 
      echo "ACA PKI  setup complete"
  else
    echo "Error setting up ACA PKI"
    exit 1
fi

 echo "ACA setup complete"

popd