#!/bin/bash
DB_DEFAULT_PWD="hirs_db"

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
       /usr/bin/mysql_install_db
       chown -R mysql:mysql /var/lib/mysql/  
       chown -R mysql:mysql /var/log/mariadb/
       nohup /usr/bin/mysqld_safe > /dev/null 2>&1 &
   else
       SQL_SERVICE=`/opt/hirs/scripts/common/get_db_service.sh`
       systemctl $SQL_SERVICE enable
       systemctl $SQL_SERVICE start
   fi
  # Set intial password
  mysqladmin -u root password $DB_DEFAULT_PWD
fi
# Initialize the hirs_db database

DB_CREATE_SCRIPT=/opt/hirs/scripts/common/db_create.sql.el7
mysql -u root --password="$DB_DEFAULT_PWD" < $DB_CREATE_SCRIPT
