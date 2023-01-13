#!/bin/bash

# Check if we're in a Docker container
if [ -f /.dockerenv ]; then
    DOCKER_CONTAINER=true
else
    DOCKER_CONTAINER=false
fi

echo "Creating HIRS Database..."

if [ $DOCKER_CONTAINER  = true ]; then
    echo "running in a container..."
    # If in Docker container, avoid services that invoke the D-Bus
  #  if [[ $(pgrep -c -u mysql mysqld) -eq 0 ]]; then
      #  /usr/libexec/mariadb-prepare-db-dir
      #  nohup /usr/bin/mysqld_safe --basedir=/usr &>/dev/null &
      #  MYSQLD_PID=$(pgrep -u mysql mysqld)
      #  /usr/libexec/mariadb-wait-:ready $MYSQLD_PID
   # fi
else
    SQL_SERVICE=`/opt/hirs/scripts/common/get_db_service.sh`
    systemctl $SQL_SERVICE enable
    systemctl $SQL_SERVICE start
fi

DB_CREATE_SCRIPT=/opt/hirs/scripts/common/db_create.sql.el7

mysql -u root --password="hirs_db" < $DB_CREATE_SCRIPT

// Set logfile for 
echo '[mysqld]' >> /etc/my.cnf
echo 'log-error=/var/log/mariadb/hirs_db.log' >> /etc/my.cnf

