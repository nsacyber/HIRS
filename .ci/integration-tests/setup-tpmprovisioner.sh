#!/bin/bash

# Script to setup the TPM2 Provisioner Docker Image for Integration Tests

set -e

# Attempt to build TPM Emulator
pushd /tpm_emulator
cd tpm-emulator-0.7.5 && ./build.sh && cd build && make install
popd

# Initialize TPM Emulator
modprobe tpmd_dev &> /tpm_emulator/test-file
tpmd &>> /tpm_emulator/test-file
tcsd &>> /tpm_emulator/test-file

# Package and install HIRS TPM 1.2 Provisioner
pushd /HIRS
if [ ! -d package/rpm/RPMS ]; then
    ./package/package.centos.sh
fi
yum install -y package/rpm/RPMS/x86_64/HIRS_Provisioner_TPM_1_2*.el7.noarch.rpm
popd
