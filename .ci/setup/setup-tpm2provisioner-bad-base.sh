#!/bin/bash

# Script to setup the TPM2 Provisioner Docker Image for Integration Tests
set -e

# Function to initialize the TPM2 Emulator with a bad base certificate
function InitTpm2Emulator {
	echo "===========Initializing TPM2 Emulator with bad base certificate...==========="

	# EK and PC Certificate
	ek_cert_der="/HIRS/.ci/setup/certs/ek_cert.der"
	platform_cert="badPlatformCertificate.der"

    echo "Creating Bad Base Platform Cert..."
    PC_DIR=/var/hirs/pc_generation
    ls -al $PC_DIR
	#mkdir -p $PC_DIR
    rm -f $PC_DIR/componentsFile
    rm -f $PC_DIR/optionsFile
    rm -f $PC_DIR/extentionsFile
    rm -f $PC_DIR/observerFile

	/opt/paccor/scripts/allcomponents.sh > $PC_DIR/componentsFile
	echo
    echo "PACCOR generated components file:"
    cat $PC_DIR/componentsFile

	echo "#1"
	ls -al /var/hirs/pc_generation

    #Add bad components and create badComponentsFile used below
    python /HIRS/.ci/setup/addFaultyComponents.py
    echo "#2"
	ls -al /var/hirs/pc_generation
	/opt/paccor/scripts/referenceoptions.sh > $PC_DIR/optionsFile
	/opt/paccor/scripts/otherextensions.sh > $PC_DIR/extensionsFile
	/opt/paccor/bin/observer -c $PC_DIR/badComponentsFile -p $PC_DIR/optionsFile -e $ek_cert_der -f $PC_DIR/observerFile
	/opt/paccor/bin/signer -o $PC_DIR/observerFile -x $PC_DIR/extensionsFile -b 20180101 -a 20280101 -N $RANDOM -k /HIRS/.ci/setup/certs/ca.key -P /HIRS/.ci/setup/certs/ca.crt -f $PC_DIR/$platform_cert

    echo
    echo "Generated bad components file:"
    cat $PC_DIR/badComponentsFile

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
	echo "Loading EK cert $ek_cert_der into NVRAM."
	tpm2_nvwrite -x 0x1c00002 -a 0x40000001 $ek_cert_der

	if tpm2_nvlist | grep -q 0x1c90000; then
	  echo "Released NVRAM for PC."
	  tpm2_nvrelease -x 0x1c90000 -a 0x40000001
	fi

	# Store the platform certificate in the TPM's NVRAM
	size=$(cat $PC_DIR/$platform_cert | wc -c)
	echo "Define NVRAM location for PC cert of size $size."
	tpm2_nvdefine -x 0x1c90000 -a 0x40000001 -t 0x2000A -s $size

	echo "Loading PC cert $PC_DIR/$platform_cert into NVRAM."
	tpm2_nvwrite -x 0x1c90000 -a 0x40000001 $PC_DIR/$platform_cert

	echo "===========TPM2 Emulator Initialization Complete!==========="

	# Set Logging to INFO Level
	sed -i "s/WARN/INFO/" /etc/hirs/TPM2_Provisioner/log4cplus_config.ini
}

# Install TPM2 Emulator
InitTpm2Emulator

tpm2_nvlist
echo ""
echo "===========HIRS ACA TPM2 Provisioner Setup Complete!==========="
