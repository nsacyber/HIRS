#!/bin/bash
#####################################################################################
#
# Script to run ACA using the gradle spring pluing bootRun command with parameters
#       parameters include setting up the DB with TLS and embedded Tomcat with TLS.
#
#####################################################################################

USE_WAR=$1
CONFIG_FILE="/etc/hirs/aca/application.properties"
ALG=RSA
RSA_PATH=rsa_3k_sha384_certs
ECC_PATH=ecc_512_sha384_certs
SCRIPT_DIR=$( dirname -- "$( readlink -f -- "$0"; )"; )
LOG_FILE=/dev/null
GRADLE_WRAPPER="./gradlew"

# Check for sudo or root user 
if [ "$EUID" -ne 0 ]
     then echo "This script requires root.  Please run as root" 
     exit 1
fi

source $SCRIPT_DIR/../db/start_mysqld.sh

if [ $ALG = "RSA" ]; then 
   CERT_PATH="/etc/hirs/certificates/HIRS/$RSA_PATH"
   CERT_CHAIN="$CERT_PATH/HIRS_rsa_3k_sha384_Cert_Chain.pem"
   CLIENT_DB_P12=$CERT_PATH/HIRS_db_client_rsa_3k_sha384.p12
   ALIAS="hirs_aca_tls_rsa_3k_sha384"
 else
   CERT_PATH="/etc/hirs/certificates/HIRS/$ECC_PATH"
   CERT_CHAIN="$CERT_PATH/HIRS_ecc_512_sha384_Cert_Chain.pem"
   CLIENT_DB_P12=$CERT_PATH/HIRS_db_client_ecc_512_sha384.p12
   ALIAS="hirs_aca_tls_ecc_512_sha384"
fi

check_for_container
start_mysqlsd

if [ ! -d "$CERT_PATH" ]; then
     echo "$CERT_PATH directory does not exist. Please run aca_setup.sh and try again."
     exit 1;
fi

if [ ! -f "$GRADLE_WRAPPER" ]; then
    echo "This script needs to be run from the HIRS top level project directory. Exiting."
    exit 1;
fi

echo "Starting HIRS ACA on https://localhost:8443/HIRS_AttestationCAPortal/portal/index"

source /etc/hirs/aca/aca.properties;

# Run the embedded tomcat server with Web TLS enabled and database client TLS enabled by overrding critical parameters
# Note "&" is a sub parameter continuation, space represents a new parameter. Spaces and quotes matter.
# hibernate.connection.url is used for the DB connector which established DB TLS connectivity
# server.ssl arguments support the embeded tomcats use of TLS for the ACA Portal
CONNECTOR_PARAMS="--hibernate.connection.url=jdbc:mariadb://localhost:3306/hirs_db?autoReconnect=true&\
user=$hirs_db_username&\
password=$hirs_db_password&\
sslMode=VERIFY_CA&\
serverSslCert=$CERT_CHAIN&\
keyStoreType=PKCS12&\
keyStorePassword=$hirs_pki_password&\
keyStore="$CLIENT_DB_P12" "

WEB_TLS_PARAMS="--server.ssl.key-store-password=$hirs_pki_password \
--server.ssl.trust-store-password=$hirs_pki_password"

# uncomment to show spring boot and hibernate properties used as gradle argumanets
#echo "--args=\"$CONNECTOR_PARAMS $WEB_TLS_PARAMS\""

if [ "$USE_WAR" == "war" ]; then
    echo "Booting the ACA from a $USE_WAR file..."
    java -jar HIRS_AttestationCAPortal/build/libs/HIRS_AttestationCAPortal.war $CONNECTOR_PARAMS$WEB_TLS_PARAMS
else 
   echo "Booting the ACA from local build..."
 ./gradlew bootRun --args="$CONNECTOR_PARAMS$WEB_TLS_PARAMS"
fi
