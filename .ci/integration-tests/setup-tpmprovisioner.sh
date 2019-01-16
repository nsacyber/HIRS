#!/bin/bash

# Script to setup the TPM2 Provisioner Docker Image for Integration Tests

set -e

echo "START TPMPROVISIONER SCRIPT!!!!!!!!!!!!!!!!!!!!!"

ls -la /dev/

# ln -s /lib/x86_64-linux-gnu/libcrypto.so.1.0.0 /lib64/libcrypto.so.1.0.0
echo "Starting TrouSerS Daemon"
tcsd

echo "Testing TPM Connectivity"
tpm_selftest

# Package and install HIRS TPM 1.2 Provisioner
pushd /HIRS
if [ ! -d package/rpm/RPMS ]; then
    ./package/package.centos.sh
fi
yum install -y package/rpm/RPMS/noarch/HIRS_Provisioner_TPM_1_2*.el7.noarch.rpm
popd
