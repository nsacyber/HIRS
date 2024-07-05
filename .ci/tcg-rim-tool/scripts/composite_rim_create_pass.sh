#!/bin/bash
# Composite rim create pass test
# Capture location of this script to allow from invocation from any location
scriptDir=$(dirname -- "$(readlink -f -- "${BASH_SOURCE[0]}")")
# go to the script directory so everything runs smoothly ...
pushd $scriptDir > /dev/null

. ./rim_functions.sh
#clearing and creating a new tmp folder
rm -rf tmp
mkdir -p tmp

failCount=0
# primary rim create
rim -c base -a ../configs/Primary_Rim_Config.json -l ../eventlogs/TpmLog.bin -k ../keys/PC_OEM1_rim_signer_rsa_3k_sha384.key -p ../keys/PC_OEM1_rim_signer_rsa_3k_sha384.pem -o tmp/primaryRimFile.swidtag
rim_create_status $?

# verify primary rim
rim -v tmp/primaryRimFile.swidtag -l ../eventlogs/TpmLog.bin -t ../certs/PC_OEM1_Cert_Chain.pem -p ../keys/PC_OEM1_rim_signer_rsa_3k_sha384.pem
rim_verify_status $?

# comp rim create
rim -c base -a ../configs/Component1_Rim_Config.json -l ../eventlogs/TpmLog2.bin -k ../keys/COMP_OEM1_rim_signer_rsa_3k_sha384.key -p ../keys/COMP_OEM1_rim_signer_rsa_3k_sha384.pem -o tmp/compRimFile.swidtag
rim_create_status $?

# verify comp rim
rim -v tmp/compRimFile.swidtag -l ../eventlogs/TpmLog2.bin -t ../certs/COMP_OEM1_Cert_Chain.pem -p ../keys/COMP_OEM1_rim_signer_rsa_3k_sha384.pem
rim_verify_status $?

#Return to where ever you came from
popd > /dev/null

if [ $failCount -eq 0 ]; then
    echo "Expected Result (PASS) Result: PASS, primaryRimFile.swidtag has a new base rim file signed by PC_OEM1_rim_signer_rsa_3k_sha384.key"
    echo "Expected Result (PASS) Result: PASS, compRimFile.swidtag has a new base rim file signed by COMP_OEM1_rim_signer_rsa_3k_sha384.key"
else
    echo "Expected Result (PASS) Result: FAILED, exit status $failCount"
fi
exit $failCount

