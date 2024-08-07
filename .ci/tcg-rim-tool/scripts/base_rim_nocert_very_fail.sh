#!/bin/bash
#Test verify with no cert chain
# Capture location of this script to allow from invocation from any location
scriptDir=$(dirname -- "$(readlink -f -- "${BASH_SOURCE[0]}")")
# go to the script directory so everything runs smoothly ...
pushd $scriptDir > /dev/null

. ./rim_functions.sh
#clearing and creating a new tmp folder
rm -rf tmp
mkdir -p tmp
#rim create
rim -c base -a ../configs/Base_Rim_Config.json -l ../eventlogs/TpmLog.bin -k ../keys/PC_OEM1_rim_signer_rsa_3k_sha384.key -p ../certs/PC_OEM1_rim_signer_rsa_3k_sha384.pem -o tmp/noCert.swidtag
rim_create_status $?

# rim verify without cert chain
rim -v tmp/noCert.swidtag -l ../eventlogs/TpmLog.bin -t ../certs/PC_OEM1_Cert_Chain.pem
rim_verify_fail_test $?

#Return to where ever you came from
popd > /dev/null