#!/bin/bash
#####################################################################################
#
# Script to create ACA setup files and configure the hirs_db database.
#
#
#####################################################################################
# Capture location of the script to allow from invocation from any location
SCRIPT_DIR=$( dirname -- "$( readlink -f -- "$0"; )"; )
HIRS_CONF_DIR=/etc/hirs/aca
LOG_FILE_NAME="hirs_aca_install_"$(date +%Y-%m-%d).log 
LOG_DIR="/var/log/hirs/"
LOG_FILE="$LOG_DIR$LOG_FILE_NAME"
HIRS_JSON_DIR="/etc/hirs/aca/default-properties"
ACA_OPT_DIR="/opt/hirs/aca/"
ACA_VERSION_FILE="/opt/hirs/aca/VERSION"
SPRING_PROP_FILE="/etc/hirs/aca/application.properties"
PROP_FILE='../../../HIRS_AttestationCAPortal/src/main/resources/application.properties'
COMP_JSON='../../../HIRS_AttestationCA/src/main/resources/component-class.json'
VENDOR_TABLE='../../../HIRS_Utils/src/main/resources/vendor-table.json'

help () {
  echo "  Setup script for the HIRS ACA"
  echo "  Syntax: sh aca_setup.sh [-u|h|sb|sp|--skip-db|--skip-pki]"
  echo "  options:"
  echo "     -u  | --unattended   Run unattended"
  echo "     -h  | --help   Print this Help."
  echo "     -sp | --skip-pki run the setup without pki setup."
  echo "     -sd | --skip-db run the setup without database setup."
  echo "     -aa | --aca-alg specify the ACA's default algorithm (rsa, ecc, or mldsa) for Attestation Certificates"
  echo "     -ta | --tls-alg specify the ACA's default algorithm (rsa, ecc, or mldsa) for TLS on the ACA portal"
  echo "     -da | --db-alg specify the ACA's default algorithm (rsa, ecc, or mldsa) for use with maraidb"
  echo
}

# Process parameters Argument handling 
while [[ $# -gt 0 ]]; do
  case $1 in
    -sd|--skip-db)
      ARG_SKIP_DB=YES
      shift # past argument
      ;;
    -sp|--skip-pki)
      ARG_SKIP_PKI=YES
      shift # past argument
      ;;
    -u|--unattended)
      ARG_UNATTEND=YES
      shift # past argument
      ;;
   -aa|--aca-alg)
      ARG_ACA_ALG=YES
      shift # past argument 
      ACA_ALG=$1
      shift # past parameter
      ;;
   -ta|--tls-alg)
      ARG_TLS_ALG=YES
      shift # past argument
      TLS_ALG=$1
      ;;
   -da|--db-alg)
      ARG_DB_ALG=YES
      shift # past argument
      DB_ALG=$1
      shift # past parameter
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
     # shift # past argumfrom 'build/VERSION'ent
     break
      ;;
  esac
done
# Set default algorithms to rsa
if [ -z $ARG_ACA_ALG ]; then 
     ACA_ALG="rsa"
     echo "Using default algorithm ($ACA_ALG) for Attestation Certs"
fi
if [ -z $ARG_TLS_ALG ]; then
     TLS_ALG="rsa"
     echo "Using default algorithm ($TLS_ALG) for the ACA portal"
fi
if [ -z $ARG_DB_ALG ]; then 
     DB_ALG="rsa"
     echo "Using default algorithm ($DB_ALG) for the Database"
fi

# Check for valid algorithms
if [ ! $ACA_ALG == "rsa" ] && [ ! $ACA_ALG == "ecc" ] ; then
   echo  "Invalid ACA algorithm $ACA_ALG specified. Valid options are rsa or ecc."
   exit 1;
fi
if [ ! $TLS_ALG == "rsa" ] && [ ! $TLS_ALG == "ecc" ] ; then
   echo  "Invalid TLS algorithm $TLS_ALG specified. Valid options are rsa or ecc."
   exit 1;
fi
if [ ! $DB_ALG == "rsa" ] && [ ! $DB_ALG == "ecc" ] ; then
   echo  "Invalid DB algorithm $DB_ALG specified. Valid options are rsa or ecc."
   exit 1;
fi

#echo  "ARG_ACA_ALG is $ARG_ACA_ALG"
#echo  "ACA_ALG is $ACA_ALG"

#echo  "ARG_TLS_ALG is $ARG_TLS_ALG"
#echo "TLS_ALG is $TLS_ALG"

#echo "ARG_DB_ALG is  $ARG_DB_ALG"
#echo "DB_ALG is $DB_ALG"

#echo "Input is $1"
#if [[ $1 -eq 1 ]] ; then
#   echo "Install detected $1"
#   else
#   echo "Upgrade detected $1"
#fi
 
# Check for existing installation folders and exist if found
if [ -z $ARG_UNATTEND ]; then
  if [ -d "/etc/hirs" ]; then
    echo "/etc/hirs exists, aborting install."
    exit 1  
  fi
  if [ -d "/opt/hirs" ]; then
    echo "/opt/hirs exists, aborting install."
    exit 1  
  fi
fi

mkdir -p $HIRS_CONF_DIR $LOG_DIR $HIRS_JSON_DIR $ACA_OPT_DIR
touch "$LOG_FILE"

