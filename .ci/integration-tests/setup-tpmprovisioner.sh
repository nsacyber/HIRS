#!/bin/bash

# Script to setup the TPM2 Provisioner Docker Image for Integration Tests

set -e

echo "START TPMPROVISIONER SCRIPT!!!!!!!!!!!!!!!!!!!!!"
ln -s /lib/x86_64-linux-gnu/libcrypto.so.1.0.0 /lib64/libcrypto.so.1.0.0

# Attempt to build TPM Emulator
pushd /tpm_emulator
cd tpm-emulator-0.7.5 && ./build.sh

echo "Build complete!"
cd build && make install
popd

echo "Finished installing"

# Initialize TPM Emulator
echo "Attempting modprobe"
modprobe tpmd_dev &> /tpm_emulator/test-file
echo $?
/usr/local/bin/tpmd &>> /tpm_emulator/test-file
echo $?
tcsd &>> /tpm_emulator/test-file
echo $?

# Package and install HIRS TPM 1.2 Provisioner
pushd /HIRS
if [ ! -d package/rpm/RPMS ]; then
    ./package/package.centos.sh
fi
yum install -y package/rpm/RPMS/noarch/HIRS_Provisioner_TPM_1_2*.el7.noarch.rpm
popd
