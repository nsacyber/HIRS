#!/bin/bash
#####################################################################################
#
# Functions to check mysql and start if not running.
#    Also a function for checking if running in a container
#
#####################################################################################
SQL_SERVICE="mariadb"

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
}

# Check for mysql command line
check_mariadb_install () {
   type mysql >/dev/null 2>&1 && installed=true || installed=false
   if [ $installed = true ]; then
      echo "mysql has been installed"
    else
      echo "mysql has NOT been installed, aborting install"
      exit 1;
   fi 
}
# Starts mariadb during intial install 
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
       else #not a container
         systemctl enable $SQL_SERVICE 
         systemctl start $SQL_SERVICE 
       fi
     else # mysql process is running
     # check if mysql service is running 
     if [ ! $DOCKER_CONTAINER  = true ]; then
     DB_STATUS=$(systemctl status mysql |grep 'running' | wc -l )
       if [ $DB_STATUS -eq 0 ]; then 
         echo "mariadb not running , attempting to restart"
         systemctl start mariadb
       fi
     fi
   fi   

  # Wait for mysql to start before continuing.
  echo "Checking mysqld status..."| tee -a "$LOG_FILE"
  while ! mysqladmin ping -h "$localhost" --silent; do
  sleep 1;
  done

  echo "mysqld is running."| tee -a "$LOG_FILE"
}

# Basic check for marai db status, attempts restart if not running
check_mysql () {
 echo "Checking mysqld status..."
  if [ $DOCKER_CONTAINER  = true ]; then
       if [[ $(pgrep -c -u mysql mysqld) -eq 0 ]]; then
          echo "mariadb not running , attempting to restart"
          /usr/bin/mysqld_safe &
       fi
  else  # not in a contianer
    DB_STATUS=$(systemctl status mysql |grep 'running' | wc -l )
    if [ $DB_STATUS -eq 0 ]; then 
       echo "mariadb not running , attempting to restart"
       systemctl start mariadb
    fi
  fi

 # Wait for mysql to start before continuing.

  while ! mysqladmin ping -h "$localhost" --silent; do
     sleep 1;
  done

  echo "   Mariadb is running."

}

# restart maraidb
mysqld_reboot () {
  # reboot mysql server
  mysql -u root --password=$DB_ADMIN_PWD -e "SHUTDOWN"
  sleep 2
  check_for_container
  start_mysqlsd
}