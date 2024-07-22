#!/bin/bash
#test rim create with no config file.
# Capture location of this script to allow from invocation from any location
scriptDir=$(dirname -- "$(readlink -f -- "${BASH_SOURCE[0]}")")
# go to the script directory so everything runs smoothly ...
pushd $scriptDir > /dev/null

. ./rim_functions.sh
#rim create
rim -c base -l ../eventlog/TpmLog.bin -k ../keys/PC_OEM1_rim_signer_rsa_3k_sha384.key -p ../certs/PC_OEM1_rim_signer_rsa_3k_sha384.pem  -o noConfig.swidtag
rim_create_fail_test $?

#Return to where ever you came from
popd > /dev/null