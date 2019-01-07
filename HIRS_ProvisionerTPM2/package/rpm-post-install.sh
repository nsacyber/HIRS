set -e

if ! [ $(id -u) = 0 ]; then
   echo "Please run this script as root."
   exit 1
fi

HIRS_SITE_CONFIG="/etc/hirs/hirs-site.config"

mkdir -p /var/log/hirs/provisioner
ln /usr/local/lib/libcurl.so /usr/lib64/libcurl.so
ln -s -f /usr/local/bin/hirs-provisioner-tpm2 /usr/sbin/hirs-provisioner-tpm2
ln -s -f /usr/local/bin/tpm_aca_provision /usr/sbin/tpm_aca_provision

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
    echo "Set your site configuration manually in $HIRS_SITE_CONFIG, then run 'hirs-provisioner-tpm2 provision' to provision this system"
fi
