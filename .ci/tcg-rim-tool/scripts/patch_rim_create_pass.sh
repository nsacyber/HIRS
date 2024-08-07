#!/bin/bash
#Creates and Verifies a Patch RIM. Needs to refer to the Primary RIM created in previous test.
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
# rim create
rim -c base -a ../configs/Patch_RIM_Config.json -l ../eventlogs/TpmLog2.bin -k ../keys/PC_OEM1_rim_signer_rsa_3k_sha384.key -p ../certs/PC_OEM1_rim_signer_rsa_3k_sha384.pem -o tmp/patchRimFile.swidtag
rim_create_status $?

# RIM verify
rim -v tmp/patchRimFile.swidtag -t ../certs/PC_OEM1_Cert_Chain.pem -l ../eventlogs/TpmLog2.bin -p ../certs/PC_OEM1_rim_signer_rsa_3k_sha384.pem
rim_verify_status $?

#Return to where ever you came from
popd > /dev/null

#Exit status with message
if [ $failCount -eq 0 ]; then
    echo "Expected Result (PASS) Result: PASS, patchRimFile.swidtag has a new base rim file signed by PC_OEM1_rim_signer_rsa_3k_sha384.key"
else
    echo "Expected Result (PASS) Result: FAILED, exit status $failCount"
fi
exit $failCount