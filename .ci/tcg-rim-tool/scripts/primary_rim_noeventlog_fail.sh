#!/bin/bash
#Test for a missing event log
# Capture location of this script to allow from invocation from any location
scriptDir=$(dirname -- "$(readlink -f -- "${BASH_SOURCE[0]}")")
# go to the script directory so everything runs smoothly ...
pushd $scriptDir > /dev/null

. ./rim_functions.sh
#rim create
rim -c base -a ../configs/Base_Rim_Config.json -k ../keys/PC_OEM1_rim_signer_rsa_3k_sha384.key -p ../certs/PC_OEM1_rim_signer_rsa_3k_sha384.pem  -o noEventlog.swidtag
rim_create_fail_test $?

#Return to where ever you came from
popd > /dev/null