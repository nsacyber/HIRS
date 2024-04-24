#!/bin/bash
#####################################################################################
#
# Script to remove ACA setup files and database items.
# 
#
#####################################################################################

SCRIPT_DIR=$( dirname -- "$( readlink -f -- "$0"; )"; )
OPTION_IN=$1; # per Fedora packing guidelines: $1 = 1 for an upgrade, 0 for a remove
if [ -z $1 ]; then OPTION_IN="2"; fi  # Set if called by command line
case $OPTION_IN in
   "0")
      echo "Package removal requested"
      OPTION="ACA_PKG_REMOVE"
    ;;
   "1")
      echo "Package upgrade requested"
      OPTION="ACA_UPGRADE"
    ;;
   "2") 
      echo "ACA Setup removal requested"
      OPTION="ACA_SET_REMOVE"
       ;;
   *) 
     echo "$1 is an unknown parameter for aca_remove_setup"
     exit 1
     break
    ;;
esac

LOG_FILE=/dev/null
LOG_DIR="/var/log/hirs/"

# Check for Admin privileges
if [ "$EUID" -ne 0 ]; then
      echo "This script requires root.  ACA setup not removed. Please run as root."
      exit 1
fi

if [ ! -d "/etc/hirs" ]; then
  echo "/etc/hirs does not exist, aborting removal."
  exit 1  
fi
if [ ! -d "/opt/hirs" ]; then
  echo "/opt/hirs does not exist, aborting removal."
  exit 1  
fi


source $SCRIPT_DIR/../db/mysql_util.sh

# Make sure mysql root password is available before continuing...
check_mariadb_install

check_mysql_root

# remove the hrs-db and hirs_db user
if [ $OPTION = "ACA_SET_REMOVE" ] || [ $OPTION = "ACA_PKG_REMOVE" ]; then 
  pushd $SCRIPT_DIR/../db/  &>/dev/null
  ./db_drop.sh $DB_ADMIN_PWD
  popd  &>/dev/null
fi

# remove pki files and config files if not installed by rpm
echo "Removing certificates and config files..."

# Remove /opt/hirs only if not configured by a package based install:
if [ $OPTION = "ACA_SET_REMOVE" ]; then
  if [ -d "/etc/hirs" ]; then
     rm -rf /etc/hirs >/dev/null 2>&1
  fi
  if [ -d "/opt/hirs" ]; then
     rm -rf /opt/hirs >/dev/null 2>&1
  fi
fi

if [ -d $LOG_DIR ]; then 
   rm -rf $LOG_DIR;
fi

# Remove current ACA process
echo "Shutting down the aca..."
ps axf | grep HIRS_AttestationCAPortal.war | grep -v grep | awk '{print "kill " $1}' | sh  >/dev/null 2>&1
echo "ACA setup removal complete."
