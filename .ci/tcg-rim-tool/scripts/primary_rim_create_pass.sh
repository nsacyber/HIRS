#!/bin/bash
#This test creates and verifies a single Primary RIM
# Capture location of this script to allow from invocation from any location
scriptDir=$(dirname -- "$(readlink -f -- "${BASH_SOURCE[0]}")")
# go to the script directory so everything runs smoothly ...
pushd $scriptDir > /dev/null

. ./rim_functions.sh
#clearing and creating a new tmp folder
rm -rf tmp
mkdir -p tmp

#declares failCount as number of failure tests that are not working as they should
#Exit 1: Rim Create failure
#Exit 2: Rim verify failure
failCount=0
# creating a base rim and checking exit status
rim -c base -a ../configs/Base_Rim_Config.json -l ../eventlogs/TpmLog.bin -k ../keys/PC_OEM1_rim_signer_rsa_3k_sha384.key -p ../keys/PC_OEM1_rim_signer_rsa_3k_sha384.pem -o tmp/baseRimFile.swidtag
rim_create_status $?

# RIM verify and checking exit status
rim -v tmp/baseRimFile.swidtag -p ../certs/PC_OEM1_rim_signer_rsa_3k_sha384.pem -t ../certs/PC_OEM1_Cert_Chain.pem -l ../eventlogs/TpmLog.bin
rim_verify_status $?

#Return to where ever you came from
popd > /dev/null

#script exit status
if [ $failCount -eq 0 ]; then
    echo "Expected Result (PASS) Result: PASS, baseRimFile.swidtag has a new base rim file signed by PC_OEM1_rim_signer_rsa_3k_sha384.key"
else
    echo "Expected Result (PASS) Result: FAILED, exit status $failCount"
fi
exit $failCount



