#!/bin/bash
# Capture location of the script to allow from invocation from any location
SCRIPT_DIR=$( dirname -- "$( readlink -f -- "$0"; )"; )
#SPRING_PROP_FILE='../../../HIRS_AttestationCAPortal/src/main/resources/application.properties'
HIRS_CONF_DIR=/etc/hirs/aca
LOG_FILE_NAME="hirs_aca_install_"$(date +%Y-%m-%d).log 
LOG_DIR="/var/log/hirs/"
LOG_FILE="$LOG_DIR$LOG_FILE_NAME"
HIRS_PROP_DIR="/opt/hirs/default-properties"
#COMP_JSON='../../../HIRS_AttestationCA/src/main/resources/component-class.json'
#VENDOR_TABLE='../../../HIRS_AttestationCA/src/main/resources/vendor-table.json'

help () {
  echo "  Setup script for the HIRS ACA"
  echo "  Syntax: sh aca_setup.sh [-u|h|sb|sp|--skip-db|--skip-pki]"
  echo "  options:"
  echo "     -u  | --unattended   Run unattended"
  echo "     -h  | --help   Print this Help."
  echo "     -sp | --skip-pki run the setup without pki setup."
  echo "     -sb | --skip-db run the setup without databse setup."
  echo
}

# Process parameters Argument handling 
POSITIONAL_ARGS=()
ORIGINAL_ARGS=("$@")
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
     # shift # past argument
     break
      ;;
  esac
done

set -- "${POSITIONAL_ARGS[@]}" # restore positional parameters

mkdir -p $HIRS_CONF_DIR $LOG_DIR $HIRS_PROP_DIR

echo "ACA setup log file is $LOG_FILE"

#if [ -z $HIRS_MYSQL_ROOT_PWD ]; then 
#    echo "HIRS_MYSQL_ROOT_PWD is not set, using locally generated mysql root password"
#  else 
#    echo "HIRS_MYSQL_ROOT_PWD is set, using previously set mysql root password"
#fi

if [ "$EUID" -ne 0 ]
      then echo "This script requires root.  Please run as root"
      exit 1
fi

touch "$LOG_FILE"
echo "HIRS ACA Setup initiated on $(date +%Y-%m-%d)" >> "$LOG_FILE"

pushd $SCRIPT_DIR &>/dev/null

# Set HIRS PKI  password
if [ -z $HIRS_PKI_PWD ]; then
   # Create a 32 character random password
   PKI_PASS=$(head -c 64 /dev/urandom | md5sum | tr -dc 'a-zA-Z0-9')
   echo "Using randomly generated password for the PKI key password" | tee -a "$LOG_FILE"
  else
   PKI_PASS=$HIRS_PKI_PWD
   echo "Using system supplied password for the PKI key password" | tee -a "$LOG_FILE"
fi

# Copy HIRS configuration and data files if not a package install
#if [ -f $SPRING_PROP_FILE ]; then
#   cp -n $SPRING_PROP_FILE $HIRS_CONF_DIR/.
#   cp -n $COMP_JSON $HIRS_PROP_DIR/.
#   cp -n $VENDOR_TABLE $HIRS_PROP_DIR/.
#fi

if [ -z "${ARG_SKIP_PKI}" ]; then
   sh ../pki/pki_setup.sh $LOG_FILE $PKI_PASS $ARG_UNATTEND
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
   sh ../db/db_create.sh $LOG_FILE $ARG_UNATTEND
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