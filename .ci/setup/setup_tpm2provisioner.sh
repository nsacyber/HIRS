#!/bin/bash
#########################################################################################
#  Script to setup the TPM 2.0 Provisioner Docker Image for System Tests
#
#########################################################################################
set -e
pushd /  > /dev/null
echo "Setting up TPM emulator for the TPM2 Provisioner"

# Function to make and install TPM 2.0 Provisioner packages
function installProvisioner {
   echo "===========Installing TPM 2.0 Provisioner Packages...==========="
   pushd /HIRS  > /dev/null
    echo "Building the HIRS Provisioner ..."
    mkdir -p /HIRS/logs/provisioner/
    sh package/package.centos.sh &> /HIRS/logs/provisioner/provisioner_build.log
    echo "Installing the HIRS Provisioner ..."
    yum install -y package/rpm/RPMS/x86_64/HIRS_Provisioner_TPM_2_0*.el7.x86_64.rpm
  popd  > /dev/null
}

# use ibm tss to properly clear tpm pcr values
function setTpmPcrValues {
  mkdir /ibmtss
  pushd /ibmtss  > /dev/null
    echo "Installing IBM TSS to set the TPM simulator intial values correctly..."
    wget --no-check-certificate https://downloads.sourceforge.net/project/ibmtpm20tss/ibmtss1.6.0.tar.gz > /dev/null
    tar -zxvf ibmtss1.6.0.tar.gz > /dev/null
    cd utils
    make -f makefiletpmc > /dev/null
    cd ../utils
    ./startup
  popd  > /dev/null
}

# Set default values tcg_boot_properties
function setTcgProperties {
  propFile="/etc/hirs/tcg_boot.properties";

  echo "tcg.rim.dir=/boot/tcg/manifest/rim/" > $propFile;
  echo "tcg.swidtag.dir=/boot/tcg/manifest/swidtag/" >> $propFile;
  echo "tcg.cert.dir=/boot/tcg/cert/platform/" >> $propFile;
  echo "tcg.event.file=/sys/kernel/security/tpm0/binary_bios_measurements" >> $propFile;
}

# Function to initialize the TPM 2.0 Emulator
function initTpm2Emulator {
   echo "===========Initializing TPM 2.0 Emulator...==========="

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

   sleep 1
   # Use the ibmtss to clear the PCR values (tpm2-abrmd will currupt PCR0)
   setTpmPcrValues
   # Give tpm_server time to start and register on the DBus
   sleep 1

   tpm2-abrmd -t socket &
   echo "TPM2-Abrmd started"

   # Give ABRMD time to start and register on the DBus
   sleep 2

   # Certificates
   ek_cert="/HIRS/.ci/setup/certs/ek_cert.der"
   ca_key="/HIRS/.ci/setup/certs/ca.key"
   ca_cert="/HIRS/.ci/setup/certs/ca.crt"
   platform_cert="platformAttributeCertificate.der"

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

   if tpm2_nvlist | grep -q 0x1c90000; then
     echo "Released NVRAM for PC."
     tpm2_nvrelease -x 0x1c90000 -a 0x40000001
   fi

   echo "===========TPM 2.0 Emulator Initialization Complete!==========="

   # Set Logging to INFO Level
   sed -i "s/WARN/INFO/" /etc/hirs/TPM2_Provisioner/log4cplus_config.ini
}

# Function to update the hirs-site.config file
function updateHirsSiteConfigFile {
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

function waitForAca {
# Wait for ACA to boot
  echo "Waiting for ACA to spin up at address ${HIRS_ACA_PORTAL_IP} on port ${HIRS_ACA_PORTAL_PORT} ..."
  until [ "`curl --silent --connect-timeout 1 -I -k https://${HIRS_ACA_PORTAL_IP}:${HIRS_ACA_PORTAL_PORT}/HIRS_AttestationCAPortal | grep '302 Found'`" != "" ]; do
    sleep 1;
  #echo "Checking on the ACA..."
  done
  echo "ACA is up!"
}

#Wait for the ACA to spin up, if it hasnt already
waitForAca

# Install packages
installProvisioner

# set location of tcg artifacts
setTcgProperties
#echo "Contents of /etc/hirs is $(ls -al /etc/hirs)";

# Install TPM 2.0 Emulator
initTpm2Emulator

# Update the hirs-site.config file
updateHirsSiteConfigFile

echo "TPM 2.0 Emulator NV RAM list"
tpm2_nvlist

echo ""
echo "===========HIRS ACA TPM 2.0 Provisioner Setup Complete!==========="

popd > /dev/null