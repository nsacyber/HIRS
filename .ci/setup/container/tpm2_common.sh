#!/bin/bash
#########################################################################################
#  Support scripts for the TPM 2.0 Provisioner System Tests
#
#########################################################################################

# Function to write the appsettings.json file
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

function removeSavedProvisionerKeys {
  rm -f -- *ak_*.pub *ak_*.priv *ldevid.pub *ldevid.priv
}

function startupTpm {
  echo "Running tpm2_startup"
  tpm2_startup -T mssim -c
  sleep 2
}

function installEkCert {
  # Setting configurations
  . /hirs/.ci/docker/.env

  # The simulator gets a new endorsement seed after each reset. Certify its actual EK
  tpm2_createek -T mssim -G rsa -c 0x81010001 > /dev/null
  tpm2_readpublic -T mssim -c 0x81010001 -f pem -o /tmp/hirs-system-test-ek-public.pem > /dev/null
  openssl x509 -new -sha256 -days 3650 \
    -subj '/C=US/ST=ST/L=EXAMPLE/O=org/CN=dummy' \
    -force_pubkey /tmp/hirs-system-test-ek-public.pem \
    -CA "$HIRS_CI_TPM_EK_CA_CERT_FILE" -CAkey "$HIRS_CI_TPM_EK_CA_KEY_FILE" -CAkeyform DER \
    -set_serial 0x01642813778A \
    -extfile <(printf 'basicConstraints=critical,CA:FALSE\nkeyUsage=critical,keyEncipherment\n') \
    -outform DER -out "$HIRS_CI_TPM_EK_CERT_FILE"

  echo "Installing EK Cert $HIRS_CI_TPM_EK_CERT_FILE into TPM NVRAM at index $HIRS_CI_TPM_EK_CERT_NV_INDEX"
  tpm2_nvdefine -T mssim -C o -a "$HIRS_CI_TPM_EK_CERT_NV_ATTR" -s "$(stat -c %s "$HIRS_CI_TPM_EK_CERT_FILE")" "$HIRS_CI_TPM_EK_CERT_NV_INDEX"
  tpm2_nvwrite -T mssim -C o -i "$HIRS_CI_TPM_EK_CERT_FILE" "$HIRS_CI_TPM_EK_CERT_NV_INDEX"
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

