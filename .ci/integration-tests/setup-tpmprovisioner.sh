#!/bin/bash

# Script to setup the TPM2 Provisioner Docker Image for Integration Tests

set -e

ls -la /dev

echo "Starting TrouSerS Daemon"
tcsd -f

echo "Testing TPM Connectivity"
tpm_selftest

# Package and install HIRS TPM 1.2 Provisioner
pushd /HIRS
if [ ! -d package/rpm/RPMS ]; then
    ./package/package.centos.sh
fi
yum install -y package/rpm/RPMS/noarch/HIRS_Provisioner_TPM_1_2*.el7.noarch.rpm
popd
