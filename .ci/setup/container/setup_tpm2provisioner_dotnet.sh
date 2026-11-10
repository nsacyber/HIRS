#!/bin/bash
#########################################################################################
#  Script to setup the TPM Provisioner.NET for System Tests
#########################################################################################

# Setting configurations
. /hirs/.ci/docker/.env
source /hirs/.ci/setup/container/tpm2_common.sh

set -a

set -e
echo "*** Setting up TPM emulator for the TPM2 Provisioner *** "

# Wait for ACA to boot
waitForAca

## Un-package Provisioner.NET RPM
yes | dnf install $HIRS_CI_REPO_ROOT/HIRS_Provisioner.NET/build/distributions/*.rpm -y > /dev/null

# Initiate startup for IBMTSS Tools
startFreshTpmServer -f
startupTpm
installEkCert

setCiHirsAppsettingsFile

# Triggering a single provision for test
echo "==========="
echo "*** INITIAL TEST: Single Provision with Default Policy:"
echo "==========="
/usr/share/hirs/tpm_aca_provision --tcp --ip 127.0.0.1:2321 --sim
