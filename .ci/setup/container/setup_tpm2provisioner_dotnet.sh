#!/bin/bash
#########################################################################################
#  Script to setup the TPM Provisioner.NET for System Tests
#########################################################################################

# Setting configurations
. ./.ci/docker/.env

set -a

set -e
echo "*** Setting up TPM emulator for the TPM2 Provisioner *** "

# Wait for ACA to boot
echo "*** Waiting for ACA to spin up at address ${HIRS_ACA_PORTAL_IP} on port ${HIRS_ACA_PORTAL_PORT} ..."
  until [ "`curl --silent -I -k https://${HIRS_ACA_PORTAL_IP}:${HIRS_ACA_PORTAL_PORT}/HIRS_AttestationCAPortal | grep 'HTTP/1.1 200'`" != "" ]; do
    sleep 1;
  done
  echo "*** ACA is up!"

## Un-package Provisioner.NET RPM
yes | dnf install HIRS_Provisioner.NET/hirs/bin/Release/net6.0/linux-x64/HIRS_Provisioner.NET.2.2.0.linux-x64.rpm -y > /dev/null

# Initiate startup for IBMTSS Tools
pushd /ibmtss/utils
tpm2_startup -T mssim -c &
sleep 5
tpm2_nvdefine -T mssim -C o -a 0x2000A -s $(cat /hirs/.ci/setup/certs/ek_cert.der | wc -c) 0x1c00002
tpm2_nvwrite -T mssim -C o -i /hirs/.ci/setup/certs/ek_cert.der 0x1c00002
popd

# Writing to Provisioner.Net configurations file for modified aca port and efi prefix
cat <<APPSETTINGS_FILE > /usr/share/hirs/appsettings.json
{
  "auto_detect_tpm":  "TRUE",
  "aca_address_port": "https://${HIRS_ACA_PORTAL_IP}:${HIRS_ACA_PORTAL_PORT}",
  "efi_prefix": "/ci_test/boot/efi",
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
cp /usr/share/hirs/appsettings.json /usr/share/hirs/appsettings_default.json
cat <<APPSETTINGS_FILE_HW > /usr/share/hirs/appsettings_hw.json
{
  "auto_detect_tpm":  "TRUE",
  "aca_address_port": "https://172.19.0.2:8443",
  "efi_prefix": "/ci_test/boot/efi",
  "paccor_output_file": "/ci_test/hw.json",
  "event_log_file":  "/ci_test/binary_bios_measurements",
  "hardware_manifest_collectors": "",
  "linux_bios_vendor_file": "/ci_test/dmi/id/bios_vendor",
  "linux_bios_version_file": "/ci_test/dmi/id/bios_version",
  "linux_bios_date_file": "/ci_test/dmi/id/bios_date",
  "linux_sys_vendor_file": "/ci_test/dmi/id/sys_vendor",
  "linux_product_name_file": "/ci_test/dmi/id/product_name",
  "linux_product_version_file": "/ci_test/dmi/id/product_version",
  "linux_product_serial_file": "/ci_test/dmi/id/product_serial",

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
APPSETTINGS_FILE_HW

# Triggering a single provision for test
echo "==========="
echo "*** INITIAL TEST: Single Provision with Default Policy:"
echo "==========="
/usr/share/hirs/tpm_aca_provision --tcp --ip 127.0.0.1:2321 --sim
