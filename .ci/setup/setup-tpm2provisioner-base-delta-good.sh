#!/bin/bash

# Script to setup the TPM 2.0 Provisioner Docker Image for System Tests Base/Delta(Good)
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

# Function to initialize the TPM2 Emulator with a good base certificate
function InitTpm2Emulator {
	echo "===========Initializing TPM2 Emulator with good base certificate...==========="

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

	# Give tpm_server time to start and register on the DBus
	sleep 5

	tpm2-abrmd -t socket &
	echo "TPM2-Abrmd started"

	# Give ABRMD time to start and register on the DBus
	sleep 5

	# Certificates
	ek_cert="/HIRS/.ci/setup/certs/ek_cert.der"
	ca_key="/HIRS/.ci/setup/certs/ca.key"
	ca_cert="/HIRS/.ci/setup/certs/ca.crt"
	pBase_certA="PBaseCertA.der"
	pBase_certB="PBaseCertB.der"
	si_delta_cert_A1="SIDeltaCertA1.der"
	si_delta_cert_A2="SIDeltaCertA2.der"
	si_delta_cert_A2_resolved="SIDeltaCertA2_resolved.der"
	si_delta_cert_A3="SIDeltaCertA3.der"
	var_delta_cert_A1="VARDeltaCertA1.der"
	var_delta_cert_A2="VARDeltaCertA2.der"
	var_delta_cert_A2_resolved="VARDeltaCertA2_resolved.der"

	# PACCOR directory
	PC_DIR=/var/hirs/pc_generation
	mkdir -p $PC_DIR

    echo "Running PACCOR to generate local component information..."
    # Use specific PACCOR script for system testing.
	# Will provide default component SN#s when needed.
	cp -f /opt/paccor/scripts/allcomponents_hirs_system_tests.sh /opt/paccor/scripts/allcomponents.sh
	/opt/paccor/scripts/allcomponents.sh > $PC_DIR/componentsFile

	# Split into JSON files needed to generate the certificates
	python /HIRS/.ci/setup/createDeltaComponentsForPBaseCertA.py
	echo

	# Generate certificates in the order they'll be used in the system tests.
	# And stager the begin dates properly (the -b option for the /opt/paccor/bin/signer)
	echo "Generating certificates..."
	echo "Generating $pBase_certA..."
	/opt/paccor/scripts/referenceoptions.sh > $PC_DIR/optionsFile
	/opt/paccor/scripts/otherextensions.sh > $PC_DIR/extensionsFile
	/opt/paccor/bin/observer -c $PC_DIR/PBaseCertA.componentlist.json -p $PC_DIR/optionsFile -e $ek_cert -f $PC_DIR/observerFile
	/opt/paccor/bin/signer -c $PC_DIR/PBaseCertA.componentlist.json -o $PC_DIR/observerFile -x $PC_DIR/extensionsFile -b 20180101 -a 20280101 -N $RANDOM -k $ca_key -P $ca_cert -f $PC_DIR/$pBase_certA
	echo "Done"

	# Generate the PBaseCertB certificate. Just need to copy from PBaseCertA.
	echo "Generating $pBase_certB..."
	cp $PC_DIR/$pBase_certA $PC_DIR/$pBase_certB
	echo "Done"

    # Generate the SIDeltaCertA1 certificate
    echo "Generating $si_delta_cert_A1, using $pBase_certA..."
    rm -f $PC_DIR/observerFile
	/opt/paccor/bin/observer -c $PC_DIR/SIDeltaCertA1.componentlist.json -p $PC_DIR/optionsFile -e $PC_DIR/$pBase_certA -f $PC_DIR/observerFile
	/opt/paccor/bin/signer -c $PC_DIR/SIDeltaCertA1.componentlist.json -o $PC_DIR/observerFile -x $PC_DIR/extensionsFile -b 20180201 -a 20280101 -N $RANDOM -k $ca_key -P $ca_cert -e $PC_DIR/$pBase_certA -f $PC_DIR/$si_delta_cert_A1
	echo "Done"

	# Generate the VARDeltaCertA1 certificate
    echo "Generating $var_delta_cert_A1, using $pBase_certA..."
    rm -f $PC_DIR/observerFile
	/opt/paccor/bin/observer -c $PC_DIR/VARDeltaCertA1.componentlist.json -p $PC_DIR/optionsFile -e $PC_DIR/$pBase_certA -f $PC_DIR/observerFile
	/opt/paccor/bin/signer -c $PC_DIR/VARDeltaCertA1.componentlist.json -o $PC_DIR/observerFile -x $PC_DIR/extensionsFile -b 20180301 -a 20280101 -N $RANDOM -k $ca_key -P $ca_cert -e $PC_DIR/$pBase_certA -f $PC_DIR/$var_delta_cert_A1
	echo "Done"

    # Generate the SIDeltaCertA2 certificate
    echo "Generating $si_delta_cert_A2, using $pBase_certA..."
    rm -f $PC_DIR/observerFile
	/opt/paccor/bin/observer -c $PC_DIR/SIDeltaCertA2.componentlist.json -p $PC_DIR/optionsFile -e $PC_DIR/$pBase_certA -f $PC_DIR/observerFile
	/opt/paccor/bin/signer -c $PC_DIR/SIDeltaCertA2.componentlist.json -o $PC_DIR/observerFile -x $PC_DIR/extensionsFile -b 20180401 -a 20280101 -N $RANDOM -k $ca_key -P $ca_cert -e $PC_DIR/$pBase_certA -f $PC_DIR/$si_delta_cert_A2
	echo "Done"

	# Generate the SIDeltaCertA2_resolved certificate
    echo "Generating $si_delta_cert_A2_resolved, using $pBase_certA..."
    rm -f $PC_DIR/observerFile
	/opt/paccor/bin/observer -c $PC_DIR/SIDeltaCertA2.resolved.componentlist.json -p $PC_DIR/optionsFile -e $PC_DIR/$pBase_certA -f $PC_DIR/observerFile
	/opt/paccor/bin/signer -c $PC_DIR/SIDeltaCertA2.resolved.componentlist.json -o $PC_DIR/observerFile -x $PC_DIR/extensionsFile -b 20180501 -a 20280101 -N $RANDOM -k $ca_key -P $ca_cert -e $PC_DIR/$pBase_certA -f $PC_DIR/$si_delta_cert_A2_resolved
	echo "Done"

	# Generate the VARDeltaCertA2 certificate
    echo "Generating $var_delta_cert_A2, using $pBase_certA..."
    rm -f $PC_DIR/observerFile
	/opt/paccor/bin/observer -c $PC_DIR/VARDeltaCertA2.componentlist.json -p $PC_DIR/optionsFile -e $PC_DIR/$pBase_certA -f $PC_DIR/observerFile
	/opt/paccor/bin/signer -c $PC_DIR/VARDeltaCertA2.componentlist.json -o $PC_DIR/observerFile -x $PC_DIR/extensionsFile -b 20180601 -a 20280101 -N $RANDOM -k $ca_key -P $ca_cert -e $PC_DIR/$pBase_certA -f $PC_DIR/$var_delta_cert_A2
	echo "Done"

	# Generate the VARDeltaCertA2_resolved certificate
    echo "Generating $var_delta_cert_A2_resolved, using $pBase_certA..."
    rm -f $PC_DIR/observerFile
	/opt/paccor/bin/observer -c $PC_DIR/VARDeltaCertA2.resolved.componentlist.json -p $PC_DIR/optionsFile -e $PC_DIR/$pBase_certA -f $PC_DIR/observerFile
	/opt/paccor/bin/signer -c $PC_DIR/VARDeltaCertA2.resolved.componentlist.json -o $PC_DIR/observerFile -x $PC_DIR/extensionsFile -b 20180701 -a 20280101 -N $RANDOM -k $ca_key -P $ca_cert -e $PC_DIR/$pBase_certA -f $PC_DIR/$var_delta_cert_A2_resolved
	echo "Done"

	# Generate the SIDeltaCertA3 certificate
    echo "Generating $si_delta_cert_A3, using $si_delta_cert_A1 as Base..."
    rm -f $PC_DIR/observerFile
	/opt/paccor/bin/observer -c $PC_DIR/SIDeltaCertA3.componentlist.json -p $PC_DIR/optionsFile -e $PC_DIR/$si_delta_cert_A1 -f $PC_DIR/observerFile
	/opt/paccor/bin/signer -c $PC_DIR/SIDeltaCertA3.componentlist.json -o $PC_DIR/observerFile -x $PC_DIR/extensionsFile -b 20180801 -a 20280101 -N $RANDOM -k $ca_key -P $ca_cert -e $PC_DIR/$si_delta_cert_A1 -f $PC_DIR/$si_delta_cert_A3
	echo "Done"

	# Release EK nvram
	if tpm2_nvlist | grep -q 0x1c00002; then
	  echo "Released NVRAM for EK."
	  tpm2_nvrelease -x 0x1c00002 -a 0x40000001
	fi

	# Define nvram space to enable loading of EK cert (-x NV Index, -a handle to
	# authorize [0x40000001 = ownerAuth handle], -s size [defaults to 2048], -t
	# specifies attribute value in publicInfo struct
	# [0x2000A = ownerread|ownerwrite|policywrite])
	size=$(cat $ek_cert | wc -c)
	echo "Define NVRAM location for EK cert of size $size."
	tpm2_nvdefine -x 0x1c00002 -a 0x40000001 -t 0x2000A -s $size

	# Load key into TPM nvram
	echo "Loading EK cert $ek_cert into NVRAM."
	tpm2_nvwrite -x 0x1c00002 -a 0x40000001 $ek_cert

	# Release PC nvram
	if tpm2_nvlist | grep -q 0x1c90000; then
	  echo "Released NVRAM for PC."
	  tpm2_nvrelease -x 0x1c90000 -a 0x40000001
	fi

	# Store the platform certificate in the TPM's NVRAM
	size=$(cat $PC_DIR/$pBase_certA | wc -c)
	echo "Define NVRAM location for PC cert of size $size."
	tpm2_nvdefine -x 0x1c90000 -a 0x40000001 -t 0x2000A -s $size

	echo "Loading PC cert $PC_DIR/$pBase_certA into NVRAM."
	tpm2_nvwrite -x 0x1c90000 -a 0x40000001 $PC_DIR/$pBase_certA

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
echo "TPM2 Emulator NV RAM list"
tpm2_nvlist

echo ""
echo "===========HIRS ACA TPM2 Provisioner Setup Complete!==========="
