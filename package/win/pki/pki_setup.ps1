#!/bin/bash
############################################################################################
# Creates 2 Certificate Chains for the ACA:
# 1 RSA 3K SHA 384
# 2 ECC 512 SHA 384
# 
############################################################################################

param (
    [string]$LOG_FILE = $null,
	[string]$PKI_PASS = $null,
	[switch]$UNATTENDED = $false
)

$APP_HOME=(Split-Path -parent $PSCommandPath)
$ACA_COMMON_SCRIPT=(Join-Path "$APP_HOME" .. aca aca_common.ps1)

# Load other scripts
. $ACA_COMMON_SCRIPT

# Read aca.properties
read_aca_properties $global:HIRS_DATA_ACA_PROPERTIES_FILE

# Read spring application.properties
read_spring_properties $global:HIRS_DATA_SPRING_PROP_FILE

# Parameter check
if (-not (Test-Path -Path $LOG_FILE)) {
	New-Item -ItemType File -Path $LOG_FILE
	$global:LOG_FILE=$LOG_FILE
} else {
	set_up_log
}

if (!$PKI_PASS) {
	if ($Env:HIRS_PKI_PWD) {
		$PKI_PASS=$Env:HIRS_PKI_PWD
	} else {
		$PKI_PASS=(create_random)
		Write-Output "Using randomly generated password for the PKI key password" | WriteAndLog
	}
}

New-Item -ItemType Directory -Path $global:HIRS_CONF_DIR -Force | Out-Null
Write-Output "APP_HOME is $APP_HOME" | WriteAndLog

# Check for sudo or root user 
if(!(New-Object Security.Principal.WindowsPrincipal(
		[Security.Principal.WindowsIdentity]::GetCurrent())
	).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
	Write-Output "This script requires root.  Please run as root" | WriteAndLog
	exit 1
}

# Create Cert Chains
if (![System.IO.Directory]::Exists($global:HIRS_DATA_CERTIFICATES_DIR)) {
    if ([System.IO.Directory]::Exists($global:HIRS_REL_WIN_PKI_HOME)) {
		$PKI_SETUP_DIR=$global:HIRS_REL_WIN_PKI_HOME
	} else {
        $PKI_SETUP_DIR=$APP_HOME
	}
	Write-Output "PKI_SETUP_DIR is $PKI_SETUP_DIR" | WriteAndLog

    New-Item -ItemType Directory -Path $global:HIRS_DATA_CERTIFICATES_DIR -Force | Out-Null

    Copy-Item $PKI_SETUP_DIR\ca.conf $global:HIRS_DATA_CERTIFICATES_DIR
	pwsh -ExecutionPolicy Bypass $PKI_SETUP_DIR/pki_chain_gen.ps1 "HIRS" "rsa" "3072" "sha384" "$PKI_PASS" "$global:LOG_FILE"
    pwsh -ExecutionPolicy Bypass $PKI_SETUP_DIR/pki_chain_gen.ps1 "HIRS" "ecc" "512" "sha384" "$PKI_PASS" "$global:LOG_FILE"

    # Save the password to the ACA properties file.
	add_new_aca_property -file:"$global:HIRS_DATA_ACA_PROPERTIES_FILE" -newKeyAndValue:"hirs_pki_password=$PKI_PASS"
    
    # Save connector information to the application properties file.
    add_new_spring_property -file:"$global:HIRS_DATA_SPRING_PROP_FILE" -newKeyAndValue:"server.ssl.key-store-password=$PKI_PASS"
    add_new_spring_property -file:"$global:HIRS_DATA_SPRING_PROP_FILE" -newKeyAndValue:"server.ssl.trust-store-password=$PKI_PASS"
} else {
    Write-Output "$global:HIRS_DATA_CERTIFICATES_DIR exists, skipping" | WriteAndLog
}
