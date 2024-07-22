#!/bin/bash
#Test the option to embed a certificate into the swidtag
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
#rim create, -e flag fails? Bouncy Castle error...
rim -c base -a ../configs/Base_Rim_Config.json -l ../eventlogs/TpmLog.bin -k ../keys/PC_OEM1_rim_signer_rsa_3k_sha384.key -p ../certs/PC_OEM1_rim_signer_rsa_3k_sha384.pem -e -o tmp/primary_embedd.swidtag
rim_create_status $?

#rim verify.
rim -v  tmp/primary_embedd.swidtag -p ../certs/PC_OEM1_rim_signer_rsa_3k_sha384.pem -l ../eventlogs/TpmLog.bin -t ../certs/PC_OEM1_RSA_Cert_Chain.pem
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