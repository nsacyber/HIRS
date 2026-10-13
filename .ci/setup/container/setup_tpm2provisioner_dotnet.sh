#!/bin/bash
#########################################################################################
#  Script to setup the TPM Provisioner.NET for System Tests
#########################################################################################

set -e

# Setting configurations
. /hirs/.ci/docker/.env
source /hirs/.ci/setup/container/tpm2_common.sh

set -a

echo "*** Setting up TPM emulator for the TPM2 Provisioner *** "

# Wait for ACA to boot
waitForAca

## Un-package Provisioner.NET RPM
dnf install HIRS_Provisioner.NET/hirs/bin/Release/**/linux-x64/*.rpm -y > /dev/null

# Initiate startup for IBMTSS Tools
startFreshTpmServer -f
startupTpm
installEkCert

setCiHirsAppsettingsFile

# Triggering a single provision for test
echo "==========="
echo "*** INITIAL TEST: Single Provision with Default Policy:"
echo "==========="
# Exercise the actual .NET transport and credential activation. tpm2-tools alone
# does not negotiate the MSSIM handshake required by Microsoft.TSS.
status=0
timeout --kill-after=5s 180s /usr/share/hirs/tpm_aca_provision --tcp --ip 127.0.0.1:2321 --sim || status=$?
if [ "$status" -ne 0 ]; then
  echo "Initial provisioning failed (status $status) with $HIRS_CI_TPM_SIM." >&2
  if [ "$HIRS_CI_TPM_SIM" = wolftpm ]; then
    echo "Check that the provisioner CI image was rebuilt with this branch's wolfTPM patches and wolfSSL options." >&2
  fi
  exit "$status"
fi
