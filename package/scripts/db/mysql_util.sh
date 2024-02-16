#!/bin/bash
#####################################################################################
#
# Functions to check mysql and start if not running.
#    Also a function for checking if running in a container
#
#####################################################################################
SQL_SERVICE="mariadb"

# Checks to see if running in a container
# -p to print status
check_for_container () {
  PRINT_STATUS=$1
  # Check if we're in a Docker container
  if [[ $(cat /proc/1/cgroup | head -n 1) == *"docker"* ]] || [[ -f /.dockerenv ]]; then  
    DOCKER_CONTAINER=true
    if [[ $PRINT_STATUS == "-p" ]]; then echo "ACA is running in a container..." | tee -a "$LOG_FILE"; fi
  else
    DOCKER_CONTAINER=false
    if [[ $PRINT_STATUS == "-p" ]]; then  echo "ACA is not running in a container..." | tee -a "$LOG_FILE"; fi
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
  PRINT_STATUS=$1
  PROCESS="mysqld"
  source /etc/os-release 
  if [ $ID = "ubuntu" ]; then 
     PROCESS="mariadb"
  fi
   # Check if mysql is already running, if not initialize
   if [[ $(pgrep -c -u mysql $PROCESS) -eq 0 ]]; then
   # Check if running in a container
      if [ $DOCKER_CONTAINER  = true ]; then
      # if in Docker container, avoid services that invoke the D-Bus
         # Check if mariadb is setup
         if [ ! -d "/var/lib/mysql/mysql/" ]; then
           echo "Installing mariadb"
           /usr/bin/mysql_install_db >> "$LOG_FILE"
           chown -R mysql:mysql /var/lib/mysql/ >> "$LOG_FILE"
         fi
         if [[ $PRINT_STATUS == "-p" ]]; then echo "Starting mysql..."; fi
         /usr/bin/mysqld_safe  --skip-syslog  >> "$LOG_FILE" &
         chown -R mysql:mysql /var/lib/mysql/ >> "$LOG_FILE"
         echo "Attempting to start mariadb"
         else #not a container
           systemctl enable $SQL_SERVICE & >> "$LOG_FILE";
           systemctl start $SQL_SERVICE & >> "$LOG_FILE";
         fi
     else # mysql process is running
     # check if mysql service is running
     if [ ! $DOCKER_CONTAINER  = true ]; then
      DB_STATUS=$(systemctl status mysql |grep 'running' | wc -l )
       if [ $DB_STATUS -eq 0 ]; then 
         echo "mariadb not running , attempting to restart"
         systemctl start mariadb >> "$LOG_FILE";
         sleep 2
       fi
     fi # non contanier mysql start
   fi   
}

# Basic check for marai db status, attempts restart if not running
check_mysql () {
 PROCESS="mysqld"
   source /etc/os-release 
   if [ $ID = "ubuntu" ]; then 
       PROCESS="mariadb"
   fi

 echo "Checking mysqld status..."
  if [ $DOCKER_CONTAINER  = true ]; then
       if [[ $(pgrep -c -u mysql $PROCESS ) -eq 0 ]]; then
          echo "mariadb not running , attempting to restart"
          chown mysql:mysql /var/log/mariadb/mariadb.log >> "$LOG_FILE";
          /usr/bin/mysqld_safe  --skip-syslog  >> "$LOG_FILE" &
       fi
  else  # not in a contianer
    DB_STATUS=$(systemctl status mysql |grep 'running' | wc -l )
    if [ $DB_STATUS -eq 0 ]; then 
       echo "mariadb not running , attempting to restart"
       systemctl start mariadb
    fi
  fi


# Wait for mysql to start before continuing.
  count=1;
  if [[ $PRINT_STATUS == "-p" ]]; then  echo "Testing mysqld connection..."| tee -a "$LOG_FILE"; fi

  until mysqladmin ping -h "localhost" --silent ; do
  ((count++))
  if [[ $count -gt 20 ]]; then
     break;
  fi
  sleep 1;
  done
   if [[ $count -gt 20 ]]; then
     echo "Timed out waiting for Mariadb to respond"
     exit 1;
   else
     echo "Mariadb started"
  fi
}

# Check for mysql root password , abort if not available 
check_mysql_root () {
  if [ -z $HIRS_MYSQL_ROOT_PWD ]; then
    if [ ! -f /etc/hirs/aca/aca.properties ]; then
      echo "aca.properties does not exist."
    else
      source /etc/hirs/aca/aca.properties;
      DB_ADMIN_PWD=$mysql_admin_password
    fi
  else  #HIRS_MYSQL_ROOT_PWD set
  DB_ADMIN_PWD=$HIRS_MYSQL_ROOT_PWD
fi

# Allow user to enter password if not using env variabel or file
if [ -z $DB_ADMIN_PWD ]; then
  read -p "Enter mysql root password" DB_ADMIN_PWD
  else 
      DB_ADMIN_PWD=$mysql_admin_password
fi

# Make sure root password is correct
$(mysql -u root -p$DB_ADMIN_PWD -e 'quit'  &> /dev/null);
  if [ $? -eq 0 ]; then
     echo "root password verified"  | tee -a "$LOG_FILE"
  else
     echo "MYSQL root password was not the default, not supplied,  or was incorrect"
     echo "      please set the HIRS_MYSQL_ROOT_PWD system variable and retry."
     echo "      ********** ACA Mysql setup aborted ********" ;
     exit 1;
  fi
}

check_db_cleared () {
   $(mysql -u root -e 'quit'  &> /dev/null);
   if [ $? -eq 0 ]; then
     echo "  Empty root password verified"  | tee -a "$LOG_FILE"
   else
     echo "  Mysql Root password is not empty"
   fi
   HIRS_DB_USER_EXISTS="$(mysql -uroot -sse "SELECT EXISTS(SELECT 1 FROM mysql.user WHERE user = 'hirs_db')")"
   if [[ $HIRS_DB_USER_EXISTS == 1 ]]; then
     echo "  hirs_db user exists"
     else
     echo "  hirs_db user does not exist"
     
   fi
   HIRS_DB_EXISTS=`mysql -uroot -e "SHOW DATABASES" | grep hirs_db`
   if [[ $HIRS_DB_EXISTS == "hirs_db" ]]; then 
      echo "  hirs_db databse exists"
    else 
      echo "  hirs_db database does not exists"
   fi
}

# restart maraidb
mysqld_reboot () {
  # reboot mysql server
  mysql -u root --password=$DB_ADMIN_PWD -e "SHUTDOWN"
  sleep 2
  check_for_container
  start_mysqlsd >> "$LOG_FILE";
}
