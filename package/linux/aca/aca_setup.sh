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
VENDOR_TABLE='../../../HIRS_AttestationCA/src/main/resources/vendor-table.json'

help () {
  echo "  Setup script for the HIRS ACA"
  echo "  Syntax: sh aca_setup.sh [-u|h|sb|sp|--skip-db|--skip-pki]"
  echo "  options:"
  echo "     -u  | --unattended   Run unattended"
  echo "     -h  | --help   Print this Help."
  echo "     -sp | --skip-pki run the setup without pki setup."
  echo "     -sd | --skip-db run the setup without database setup."
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

echo "Input is $1"
if [[ $1 -eq 1 ]] ; then
   echo "Install detected $1"
   else
   echo "Upgrade detected $1"
fi
 
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
   ../db/db_create.sh $LOG_FILE $PKI_PASS $ARG_UNATTEND
   if [ $? -eq 0 ]; then
      echo "ACA database setup complete" | tee -a "$LOG_FILE"
    else
      echo "Error setting up ACA DB" | tee -a "$LOG_FILE"
    exit 1
   fi
   else
      echo "ACA Database setup not run due to command line argument: $ORIGINAL_ARGS" | tee -a "$LOG_FILE"
fi

echo "ACA setup complete" | tee -a "$LOG_FILE"

popd &>/dev/null
