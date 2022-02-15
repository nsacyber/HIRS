set -e

if ! [ $(id -u) = 0 ]; then
   echo "Please run this script as root."
   exit 1
fi

HIRS_SITE_CONFIG="/etc/hirs/hirs-site.config"

mkdir -p /var/log/hirs/provisioner
mkdir -p /etc/hirs/provisioner/certs
ln -s -f /usr/local/bin/hirs-provisioner-tpm2 /usr/sbin/hirs-provisioner-tpm2
ln -s -f /usr/local/bin/tpm_aca_provision /usr/sbin/tpm_aca_provision
ln -s -f /usr/local/bin/tpm_version /usr/sbin/tpm_version

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
ln -s -f /etc/hirs/provisioner/hirs-provisioner.sh /usr/sbin/hirs-provisioner

TCG_TEMP_FILE="/tmp/boot_properties"
TCG_BOOT_FILE="/etc/hirs/tcg_boot.properties"
TCG_DIRECTORY="/boot/tcg"
RIM_FILE_LOCATION="$TCG_DIRECTORY/manifest/rim/"
SWIDTAG_FILE_LOCATION="$TCG_DIRECTORY/manifest/swidtag/"
CREDENTIALS_LOCATION="$TCG_DIRECTORY/cert/platform/"
BINARY_BIOS_MEASUREMENTS="/sys/kernel/security/tpm0/binary_bios_measurements"

if [ ! -f "$TCG_BOOT_FILE" ]; then
  touch "$TCG_TEMP_FILE"
  echo "tcg.rim.dir=$RIM_FILE_LOCATION" > "$TCG_TEMP_FILE"
  echo "tcg.swidtag.dir=$SWIDTAG_FILE_LOCATION" >> "$TCG_TEMP_FILE"
  echo "tcg.cert.dir=$CREDENTIALS_LOCATION" >> "$TCG_TEMP_FILE"
  echo "tcg.event.file=$BINARY_BIOS_MEASUREMENTS" >> "$TCG_TEMP_FILE"
  install -m 644 $TCG_TEMP_FILE $TCG_BOOT_FILE
fi

