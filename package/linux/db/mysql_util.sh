#!/bin/bash
#####################################################################################
#
# Functions to check mysql and start if not running.
#    Also a function for checking if running in a container
#
#####################################################################################
SQL_SERVICE="mariadb"

# Checks to see if systemctl is available
# -p to print status
check_systemd () {
  DB_SERVICE=false
  SYSD_SERVICE=false
  PRINT_STATUS=$1
  # Check if systemctl is present (not present in a Docker container)
  if [[ $(ps --no-headers -o comm 1) == "systemd" ]]; then
    SYSD_SERVICE=true
    systemctl is-active --quiet mariadb
    if [[ $? -eq 0 ]]; then
       DB_SERVICE=true
    fi
  fi
    if [[ $PRINT_STATUS == "-p" ]] && [[ $DB_SERVICE == "true" ]]; then echo "Systemd:MariaDB service is available" | tee -a "$LOG_FILE"; fi
    if [[ $PRINT_STATUS == "-p" ]] && [[ $DB_SERVICE == "false" ]]; then  echo "Systemd:MariaDB service is NOT available" | tee -a "$LOG_FILE"; fi
  if [ -d /opt/hirs/aca/scripts/db ]; then
     MYSQL_DIR="/opt/hirs/aca/scripts/db"
   else
     MYSQL_DIR="$SCRIPT_DIR/../db"
  fi
}

# Check for mysql command line
check_mariadb_install () {
   type mysql >/dev/null 2>&1 && installed=true || installed=false
   if [ "$installed" = true ]; then
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
  if [ "$ID" = "ubuntu" ]; then
     PROCESS="mariadb"
  fi
   # Make sure log file has correct permissions
   chown -R mysql:mysql /var/lib/mysql/ >> "$LOG_FILE"
   if [ "$DB_SERVICE" = true ]; then
   	  systemctl is-active --quiet mariadb
        if [[ $? -ne 0 ]]; then
          echo "mariadb service not running , attempting to restart"
          systemctl start mariadb >> "$LOG_FILE";
          sleep 2
        fi
   else  # Not using Systemd
    # Check if mysql is already running, if not initialize
     if [[ $(pgrep -c $PROCESS) -eq 0 ]]; then
         # Check if mariadb is setup
         if [ ! -d "/var/lib/mysql/mysql/" ]; then
           echo "Installing mariadb" | tee -a "$LOG_FILE";
           /usr/bin/mysql_install_db >> "$LOG_FILE"
           chown -R mysql:mysql /var/log/mariadb/
         fi
         if [[ $PRINT_STATUS == "-p" ]]; then echo "Starting mysql..."; fi
         /usr/bin/mysqld_safe  --skip-syslog  >> "$LOG_FILE" &
         chown -R mysql:mysql /var/lib/mysql/ >> "$LOG_FILE"
         echo "Attempting to start mariadb process..." | tee -a "$LOG_FILE";
      fi      
  fi
}

# Basic check for marai db status, attempts restart if not running
check_mysql () {
 PROCESS="mysqld"
   source /etc/os-release
   if [ "$ID" = "ubuntu" ]; then
       PROCESS="mariadb"
   fi

 echo "Checking mysqld status..."
  if [ "$DB_SERVICE"  = true ]; then
    systemctl is-active --quiet mariadb
    if [[ $? -ne 0 ]]; then
       echo "mariadb service not running , attempting to restart"
       systemctl start mariadb
    fi
  else  # No systemctl 
    if [[ $(pgrep -c $PROCESS ) -eq 0 ]]; then
      echo "mariadb process not running , attempting to restart"
      chown mysql:mysql /var/log/mariadb/mariadb.log >> "$LOG_FILE";
      /usr/bin/mysqld_safe  --skip-syslog  >> "$LOG_FILE" &
    fi
  fi

 if [ "$DB_SERVICE"  = true ]; then
   systemctl is-active --quiet mariadb
    if [[ $? -eq 0 ]]; then 
       echo "mariadb service started" | tee -a "$LOG_FILE";
    fi
 else   
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
     echo "Timed out waiting for Mariadb to respond" | tee -a "$LOG_FILE";
     exit 1;
   else
     echo "Mariadb started" | tee -a "$LOG_FILE";
   fi
fi
}

