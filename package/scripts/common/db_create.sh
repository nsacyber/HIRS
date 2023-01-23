#!/bin/bash
#
###############################################
# HIRS DB creation
# Conditions to address
# a. Install is called mutiple times
# b. Another app sets the root password 
# c. ACA is updated 
# d. ACA is updated after a DB password change
################################################

if [ -z ${HIRS_DB_PWD+x} ]; then
 DB_DEFAULT_PWD="hirs_db";
 else 
 DB_DEFAULT_PWD=$HIRS_DB_PWD;
fi

# Check if we're in a Docker container
if [ -f /.dockerenv ]; then
    DOCKER_CONTAINER=true
else
    DOCKER_CONTAINER=false
fi

echo "Creating HIRS Database..."

# Check if mysql is already running, if not initialize
if [[ $(pgrep -c -u mysql mysqld) -eq 0 ]]; then
# Check if running in a container
   if [ $DOCKER_CONTAINER  = true ]; then
   # if in Docker container, avoid services that invoke the D-Bus
       echo "ACA is running in a container..."
       # Check is Dbus is running
       if [[ $(pgrep -c -u dbus dbus) -eq 0 ]]; then
           # Start DBus
           mkdir -p /var/run/dbus
           if [ -e /var/run/dbus/pid ]; then
             rm /var/run/dbus/pid
           fi
           if [ -e /var/run/dbus/system_bus_socket ]; then
             rm /var/run/dbus/system_bus_socket
           fi
           if [ -e /run/dbus/messagebus.pid ]; then
             rm /run/dbus/messagebus.pid
           fi
           echo "starting dbus";
           dbus-daemon --fork --system
       fi
       # Check if mariadb is setup
       if [ ! -d "/var/lib/mysql/mysql/" ]; then
           echo "Installing mariadb"
           /usr/bin/mysql_install_db 
           chown -R mysql:mysql /var/lib/mysql/  
           chown -R mysql:mysql /var/log/mariadb/
       fi
       echo "Starting mysql...."
       #nohup /usr/bin/mysqld_safe > /dev/null 2>&1 &
       /usr/bin/mysqld_safe &
   else
       SQL_SERVICE=`/opt/hirs/scripts/common/get_db_service.sh`
       systemctl $SQL_SERVICE enable
       systemctl $SQL_SERVICE start
   fi
fi

# Wait for mysql to start before continuing. Exit if it doesnt start.
count=0; 
while ([ $(pgrep -c -u mysql mysqld) -eq 0 ] && [ "$count" -lt 5 ]); do
  sleep 1;
  count=$((count+1));
done

if [ "$count" -gt 4 ]; then
  echo "Mysql failed to start"
  exit 1;
else
  echo "mysql is started"
fi

# Set intial password, ingore result in case its already been set
echo "Setting mysql password"
mysqladmin -u root --silent password $DB_DEFAULT_PWD || true > /dev/null 2>&1

# Create the hirs_db database  
DB_CREATE_SCRIPT=/opt/hirs/scripts/common/db_create.sql.el7
mysql -u root --password="$DB_DEFAULT_PWD" < $DB_CREATE_SCRIPT
