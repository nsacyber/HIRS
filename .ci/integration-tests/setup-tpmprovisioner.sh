#!/bin/bash

# Script to setup the TPM Provisioner Docker Image for Integration Tests
set -e

# Wait for ACA to boot
echo "Waiting for ACA to spin up..."
until [ "`curl --silent --connect-timeout 1 -I -k https://${HIRS_ACA_PORTAL_IP}:${HIRS_ACA_PORTAL_PORT}/HIRS_AttestationCAPortal | grep '302 Found'`" != "" ]; do
  :
done
echo "ACA is up!"

# Function to install Provisioner packages.
function InstallProvisioner {
	echo "===========Installing Provisioner Packages...==========="

  pushd /HIRS
  if [ ! -d package/rpm/RPMS ]; then
      ./package/package.centos.sh
  fi
  yum install -y package/rpm/RPMS/noarch/HIRS_Provisioner_TPM_1_2*.el7.noarch.rpm
  popd
}

# Function to initialize the TPM Emulator
function InitTpmEmulator {
	echo "===========Initializing TPM Emulator...==========="

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

  # Activate Software TPM
  ./tpm/tpm_server&
  ./libtpm/utils/tpmbios

  # Restart Software TPM after Activation
  pkill tpm_server
  ./tpm/tpm_server&
  ./libtpm/utils/tpmbios

  # Create EK on Software TPM
  ./libtpm/utils/createek

  # Initialize last memory address
  ./libtpm/utils/nv_definespace -in ffffffff -sz 0

  popd

  echo "Starting TrouSerS Daemon"
  tcsd -e

  echo "Testing TPM Connectivity"
  tpm_selftest

	echo "===========TPM Emulator Initialization Complete!==========="
}

# Function to update the hirs-site.config file
function UpdateHirsSiteConfigFile {
	HIRS_SITE_CONFIG="/etc/hirs/hirs-site.config"

	echo ""
	echo "===========Updating ${HIRS_SITE_CONFIG}, using values from /HIRS/.ci/docker/.env file...==========="
	cat /HIRS/.ci/docker/.env

	cat <<DEFAULT_SITE_CONFIG_FILE > $HIRS_SITE_CONFIG
#*******************************************
#* HIRS site configuration properties file
#*******************************************
CLIENT_HOSTNAME=${HIRS_ACA_PROVISIONER_IP}
TPM_ENABLED=${TPM_ENABLED}
IMA_ENABLED=${IMA_ENABLED}

# Site-specific configuration
ATTESTATION_CA_FQDN=${HIRS_ACA_PORTAL_IP}
ATTESTATION_CA_PORT=${HIRS_ACA_PORTAL_PORT}
BROKER_FQDN=${HIRS_ACA_PORTAL_IP}
BROKER_PORT=${HIRS_BROKER_PORT}
PORTAL_FQDN=${HIRS_ACA_PORTAL_IP}
PORTAL_PORT=${HIRS_ACA_PORTAL_PORT}

DEFAULT_SITE_CONFIG_FILE

 echo "===========New HIRS Config File==========="
 cat /etc/hirs/hirs-site.config
}

# Install packages
InstallProvisioner

# Install TPM Emulator
InitTpmEmulator

# Update the hirs-site.config file
UpdateHirsSiteConfigFile

echo ""
echo "===========HIRS ACA Provisioner Setup Complete!==========="

# hirs-provisioner -p
