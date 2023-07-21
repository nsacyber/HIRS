#!/bin/bash
############################################################################################
# Creates 2 Certificate Chains for the ACA:
# 1 RSA 3K SHA 384
# 2 ECC 512 SHA 384
#
############################################################################################

PROP_FILE=/etc/hirs/aca/application.properties
LOG_FILE=$1

# Capture location of the script to allow from invocation from any location 
SCRIPT_DIR=$( dirname -- "$( readlink -f -- "$0"; )"; )
echo "SCRIPT_DIR is $SCRIPT_DIR" | tee -a "$LOG_FILE"

# Check for sudo or root user 
if [ "$EUID" -ne 0 ]
        then echo "The first time this script is run, this script requires root.  Please run as root" | tee -a "$LOG_FILE"
        exit 1
fi

# Set HIRS PKI  password
if [ -z $HIRS_PKI_PWD ]; then
   # Create a 32 character random password
   PKI_PASS=$(head -c 64 /dev/urandom | md5sum | tr -dc 'a-zA-Z0-9')
   echo "Using randomly generated password" | tee -a "$LOG_FILE"
  else
   PKI_PASS=$HIRS_PKI_PWD
   echo "Using system supplied password" | tee -a "$LOG_FILE"
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
  sh $PKI_SETUP_DIR/pki_chain_gen.sh "HIRS" "rsa" "3072" "sha384" "$PKI_PASS" "$LOG_FILE"
  sh $PKI_SETUP_DIR/pki_chain_gen.sh "HIRS" "ecc" "512" "sha384" "$PKI_PASS" "$LOG_FILE"
  popd &> /dev/null

  # Add tomcat TLS support to the application.properties file 
  echo "# Tomcat TLS support">> $PROP_FILE
  echo "server.port=8443">> $PROP_FILE
  echo "server.ssl.enabled=true">> $PROP_FILE
  echo "server.ssl.trust-store-type=JKS">> $PROP_FILE
  echo "server.ssl.trust-store=/etc/hirs/certificates/HIRS/TrustStore.jks">> $PROP_FILE
  echo "server.ssl.trust-alias=hirs_aca_tls_rsa_3k_sha384">> $PROP_FILE
  echo "server.ssl.key-store-type=JKS">> $PROP_FILE
  echo "server.ssl.key-store=/etc/hirs/certificates/HIRS/KeyStore.jks">> $PROP_FILE
  echo "server.ssl.key-alias=hirs_aca_tls_rsa_3k_sha384">> $PROP_FILE
  echo "server.ssl.key-store-password="$PKI_PASS >> $PROP_FILE
  echo "server.ssl.trust-store-password="$PKI_PASS >> $PROP_FILE
else 
  echo "/etc/hirs/certificates exists, skipping" | tee -a "$LOG_FILE"
fi
