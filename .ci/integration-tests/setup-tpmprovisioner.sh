#!/bin/bash

# Script to setup the TPM2 Provisioner Docker Image for Integration Tests

set -e

# Set variables for server
export TPM_PATH=/tpm_emulator/tpm_storage
export TPM_PORT=6543

# Set variables for client utils
export TPM_SERVER_NAME=localhost
export TPM_SERVER_PORT=6543

# Set variable for TrouSerS
export TCSD_TCP_DEVICE_PORT=6543

mkdir -p $TPM_PATH

pushd /tpm_emulator

./tpm/tpm_server&
./libtpm/utils/tpmbios

popd

echo "Starting TrouSerS Daemon"
tcsd -e

echo "Testing TPM Connectivity"
tpm_selftest

# Package and install HIRS TPM 1.2 Provisioner
pushd /HIRS
if [ ! -d package/rpm/RPMS ]; then
    ./package/package.centos.sh
fi
yum install -y package/rpm/RPMS/noarch/HIRS_Provisioner_TPM_1_2*.el7.noarch.rpm
popd
