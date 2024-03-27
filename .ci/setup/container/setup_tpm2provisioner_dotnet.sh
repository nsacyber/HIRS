#!/bin/bash
#########################################################################################
#  Script to setup the TPM Provisioner.NET for System Tests
#########################################################################################

# Setting configurations
. ./.ci/docker/.env

set -a

set -e
pushd /  > /dev/null
echo "Setting up TPM emulator for the TPM2 Provisioner"

# Wait for ACA to boot
echo "Waiting for ACA to spin up at address ${HIRS_ACA_PORTAL_IP} on port ${HIRS_ACA_PORTAL_PORT} ..."
  until [ "`curl --silent -I -k https://${HIRS_ACA_PORTAL_IP}:${HIRS_ACA_PORTAL_PORT}/HIRS_AttestationCAPortal | grep 'HTTP/1.1 200'`" != "" ]; do
    sleep 1;
  done
  echo "ACA is up!"

# Un-package Provisioner.NET RPM
cd /
yes | dnf install /hirs/HIRS_Provisioner.NET/hirs/bin/Release/net6.0/linux-x64/HIRS_Provisioner.NET.2.2.0.linux-x64.rpm
echo "HIRS Provisioner.NET RPM has been opened"

# Start TPM simulator server
./ibmswtpm2/src/tpm_server &
echo "TPM Simulator Server has started"

# Create EK Certificate
cd /ibmtss/utils || exit
./startup
./createekcert -rsa 2048 -cakey cakey.pem -capwd rrrr -v
cd / || exit
echo "EK certificate has been created using IBMTSS CA Key"

# Writing to Provisioner.Net configurations file for modified aca port and efi prefix
cat <<APPSETTINGS_FILE > /usr/share/hirs/appsettings.json
{
  "auto_detect_tpm":  "TRUE",
  "aca_address_port": "https://${HIRS_ACA_PORTAL_IP}:${HIRS_ACA_PORTAL_PORT}",
  "efi_prefix": "/boot/efi",
  "paccor_output_file": "",
  "event_log_file":  "",
  "hardware_manifest_collectors": "paccor_scripts",

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
APPSETTINGS_FILE
echo "Provisioner.Net's appsettings.json file has been edited"

# Uploading CA Certificate to HIRS ACA Portal
curl -k -s -F "file=@/ibmtss/utils/certificates/cacert.pem" https://${HIRS_ACA_PORTAL_IP}:${HIRS_ACA_PORTAL_PORT}/HIRS_AttestationCAPortal/portal/certificate-request/trust-chain/upload
echo "CA Certificate has been uploaded to HIRS ACA Portal"

# Starting Provisioning
./usr/share/hirs/tpm_aca_provision --tcp --ip 127.0.0.1:2321 --sim
echo "Exiting and removing Docker network and ACA and Provisioner.Net containers..."