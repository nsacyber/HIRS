#!/bin/bash

# Script to setup the TPM2 Provisioner Docker Image for Integration Tests
set -e

# Wait for ACA to boot
echo "Waiting for ACA to spin up..."
until [ "`curl --silent --connect-timeout 1 -I -k https://${HIRS_ACA_PORTAL_IP}:${HIRS_ACA_PORTAL_PORT}/HIRS_AttestationCAPortal | grep '302 Found'`" != "" ]; do
  :
done
echo "ACA is up!"

# Function to install TPM2 Provisioner packages.
function InstallProvisioner {
	echo "===========Installing TPM2 Provisioner Packages...==========="

	pushd /HIRS
	if [ ! -d package/rpm/RPMS ]; then
    	./package/package.centos.sh
	fi
	yum install -y package/rpm/RPMS/x86_64/HIRS_Provisioner_TPM_2_0*.el7.x86_64.rpm
	popd
}

# Function to initialize the TPM2 Emulator
function InitTpm2Emulator {
	echo "===========Initializing TPM2 Emulator...==========="

	mkdir -p /var/run/dbus
	if [ -e /var/run/dbus/pid ]; then
	  rm /var/run/dbus/pid
	fi

	if [ -e /var/run/dbus/system_bus_socket ]; then
	  rm /var/run/dbus/system_bus_socket
	fi

	# Start the DBus
	dbus-daemon --fork --system
	echo "DBus started"

	# Give DBus time to start up
	sleep 5

	/ibmtpm/src/./tpm_server &
	echo "TPM Emulator started"

	tpm2-abrmd -t socket &
	echo "TPM2-Abrmd started"

	# Give ABRMD time to start and register on the DBus
	sleep 5

	# EK and PC Certificate
	ek_cert_der="/HIRS/.ci/integration-tests/certs/ek_cert.der"
	platform_cert="platformAttributeCertificate.pem"

	echo "Creating Platform Cert for Container."
	PC_DIR=/var/hirs/pc_generation
	mkdir -p $PC_DIR
	/opt/paccor/scripts/allcomponents.sh > $PC_DIR/componentsFile
	/opt/paccor/scripts/referenceoptions.sh > $PC_DIR/optionsFile
	/opt/paccor/scripts/otherextensions.sh > $PC_DIR/extensionsFile
	/opt/paccor/bin/observer -c $PC_DIR/componentsFile -p $PC_DIR/optionsFile -e $ek_cert_der -f $PC_DIR/observerFile
	/opt/paccor/bin/signer -o $PC_DIR/observerFile -x $PC_DIR/extensionsFile -b 20180101 -a 20280101 -N $RANDOM -k /HIRS/.ci/integration-tests/certs/ca.key -P /HIRS/.ci/integration-tests/certs/ca.crt --pem -f $PC_DIR/$platform_cert

	if tpm2_nvlist | grep -q 0x1c00002; then
	  echo "Released NVRAM for EK."
	  tpm2_nvrelease -x 0x1c00002 -a 0x40000001
	fi

	# Define nvram space to enable loading of EK cert (-x NV Index, -a handle to
	# authorize [0x40000001 = ownerAuth handle], -s size [defaults to 2048], -t
	# specifies attribute value in publicInfo struct
	# [0x2000A = ownerread|ownerwrite|policywrite])
	size=$(cat $ek_cert_der | wc -c)
	echo "Define NVRAM location for EK cert of size $size."
	tpm2_nvdefine -x 0x1c00002 -a 0x40000001 -t 0x2000A -s $size

	# Load key into TPM nvram
	echo "Loading EK cert into NVRAM."
	tpm2_nvwrite -x 0x1c00002 -a 0x40000001 $ek_cert_der

	if tpm2_nvlist | grep -q 0x1c90000; then
	  echo "Released NVRAM for PC."
	  tpm2_nvrelease -x 0x1c90000 -a 0x40000001
	fi

	# Store the platform certificate in the TPM's NVRAM
	size=$(cat $PC_DIR/$platform_cert | wc -c)
	echo "Define NVRAM location for PC cert of size $size."
	tpm2_nvdefine -x 0x1c90000 -a 0x40000001 -t 0x2000A -s $size

	echo "Loading PC cert into NVRAM."
	tpm2_nvwrite -x 0x1c90000 -a 0x40000001 $PC_DIR/$platform_cert

	echo "===========TPM2 Emulator Initialization Complete!==========="

	# Set Logging to INFO Level
	sed -i "s/WARN/INFO/" /etc/hirs/TPM2_Provisioner/log4cplus_config.ini
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
CLIENT_HOSTNAME=${HIRS_ACA_PROVISIONER_TPM2_IP}
TPM_ENABLED=${TPM_ENABLED}
IMA_ENABLED=${IMA_ENABLED}

# Site-specific configuration
ATTESTATION_CA_FQDN=${HIRS_ACA_HOSTNAME}
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

# Install TPM2 Emulator
InitTpm2Emulator

# Update the hirs-site.config file
UpdateHirsSiteConfigFile

echo ""
echo "===========HIRS ACA TPM2 Provisioner Setup Complete!==========="
