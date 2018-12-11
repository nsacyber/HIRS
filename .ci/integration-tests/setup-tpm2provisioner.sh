# Script to setup the TPM2 Provisioner Docker Image for Integration Tests

set -e

# TODO: See note about modular packaging in ACA script
pushd /HIRS
# ./package/package.centos.sh
yum install -y package/rpm/RPMS/x86_64/HIRS_Provisioner_TPM_2_0*.el7.x86_64.rpm
popd

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

echo "Creating Platform Cert for Container"
PC_DIR=/var/hirs/pc_generation
mkdir -p $PC_DIR
/opt/paccor/scripts/allcomponents.sh > $PC_DIR/componentsFile
/opt/paccor/scripts/referenceoptions.sh > $PC_DIR/optionsFile
/opt/paccor/scripts/otherextensions.sh > $PC_DIR/extensionsFile
/opt/paccor/bin/observer -c $PC_DIR/componentsFile -p $PC_DIR/optionsFile -e $ek_cert_der -f $PC_DIR/observerFile
/opt/paccor/bin/signer -o $PC_DIR/observerFile -x $PC_DIR/extensionsFile -b 20180101 -a 20280101 -N $RANDOM -k /HIRS/.ci/integration-tests/certs/ca.key -P /HIRS/.ci/integration-tests/certs/ca.crt --pem -f $PC_DIR/$platform_cert

# Define nvram space to enable loading of EK cert (-x NV Index, -a handle to
# authorize [0x40000001 = ownerAuth handle], -s size [defaults to 2048], -t
# specifies attribute value in publicInfo struct
# [0x2000A = ownerread|ownerwrite|policywrite])
size=$(cat $ek_cert_der | wc -c)
echo "Define nvram location for ek cert of size $size"
tpm2_nvdefine -x 0x1c00002 -a 0x40000001 -t 0x2000A -s $size

# Load key into TPM nvram
echo "Load ek cert into nvram"
tpm2_nvwrite -x 0x1c00002 -a 0x40000001 $ek_cert_der

# Store the platform certificate in the TPM's NVRAM
tpm2_nvdefine -x 0x1c90000 -a 0x40000001 -t 0x2000A -s $(cat $PC_DIR/$platform_cert | wc -c)
tpm2_nvwrite -x 0x1c90000 -a 0x40000001 $PC_DIR/$platform_cert

# Set Logging to INFO Level
sed -i "s/WARN/INFO/" /etc/hirs/TPM2_Provisioner/log4cplus_config.ini

tail -f /dev/null