pushd $SCRIPT_DIR &>/dev/null
# Check if build environment is being used and set up property files
if [ -f  $PROP_FILE ]; then
   cp -n $PROP_FILE $HIRS_CONF_DIR/
   cp -n $COMP_JSON $HIRS_JSON_DIR/
   cp -n $VENDOR_TABLE $HIRS_JSON_DIR/
fi

echo "ACA setup log file is $LOG_FILE"

if [ "$EUID" -ne 0 ]
      then echo "This script requires root.  Please run as root"
      exit 1
fi

echo "HIRS ACA Setup initiated on $(date +%Y-%m-%d)" >> "$LOG_FILE"

# Create a version file for bootRun to use
if command -v git &> /dev/null; then
   git rev-parse --is-inside-work-tree  &> /dev/null;
   if [ $? -eq 0 ]; then
     jarVersion=$(cat '../../../VERSION').$(date +%s).$(git rev-parse --short  HEAD)
   echo $jarVersion > $ACA_VERSION_FILE
   fi
fi

# Set HIRS PKI  password
if [ -z $HIRS_PKI_PWD ]; then
   # Create a 32 character random password
   PKI_PASS=$(head -c 64 /dev/urandom | md5sum | tr -dc 'a-zA-Z0-9')
   echo "Using randomly generated password for the PKI key password" | tee -a "$LOG_FILE"
  else
   PKI_PASS=$HIRS_PKI_PWD
   echo "Using system supplied password for the PKI key password" | tee -a "$LOG_FILE"
fi

if [ -z "${ARG_SKIP_PKI}" ]; then
   ../pki/pki_setup.sh $LOG_FILE $PKI_PASS $ARG_UNATTEND
   if [ $? -eq 0 ]; then 
        echo "ACA PKI  setup complete" | tee -a "$LOG_FILE"
      else
        echo "Error setting up ACA PKI" | tee -a "$LOG_FILE"
      exit 1
   fi
   else
      echo "ACA PKI setup not run due to command line argument: $ORIGINAL_ARGS" | tee -a "$LOG_FILE"
fi

if [ -z "${ARG_SKIP_DB}" ]; then
   ../db/db_create.sh $LOG_FILE $PKI_PASS $DB_ALG $ARG_UNATTEND
   if [ $? -eq 0 ]; then
      echo "ACA database setup complete" | tee -a "$LOG_FILE"
    else
      echo "Error setting up ACA DB" | tee -a "$LOG_FILE"
    exit 1
   fi
   else
      echo "ACA Database setup not run due to command line argument: $ORIGINAL_ARGS" | tee -a "$LOG_FILE"
fi

# Update properties file based upon algorithm choices
echo "Setting algorithm setting for TLS and ACA..."
# remove default config file lines for tomcat ssl aliases
  sed -i '/server.ssl.trust-alias/d' $SPRING_PROP_FILE
  sed -i '/server.ssl.key-alias/d' $SPRING_PROP_FILE
if [ "$TLS_ALG" == "rsa" ]; then
  echo "server.ssl.trust-alias=hirs_aca_tls_rsa_3k_sha384" >> $SPRING_PROP_FILE
  echo "server.ssl.key-alias=hirs_aca_tls_rsa_3k_sha384_key" >> $SPRING_PROP_FILE
elif [ "$TLS_ALG" == "ecc" ]; then
  echo "server.ssl.trust-alias=hirs_aca_tls_ecc_512_sha384" >> $SPRING_PROP_FILE
  echo "server.ssl.key-alias=hirs_aca_tls_ecc_512_sha384_key" >> $SPRING_PROP_FILE
fi

 # remove default config file lines for aca aliases
  sed -i '/aca.certificates.leaf-three-key-alias/d' $SPRING_PROP_FILE
  sed -i '/aca.certificates.intermediate-key-alias/d' $SPRING_PROP_FILE
  sed -i '/aca.certificates.root-key-alias/d' $SPRING_PROP_FILE
  sed -i '/aca.current.public.key.algorithm/d' $SPRING_PROP_FILE
if [ "$ACA_ALG" == "rsa" ]; then
  # Add new lines for aca aliases
  echo "aca.certificates.leaf-three-key-alias=HIRS_leaf_ca3_rsa_3k_sha384_key" >> $SPRING_PROP_FILE
  echo "aca.certificates.intermediate-key-alias=HIRS_intermediate_ca_rsa_3k_sha384_key" >> $SPRING_PROP_FILE
  echo "aca.certificates.root-key-alias=HIRS_root_ca_rsa_3k_sha384_key" >> $SPRING_PROP_FILE
  echo "aca.current.public.key.algorithm=rsa" >> $SPRING_PROP_FILE
elif [ "$ACA_ALG" == "ecc" ]; then
  echo "aca.certificates.leaf-three-key-alias=HIRS_leaf_ca3_ecc_512_sha384_key" >> $SPRING_PROP_FILE
  echo "aca.certificates.intermediate-key-alias=HIRS_intermediate_ca_ecc_512_sha384_key" >> $SPRING_PROP_FILE
  echo "aca.certificates.root-key-alias=HIRS_root_ca_ecc_512_sha384_key" >> $SPRING_PROP_FILE
  echo "aca.current.public.key.algorithm=ecc" >> $SPRING_PROP_FILE
fi

echo "ACA setup complete" | tee -a "$LOG_FILE"

popd &>/dev/null
