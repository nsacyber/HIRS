#!/bin/bash

SQL_SERVICE=`/opt/hirs/scripts/common/get_db_service.sh`

echo "Creating HIRS Database..."
chkconfig $SQL_SERVICE on
service $SQL_SERVICE start

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
