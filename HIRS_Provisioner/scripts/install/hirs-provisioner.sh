#!/bin/bash

# main driving script for the HIRS Provisioner RPM/DEB. Provides user options
# for creating a default hirs-site.config, used by the provisioner and the HIRS Client RPM,
# as well as initiating HIRS provisioning of the TPM and loading of credentials from the
# HIRS Attestation CA.

CERTS_DIR="/etc/hirs/provisioner/certs/"
HIRS_SITE_CONFIG="/etc/hirs/hirs-site.config"
HIRS_PROVISIONER_CONFIG="/etc/hirs/provisioner/hirs-provisioner-config.sh"
HIRS_PROVISIONER_SCRIPT="/usr/share/hirs/provisioner/bin/HIRS_Provisioner"
HIRS_PROVISIONER_2_0_SCRIPT="/usr/local/bin/hirs-provisioner-tpm2"
HIRS_PROVISIONER_PROPERTIES="/etc/hirs/provisioner/provisioner.properties"

if [ "$EUID" != "0" ]; then
   echo "This script must be run as root"
   exit 1
fi

function ShowHelp {
    echo "hirs provisioner - host integrity at runtime & startup"
    echo ""
    echo "hirs-provisioner [command]"
    echo ""
    echo "commands:"
    echo "-h, --help, help           show this help"
    echo "-c, --config, config       verify or generate site configuration"
    echo "-p, --provision, provision provision TPM/prepare for use with HIRS"
    exit 0
}

if [ $# -eq 0 ]; then # if no arguments
    ShowHelp
fi


function CheckHIRSSiteConfig {

    # Check for site config existence
    if [ ! -f $HIRS_SITE_CONFIG ]; then
        echo "--> ERROR: $HIRS_SITE_CONFIG not found - run \"hirs-provisioner -c\" to generate the file"
        exit 1
    fi

    # Read site config
    source $HIRS_SITE_CONFIG

    # Verify variable existence
    if [[ -z "$CLIENT_HOSTNAME" ]]; then
        echo "--> ERROR: CLIENT_HOSTNAME is not set in $HIRS_SITE_CONFIG"
        exit 1
    fi

    if [[ -z "$ATTESTATION_CA_FQDN" ]]; then
        echo "--> ERROR: ATTESTATION_CA_FQDN not set in $HIRS_SITE_CONFIG"
        exit 1
    fi

    if [[ -z "$ATTESTATION_CA_PORT" ]]; then
        echo "--> ERROR: ATTESTATION_CA_PORT not set in $HIRS_SITE_CONFIG"
        exit 1
    fi

    if [[ -z "$BROKER_FQDN" ]]; then
        echo "--> ERROR: BROKER_FQDN not set in $HIRS_SITE_CONFIG"
        exit 1
    fi

    if [[ -z "$BROKER_PORT" ]]; then
        echo "--> ERROR: BROKER_PORT not set in $HIRS_SITE_CONFIG"
        exit 1
    fi

    if [[ -z "$PORTAL_FQDN" ]]; then
        echo "--> ERROR: PORTAL_FQDN not set in $HIRS_SITE_CONFIG"
        exit 1
    fi

    if [[ -z "$PORTAL_PORT" ]]; then
        echo "--> ERROR: PORTAL_PORT not set in $HIRS_SITE_CONFIG"
        exit 1
    fi
}

function CheckProvisionPrereqsAndDoProvisioning {

    if [ ! -f $HIRS_SITE_CONFIG ]; then
        echo "$HIRS_SITE_CONFIG not found. Run \"hirs-provisioner -c\" to generate."
        exit 0
    fi

    # the hirs provisioner script should be verifying
    CheckHIRSSiteConfig

    echo "--> Configuring provisioner"
    eval $HIRS_PROVISIONER_CONFIG || { echo "----> Failed configuring provisioner"; exit 1; }

    if [ $TPM_ENABLED = "true" ]; then
        echo "--> Provisioning"
        Provision
    else
        echo "--> TPM not enabled - skipping provisioning"
    fi
}


function Provision {
    # Provisioner will only retain one {uuid}.cer credential; remove any existing *.cer files.
    echo "----> Removing old attestation credentials, if any"
    rm -f $CERTS_DIR/*.cer /etc/hirs/ak.cer
    echo "----> Provisioning TPM"

    if [ -f $HIRS_PROVISIONER_2_0_SCRIPT ]
    then
        $HIRS_PROVISIONER_2_0_SCRIPT provision || { echo "----> Failed to provision TPM 2.0"; exit 1; }
    else
        $HIRS_PROVISIONER_SCRIPT $CLIENT_HOSTNAME || { echo "----> Failed to provision TPM"; exit 1; }
    fi
}


function WriteDefaultHirsSiteConfigFile {
    if [ ! -f $HIRS_SITE_CONFIG ]; then
        # Create template site config if it does not exist
	    cat <<DEFAULT_SITE_CONFIG_FILE > $HIRS_SITE_CONFIG
#*******************************************
#* HIRS site configuration properties file
#*******************************************

# Client configuration
CLIENT_HOSTNAME=$(hostname -f)
TPM_ENABLED=
IMA_ENABLED=

# Site-specific configuration
ATTESTATION_CA_FQDN=
ATTESTATION_CA_PORT=8443
BROKER_FQDN=
BROKER_PORT=61616
PORTAL_FQDN=
PORTAL_PORT=8443

DEFAULT_SITE_CONFIG_FILE

        echo "$HIRS_SITE_CONFIG not found - a template has been created"
        echo "Set your site configuration manually in $HIRS_SITE_CONFIG, then run 'hirs-provisioner -p' to provision this system"
    fi
}


while test $# -gt 0; do # iterate over arguments
    case "$1" in
        -c|--config|config)
            shift
            WriteDefaultHirsSiteConfigFile
            ;;
        -p|--provision|provision)
            shift
            CheckProvisionPrereqsAndDoProvisioning
            ;;
        *)
            ShowHelp
            ;;
    esac
done
