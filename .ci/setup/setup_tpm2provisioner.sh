#!/bin/bash
#########################################################################################
#  Script to setup the TPM 2.0 Provisioner Docker Image for System Tests
#
#########################################################################################
set -e
pushd /  > /dev/null
echo "Setting up TPM emulator for the TPM2 Provisioner"
 
find / -name nvchip
source /HIRS/.ci/setup/tpm2_common.sh

#Wait for the ACA to spin up, if it hasnt already
waitForAca

# Install packages
installProvisioner

# set location of tcg artifacts
setTcgProperties
#echo "Contents of /etc/hirs is $(ls -al /etc/hirs)";

# Install TPM 2.0 Emulator
initTpm2Emulator

# Update the hirs-site.config file
updateHirsSiteConfigFile

echo "TPM 2.0 Emulator NV RAM list"
tpm2_nvlist

echo ""
echo "===========HIRS ACA TPM 2.0 Provisioner Setup Complete!==========="

popd > /dev/null