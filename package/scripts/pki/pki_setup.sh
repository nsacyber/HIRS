#!/bin/bash
############################################################################################
# Creates 2 Certificate Chains for the ACA:
# 1 RSA 3K SHA 384
# 2 ECC 512 SHA 384
# 
############################################################################################

#PROP_FILE=/etc/hirs/aca/application.properties
ACA_PROP=/etc/hirs/aca/aca.properties
SPRING_PROP_FILE="/etc/hirs/aca/application.properties"
LOG_FILE=$1
PKI_PASS=$2
UNATTENDED=$3
LOG_FILE_NAME="hirs_aca_install_"$(date +%Y-%m-%d).log
LOG_DIR="/var/log/hirs/"
HIRS_CONF_DIR=/etc/hirs/aca
# Capture location of the script to allow from invocation from any location 
SCRIPT_DIR=$( dirname -- "$( readlink -f -- "$0"; )"; )

mkdir -p $HIRS_CONF_DIR $LOG_DIR
echo "SCRIPT_DIR is $SCRIPT_DIR" | tee -a "$LOG_FILE"

if [ -z "$1" ]; then
   LOG_FILE="$LOG_DIR$LOG_FILE_NAME"
   echo "using log file $LOG_FILE" | tee -a "$LOG_FILE"
fi

if [ -z "$2" ]; then
   PKI_PASS=$(head -c 64 /dev/urandom | md5sum | tr -dc 'a-zA-Z0-9')
   echo "Using randomly generated password for the PKI key password" | tee -a "$LOG_FILE"
   echo "Using pki password=$PKI_PASS"
fi

# Check for sudo or root user 
if [ "$EUID" -ne 0 ]; then 
   echo "This script requires root.  Please run as root" | tee -a "$LOG_FILE"
        exit 1
fi

# Create Cert Chains
if [ ! -d "/etc/hirs/certificates" ]; then
  
   if [ -d "/opt/hirs/scripts/pki" ]; then
         PKI_SETUP_DIR="/opt/hirs/scripts/pki"
      else
         PKI_SETUP_DIR="$SCRIPT_DIR"
   fi
      echo "PKI_SETUP_DIR is $PKI_SETUP_DIR" | tee -a "$LOG_FILE"

  mkdir -p /etc/hirs/certificates/ | tee -a "$LOG_FILE"

  pushd  /etc/hirs/certificates/ &> /dev/null
  cp $PKI_SETUP_DIR/ca.conf .
    $PKI_SETUP_DIR/pki_chain_gen.sh "HIRS" "rsa" "3072" "sha384" "$PKI_PASS" "$LOG_FILE"
    $PKI_SETUP_DIR/pki_chain_gen.sh "HIRS" "ecc" "512" "sha384" "$PKI_PASS" "$LOG_FILE"
  popd &> /dev/null

  echo "hirs_pki_password="$PKI_PASS >>  $ACA_PROP
  echo "server.ssl.key-store-password="$PKI_PASS >> $SPRING_PROP_FILE
  echo "server.ssl.trust-store-password="$PKI_PASS >> $SPRING_PROP_FILE
else 
  echo "/etc/hirs/certificates exists, skipping" | tee -a "$LOG_FILE"
fi
