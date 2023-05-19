#!/bin/bash
############################################################################################
# Creates 2 Certificate Chains for the ACA:
# 1 RSA 3K SHA 384
# 2 ECC 512 SHA 384
#
############################################################################################

# Capture location of the script to allow from invocation from any location 
SCRIPT_DIR=$( dirname -- "$( readlink -f -- "$0"; )"; )
# Set HIRS PKI  password
if [ -z $HIRS_PKI_PWD ]; then
   # Create a 32 character random password
   PKI_PASS=$(head -c 64 /dev/urandom | md5sum | tr -dc 'a-zA-Z0-9')
   #PKI_PASS="xrb204k"
fi

# Create an ACA proerties file using the new password
pushd $SCRIPT_DIR &> /dev/null
sh ../aca/aca_property_setup.sh $PKI_PASS
popd &> /dev/null

# Create Cert Chains
rm -rf /etc/hirs/certificates
mkdir -p /etc/hirs/certificates/

pushd  /etc/hirs/certificates/

cp $SCRIPT_DIR/ca.conf .
sh $SCRIPT_DIR/pki_chain_gen.sh "HIRS" "rsa" "3072" "sha384" "$PKI_PASS"
sh $SCRIPT_DIR/pki_chain_gen.sh "HIRS" "ecc" "512" "sha384" "$PKI_PASS" 

popd
