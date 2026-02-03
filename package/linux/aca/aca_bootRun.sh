#!/bin/bash
#####################################################################################
#
# Script to run ACA using the gradle spring pluing bootRun command with parameters
#       parameters include setting up the DB with TLS and embedded Tomcat with TLS.
#
#####################################################################################

SPRING_PROP_FILE="/etc/hirs/aca/application.properties"
ALG=RSA
RSA_PATH=rsa_3k_sha384_certs
ECC_PATH=ecc_512_sha384_certs
SCRIPT_DIR=$( dirname -- "$( readlink -f -- "$0"; )"; )
LOG_FILE=/dev/null
GRADLE_WRAPPER="./gradlew"
DEPLOYED_WAR=false
DEBUG_OPTIONS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:9123"

# Check for sudo or root user 
if [ "$EUID" -ne 0 ]
     then echo "This script requires root.  Please run as root" 
     exit 1
fi

help () {
  echo "  Setup script for the HIRS ACA"
  echo "  Syntax: sh aca_setup.sh [-u|h|sb|sp|--skip-db|--skip-pki]"
  echo "  options:"
  echo "     -p  | --path   Path to the HIRS_AttestationCAPortal.war file"
  echo "     -w  | --war    Use deployed war file"
  echo "     -d  | --debug  Launch the JVM with a debug port open"
  echo "     -h  | --help   Print this help"
  echo
}

# Process parameters Argument handling 
POSITIONAL_ARGS=()
ORIGINAL_ARGS=("$@")
while [[ $# -gt 0 ]]; do
  case $1 in
    -p|--path)
      USE_WAR=YES
      shift # past argument
      WAR_PATH=$@
      DEPLOYED_WAR=true
      shift # past parameter
      ;;
    -w|--war)
      USE_WAR=YES
      shift # past argument
      WAR_PATH="/opt/hirs/aca/HIRS_AttestationCAPortal.war"
      DEPLOYED_WAR=true
      ;;
    -d|--debug)
      DEBUG_ACA=YES
      shift
      ;;
    -h|--help)
      help     
      exit 0
      shift # past argument
      ;; 
    -*|--*)
      echo "aca_setup.sh: Unknown option $1"
      help
      exit 1
      ;;
    *)
     POSITIONAL_ARGS+=("$1") # save positional arg
     # shift # past argument
     break
      ;;
  esac
done

if [ -z "${WAR_PATH}" ]; then
  WAR_PATH="HIRS_AttestationCAPortal/build/libs/HIRS_AttestationCAPortal.war"
fi 

set -- "${POSITIONAL_ARGS[@]}" # restore positional parameters

source $SCRIPT_DIR/../db/mysql_util.sh

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

check_systemd
start_mysqlsd

if [ ! -d "$CERT_PATH" ]; then
     echo "$CERT_PATH directory does not exist. Please run aca_setup.sh and try again."
     exit 1;
fi

if [ $DEPLOYED_WAR = false ]; then
  if [ ! -f "$GRADLE_WRAPPER" ]; then
    echo "This script needs to be run from the HIRS top level project directory. Exiting."
    exit 1;
  fi
fi

echo "Starting HIRS ACA on https://localhost:8443/HIRS_AttestationCAPortal/portal/index"

source /etc/hirs/aca/aca.properties;

# Run the embedded tomcat server with Web TLS enabled and database client TLS enabled by overrding critical parameters
# Note "&" is a sub parameter continuation, space represents a new parameter. Spaces and quotes matter.
# hibernate.connection.url is used fo    r the DB connector which established DB TLS connectivity
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

# uncomment to show spring boot and hibernate properties used as gradle arguments
#echo "--args=\"$CONNECTOR_PARAMS $WEB_TLS_PARAMS\""

if [ -z "$USE_WAR" ]; then
  echo "Booting the ACA from local build..."
  if [ "$DEBUG_ACA" == YES ]; then
    echo "... in debug"
    ./gradlew bootRun --args="--spring.config.location=$SPRING_PROP_FILE" -Pdebug="$DEBUG_OPTIONS" --stacktrace
  else
    ./gradlew bootRun --args="--spring.config.location=$SPRING_PROP_FILE" --stacktrace
  fi
else
  echo "Booting the ACA from a war file..."
  if [ "$DEBUG_ACA" == YES ]; then
    echo "... in debug"
    java $DEBUG_OPTIONS  -jar  $WAR_PATH --spring.config.location=$SPRING_PROP_FILE &
  else
    java -jar  $WAR_PATH --spring.config.location=$SPRING_PROP_FILE &
  fi
  exit 0
fi