# Check for mysql root password , abort if not available 
check_mysql_root () {
  if [ -z "$HIRS_MYSQL_ROOT_PWD" ]; then
    if [ ! -f /etc/hirs/aca/aca.properties ]; then
      echo "aca.properties does not exist." | tee -a "$LOG_FILE";
    else
      source /etc/hirs/aca/aca.properties;
      DB_ADMIN_PWD=$mysql_admin_password
    fi
  else  #HIRS_MYSQL_ROOT_PWD set
  DB_ADMIN_PWD=$HIRS_MYSQL_ROOT_PWD
fi

# Allow user to enter password if not using env variabel or file
if [ -z "$DB_ADMIN_PWD" ]; then
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

check_hirs_db_user () {
PRINT_STATUS=$1
HIRS_DB_USER_EXISTS="$(mysql -uroot --password="$DB_ADMIN_PWD" -sse "SELECT EXISTS(SELECT 1 FROM mysql.user WHERE user = 'hirs_db')")"
   if [[ $HIRS_DB_USER_EXISTS == "1" ]]; then
       if [[ $PRINT_STATUS == "-p" ]];then  echo "  hirs_db user exists" | tee -a "$LOG_FILE"; fi;
     else
       if [[ $PRINT_STATUS == "-p" ]]; then echo "  hirs_db user does not exist" | tee -a "$LOG_FILE"; fi;
   fi
}

check_hirs_db () {
PRINT_STATUS=$1
HIRS_DB_EXISTS="$(mysql -uroot --password="$DB_ADMIN_PWD" -e "SHOW DATABASES" | grep hirs_db)"
   if [[ $HIRS_DB_EXISTS == "hirs_db" ]]; then
      if [[ $PRINT_STATUS == "-p" ]];then echo "  hirs_db database exists" | tee -a "$LOG_FILE"; fi;
    else
      if [[ $PRINT_STATUS == "-p" ]];then echo "  hirs_db database does not exists" | tee -a "$LOG_FILE"; fi;
   fi
}

check_db_cleared () {
   $(mysql -u root -e 'quit'  &> /dev/null);
   if [ $? -eq 0 ]; then
     echo "  Empty Mysql root password verified"  | tee -a "$LOG_FILE"
   else
     echo "  Mysql Root password is not empty" | tee -a "$LOG_FILE";
   fi
   HIRS_DB_USER_EXISTS="$(mysql -uroot -sse "SELECT EXISTS(SELECT 1 FROM mysql.user WHERE user = 'hirs_db')")"
   if [[ $HIRS_DB_USER_EXISTS == "1" ]]; then
     echo "  hirs_db user exists" | tee -a "$LOG_FILE";
     else
     echo "  hirs_db user does not exist" | tee -a "$LOG_FILE";
   fi
   HIRS_DB_EXISTS=$(mysql -uroot -e "SHOW DATABASES" | grep hirs_db)
   if [[ $HIRS_DB_EXISTS == "1" ]]; then 
      echo "  hirs_db databse exists" | tee -a "$LOG_FILE";
    else 
      echo "  hirs_db database does not exists" | tee -a "$LOG_FILE";
   fi
}

clear_hirs_user () {
$(mysql -u root -e 'quit'  &> /dev/null);
   if [ $? -eq 0 ]; then
       HIRS_DB_USER_EXISTS="$(mysql -uroot -sse "SELECT EXISTS(SELECT 1 FROM mysql.user WHERE user = 'hirs_db')")"
       if [[ $HIRS_DB_USER_EXISTS == "1" ]]; then
          mysql -u root --password="$DB_ADMIN_PWD" -e "DROP USER 'hirs_db'@'localhost';"
          echo "hirs_db user found and deleted"
       fi
   fi
}

clear_hirs_db () {
$(mysql -u root -e 'quit'  &> /dev/null);
   if [ $? -eq 0 ]; then
      mysql -u root --password="$DB_ADMIN_PWD" -e "DROP DATABASE IF EXISTS hirs_db;"
   fi
}

wait_for_mysql () {
echo "Waiting for Mariadb..." | tee -a "$LOG_FILE";
  count=0
  until [ "mysqladmin ping -h localhost --silent" ] || [ "$count" -gt 20 ]; do
        ((count++))
        sleep 1
  done
   if [[ $count -gt 20 ]]; then
     echo "Timed out waiting for Mysqld to respond" | tee -a "$LOG_FILE";
   else
     echo "Mariadb started" | tee -a "$LOG_FILE";
  fi
}

# restart maraidb
mysqld_reboot () {
  # reboot mysql server
   PROCESS="mysqld"
   source /etc/os-release 
   if [ "$ID" = "ubuntu" ]; then
     PROCESS="mariadb"
   fi
   echo "Restarting Mariadb ...." | tee -a "$LOG_FILE";
   if [ "$DB_SERVICE" = true ]; then
      echo "Shutting down and restarting mysql service"  | tee -a "$LOG_FILE";
      systemctl stop mariadb >> "$LOG_FILE";
      sleep 2
      systemctl start mariadb >> "$LOG_FILE";
   else # No systemd
     echo "Shutting down and restarting mysql process"  | tee -a "$LOG_FILE";
      mysql -u root --password="$DB_ADMIN_PWD"  -e "SHUTDOWN"
      sleep 1
      # Make sure mysql has stopped
      if [[ $(pgrep -c $PROCESS) -ne 0 ]]; then
          pkill $PROCESS
      fi 
      chown mysql:mysql /var/log/mariadb/mariadb.log >> "$LOG_FILE";
      /usr/bin/mysqld_safe  --skip-syslog >> "$LOG_FILE" &
      sleep 1
      check_mysql
      wait_for_mysql
   fi
}
