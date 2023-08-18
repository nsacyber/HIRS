#!/bin/bash
#####################################################################################
#
# Function to check mysql and start if not running.
#    Also a function for checking if running in a container
#
#####################################################################################

check_for_container () {
  # Check if we're in a Docker container
  if [[ $(cat /proc/1/sched | head -n 1) == *"bash"* ]]; then  
  #if [ -f /.dockerenv ]; then
    DOCKER_CONTAINER=true
    echo "ACA is running in a container..." | tee -a "$LOG_FILE"
  else
    DOCKER_CONTAINER=false
    echo "ACA is not running in a container..." | tee -a "$LOG_FILE"
  fi
  if [ -d /opt/hirs/scripts/db ]; then
    MYSQL_DIR="/opt/hirs/scripts/db"
  else
   MYSQL_DIR="$SCRIPT_DIR/../db"
  fi
  echo "Mysql script directory is $MYSQL_DIR"
}

start_mysqlsd () {
   # Check if mysql is already running, if not initialize
   if [[ $(pgrep -c -u mysql mysqld) -eq 0 ]]; then
   # Check if running in a container
      if [ $DOCKER_CONTAINER  = true ]; then
      # if in Docker container, avoid services that invoke the D-Bus
      echo "ACA is running in a container..."
         # Check if mariadb is setup
         if [ ! -d "/var/lib/mysql/mysql/" ]; then
           echo "Installing mariadb"
           /usr/bin/mysql_install_db > "$LOG_FILE"
           chown -R mysql:mysql /var/lib/mysql/
         fi
       echo "Starting mysql...."
       chown -R mysql:mysql /var/log/mariadb
       /usr/bin/mysqld_safe &
     else
       SQL_SERVICE="mariadb"
       systemctl $SQL_SERVICE enable
       systemctl $SQL_SERVICE start
     fi
   fi  # mysql not running 

  # Wait for mysql to start before continuing.
  echo "Checking mysqld status..."| tee -a "$LOG_FILE"
  while ! mysqladmin ping -h "$localhost" --silent; do
  sleep 1;
  done

  echo "mysqld is running."| tee -a "$LOG_FILE"
}