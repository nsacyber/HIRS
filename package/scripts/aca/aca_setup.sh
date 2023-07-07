#!/bin/bash
# Capture location of the script to allow from invocation from any location
SCRIPT_DIR=$( dirname -- "$( readlink -f -- "$0"; )"; )

mkdir -p /etc/hirs/aca

pushd $SCRIPT_DIR

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
