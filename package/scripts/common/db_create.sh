#!/bin/bash

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
        /usr/libexec/mariadb-prepare-db-dir
        nohup /usr/bin/mysqld_safe --basedir=/usr &>/dev/null &
        MYSQLD_PID=$(pgrep -u mysql mysqld)
        /usr/libexec/mariadb-wait-ready $MYSQLD_PID
    fi
else
    SQL_SERVICE=`/opt/hirs/scripts/common/get_db_service.sh`
    chkconfig $SQL_SERVICE on
    service $SQL_SERVICE start
fi

CENTOS_VER=`/opt/hirs/scripts/common/get_centos_major_version.sh`
if [ $CENTOS_VER -eq "6" ] ; then
    DB_CREATE_SCRIPT=/opt/hirs/scripts/common/db_create.sql.el6
elif [ $CENTOS_VER -eq "7" ] ; then
    DB_CREATE_SCRIPT=/opt/hirs/scripts/common/db_create.sql.el7
else
    echo "Unsupported CentOS version: ${CENTOS_VER}"
    exit 1
fi

mysql -u root < $DB_CREATE_SCRIPT
