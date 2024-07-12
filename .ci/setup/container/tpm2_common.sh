#!/bin/bash
#########################################################################################
#  Support scripts for the TPM 2.0 Provisioner System Tests
#
#########################################################################################

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
  pushd /ibmtss  > /dev/null
    echo "Starting IBM TSS to set the TPM simulator initial values correctly..."
    cd utils
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
   sleep 3

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
   sleep 1

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

# Clear out existing TPM PCR values by restarting the ibm tpm simulator
function resetTpm2Emulator {
   echo "clearing the TPM PCR values"
   # Stop tpm2-abrmd and the tpm server
   pkill -f "tpm2-abrmd"
   pkill -f "tpm_server"
   # restart the tpm server and tpm2-abrmd
   /ibmtpm/src/./tpm_server &
   pushd /ibmtss/utils  > /dev/null
     ./startup
   popd > /dev/null
   tpm2-abrmd -t socket &
   sleep 1
   # tpm2_pcrlist -g sha256
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

# Function to update the hirs-site.config file
function setCiHirsAppsettingsFile {
  # Setting configurations
  . /hirs/.ci/docker/.env

  HIRS_APPSETTINGS_FILE=$HIRS_DEFAULT_APPSETTINGS_FILE
  ACA_ADDRESS="https://${HIRS_ACA_PORTAL_IP}:${HIRS_ACA_PORTAL_PORT}"
  EFI_PREFIX_PATH=$HIRS_CI_EFI_PATH_ROOT
  PACCOR_OUTPUT_FILE=""
  EVENT_LOG_FILE=""
  HARDWARE_MANIFEST_COLLECTORS="paccor_scripts"

  # Process parameters Argument handling 
  POSITIONAL_ARGS=()
  ORIGINAL_ARGS=("$@")
  while [[ $# -gt 0 ]]; do
    case $1 in
      --aca-address)
        shift # past argument
        ACA_ADDRESS=$1
        shift # past parameter
        ;;
      --efi-prefix)
        shift # past argument
        EFI_PREFIX_PATH=$1
	shift # past parameter
        ;;
      --paccor-output-file)
        shift # past argument
	PACCOR_OUTPUT_FILE=$1
	HARDWARE_MANIFEST_COLLECTORS=""
        shift # past parameter
        ;;
      --event-log-file)
        shift # past argument
        EVENT_LOG_FILE=$1
        shift # past argument
        ;;
      --linux-dmi)
	USE_LINUX_DMI=YES
	shift # past argument
	;;
    -*|--*)
        echo "setCiHirsAppsettingsFile: Unknown option $1"
        shift # past argument
        ;;
      *)
       POSITIONAL_ARGS+=("$1") # save positional arg
       # shift # past argument
       break
        ;;
    esac
  done
  echo ""
  echo "===========Updating ${HIRS_APPSETTINGS_FILE}, using values from /HIRS/.ci/docker/.env file...==========="

  cat <<DEFAULT_APPSETTINGS_FILE > $HIRS_APPSETTINGS_FILE
{
  "auto_detect_tpm":  "TRUE",
  "aca_address_port": "$ACA_ADDRESS",
  "efi_prefix": "$EFI_PREFIX_PATH",
  "paccor_output_file": "$PACCOR_OUTPUT_FILE",
  "event_log_file": "$EVENT_LOG_FILE",
  "hardware_manifest_collectors": "$HARDWARE_MANIFEST_COLLECTORS",
DEFAULT_APPSETTINGS_FILE
  if [ "$USE_LINUX_DMI" = YES ]; then
    cat <<DEFAULT_APPSETTINGS_FILE >> $HIRS_APPSETTINGS_FILE
  "linux_bios_vendor_file": "$HIRS_CI_TEST_ROOT/dmi/id/bios_vendor",
  "linux_bios_version_file": "$HIRS_CI_TEST_ROOT/dmi/id/bios_version",
  "linux_bios_date_file": "$HIRS_CI_TEST_ROOT/dmi/id/bios_date",
  "linux_sys_vendor_file": "$HIRS_CI_TEST_ROOT/dmi/id/sys_vendor",
  "linux_product_name_file": "$HIRS_CI_TEST_ROOT/dmi/id/product_name",
  "linux_product_version_file": "$HIRS_CI_TEST_ROOT/dmi/id/product_version",
  "linux_product_serial_file": "$HIRS_CI_TEST_ROOT/dmi/id/product_serial",
DEFAULT_APPSETTINGS_FILE
  fi
  cat <<DEFAULT_APPSETTINGS_FILE >> $HIRS_APPSETTINGS_FILE
  "Serilog": {
    "Using": [ "Serilog.Sinks.Console", "Serilog.Sinks.File" ],
    "Enrich": [ "FromLogContext", "WithMachineName", "WithProcessId", "WithThreadId" ],
    "MinimumLevel": {
      "Default": "Debug",
      "Override": {
        "Microsoft": "Warning",
        "System": "Warning"
      }
    },
    "WriteTo": [
      {
        "Name": "Console",
        "Args": {
          "outputTemplate": "{Message}{NewLine}",
          "theme": "Serilog.Sinks.SystemConsole.Themes.SystemConsoleTheme::Grayscale, Serilog.Sinks.Console",
          "restrictedToMinimumLevel": "Information"
        }
      },
      {
        "Name": "File",
        "Args": {
          "path": "hirs.log",
          "rollingInterval": "Day",
          "retainedFileCountLimit": 5
        }
      }
    ]
  }
}
DEFAULT_APPSETTINGS_FILE
}

