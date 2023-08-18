#!/bin/bash
# Capture location of the script to allow from invocation from any location
SCRIPT_DIR=$( dirname -- "$( readlink -f -- "$0"; )"; )
SPRING_PROP_FILE='../../../HIRS_AttestationCAPortal/src/main/resources/application.properties'
HIRS_CONF_DIR=/etc/hirs/aca
LOG_FILE_NAME="hirs_aca_install_"$(date +%Y-%m-%d).log 
LOG_DIR="/var/log/hirs/"
HIRS_PROP_DIR="/opt/hirs/default-properties"
COMP_JSON='../../../HIRS_AttestationCA/src/main/resources/component-class.json'
VENDOR_TABLE='../../../HIRS_AttestationCA/src/main/resources/vendor-table.json'
LOG_FILE="$LOG_DIR$LOG_FILE_NAME"
echo "LOG_FILE is $LOG_FILE"

if [ "$EUID" -ne 0 ]
      then echo "This script requires root.  Please run as root"
      exit 1
fi

mkdir -p $HIRS_CONF_DIR $LOG_DIR $HIRS_PROP_DIR

# Process parameters
# Argument handling https://stackoverflow.com/questions/192249/how-do-i-parse-command-line-arguments-in-bash
POSITIONAL_ARGS=()

while [[ $# -gt 0 ]]; do
  case $1 in
    --skip-db)
      ARG_SKIP_DB=YES
      shift # past argument
      ;;
    -*|--*)
      echo "aca_setup.sh: Unknown option $1"
      ;;
    *)
      POSITIONAL_ARGS+=("$1") # save positional arg
      shift # past argument
      ;;
  esac
done

set -- "${POSITIONAL_ARGS[@]}" # restore positional parameters
 
echo "HIRS ACA Setup initiated on $(date +%Y-%m-%d)" > "$LOG_FILE"

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
if [ -f $SPRING_PROP_FILE ]; then
   cp -n $SPRING_PROP_FILE $HIRS_CONF_DIR/.
   cp -n $COMP_JSON $HIRS_PROP_DIR/.
   cp -n $VENDOR_TABLE $HIRS_PROP_DIR/.
fi

if [ -z "${ARG_SKIP_DB}" ]; then
   sh ../pki/pki_setup.sh $LOG_FILE $PKI_PASS
   if [ $? -eq 0 ]; then 
        echo "ACA PKI  setup complete" | tee -a "$LOG_FILE"
      else
        echo "Error setting up ACA PKI" | tee -a "$LOG_FILE"
      exit 1
   fi
   echo "Warning: Database setup not run due to command line argument: $@" | tee -a "$LOG_FILE"
fi

sh ../db/db_create.sh $LOG_FILE
if [ $? -eq 0 ]; then
    echo "ACA database setup complete" | tee -a "$LOG_FILE"
  else
    echo "Error setting up ACA DB" | tee -a "$LOG_FILE"
    exit 1
fi

 echo "ACA setup complete" | tee -a "$LOG_FILE"

popd &>/dev/null