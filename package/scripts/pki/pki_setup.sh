#!/bin/bash
############################################################################################
# Creates 2 Certificate Chains for the ACA:
# 1 RSA 3K SHA 384
# 2 ECC 512 SHA 384
#
############################################################################################

# Capture location of the script to allow from invocation from any location 
scriptDir=$( dirname -- "$( readlink -f -- "$0"; )"; )

# Create Cert Chains
rm -rf /etc/hirs/certificates
mkdir -p /etc/hirs/certificates/

pushd  /etc/hirs/certificates/

cp $scriptDir/ca.conf .
sh $scriptDir/pki_chain_gen.sh "HIRS" "rsa" "3072" "sha384"
sh $scriptDir/pki_chain_gen.sh "HIRS" "ecc" "512" "sha384"

popd
