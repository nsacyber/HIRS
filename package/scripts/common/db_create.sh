#!/bin/bash
DB_DEFAULT_PWD="hirs_db"

# Check if we're in a Docker container
if [ -f /.dockerenv ]; then
    DOCKER_CONTAINER=true
else
    DOCKER_CONTAINER=false
fi

echo "Creating HIRS Database..."

if [ $DOCKER_CONTAINER  = true ]; then
   # If in Docker container, avoid services that invoke the D-Bus
    if [[ $(pgrep -c -u mysql mysqld) -eq 0 ]]; then
       echo "running in a container..."
       /usr/libexec/mysql-prepare-db-dir > /dev/null 2>&1 
       nohup /usr/bin/mysqld_safe > /dev/null 2>&1 &
    fi
else
    SQL_SERVICE=`/opt/hirs/scripts/common/get_db_service.sh`
    systemctl $SQL_SERVICE enable
    systemctl $SQL_SERVICE start
fi

mysqladmin -u root password $DB_DEFAULT_PWD
DB_CREATE_SCRIPT=/opt/hirs/scripts/common/db_create.sql.el7

mysql -u root --password="$DB_DEFAULT_PWD" < $DB_CREATE_SCRIPT

// Set logfile for 
#echo '[mysqld]' >> /etc/my.cnf
#echo 'log-error=/var/log/mariadb/hirs_db.log' >> /etc/my.cnf