# These functions work on the tpm2provisioner_dotnet image 
# They assume the IBM sw tpm server repo is cloned to /ibmswtpm2
# They assume the IBM tss repo is cloned to /ibmtss
# They assume tpm2-tools are installed.
# They assume the HIRS repo is cloned to /hirs.
function startFreshTpmServer {
  # Process parameters Argument handling 
  POSITIONAL_ARGS=()
  ORIGINAL_ARGS=("$@")
  while [[ $# -gt 0 ]]; do
    case $1 in
      -f|--force|--restart)
	stopTpmServer
	sleep 5
	shift # past argument
	;;
    -*|--*)
        echo "setCiHirsAppsettingsFile: Unknown option $1"
        shift # past argument
        ;;
      *)
       POSITIONAL_ARGS+=("$1") # save positional arg
       # shift # past argument
       break
        ;;
    esac
  done

  if isTpmServerRunning ; then
    echo "TPM server already running."
  else
    echo -n "Starting TPM server..."
    /ibmswtpm2/src/tpm_server -rm &> /dev/null &
    sleep 2
    pid=$(findTpmServerPid)
    echo "...running with pid: $pid"
  fi
}

function startupTpm {
  echo "Running tpm2_startup"
  tpm2_startup -T mssim -c
  sleep 2
}

function installEkCert {
  # Setting configurations
  . /hirs/.ci/docker/.env
  
  echo "Installing EK Cert $HIRS_CI_TPM_EK_CERT_FILE into TPM NVRAM at index $HIRS_CI_TPM_EK_CERT_NV_INDEX"
  tpm2_nvdefine -T mssim -C o -a $HIRS_CI_TPM_EK_CERT_NV_ATTR -s $(cat $HIRS_CI_TPM_EK_CERT_FILE | wc -c) $HIRS_CI_TPM_EK_CERT_NV_INDEX
  tpm2_nvwrite -T mssim -C o -i $HIRS_CI_TPM_EK_CERT_FILE $HIRS_CI_TPM_EK_CERT_NV_INDEX
  echo "Finished installing EK cert."
}

function findTpmServerPid {
  pid=$(pgrep -f /ibmswtpm2/src/tpm_server 2> /dev/null)
  echo -n "$pid"
}

# ex usage: isTpmServerRunning && echo "up" || echo "down"
function isTpmServerRunning {
  tpmUp=$(findTpmServerPid)
  if [ -n "$tpmUp" ]; then
    return 0
  else
    return 1
  fi
}

function stopTpmServer {
  tpmUp=$(findTpmServerPid)
  if [ -n "$tpmUp" ]; then
    echo "Stopping TPM server with pid: $tpmUp"
    kill -9 $tpmUp
  fi
}

# Wait for ACA to boot
function waitForAca {
  echo "Waiting for ACA to spin up at address ${HIRS_ACA_PORTAL_IP} on port ${HIRS_ACA_PORTAL_PORT} ..."
  until [ "`curl --silent -I -k https://${HIRS_ACA_PORTAL_IP}:${HIRS_ACA_PORTAL_PORT}/HIRS_AttestationCAPortal | grep 'HTTP/1.1 200'`" != "" ]; do
    sleep 1;
  done
  echo "ACA is up!"
}

