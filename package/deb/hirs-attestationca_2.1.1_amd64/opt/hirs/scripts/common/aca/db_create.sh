#!/bin/bash

echo "Creating HIRS Database..."
if [ -f "/etc/init.d/mariadb" ]; then
    /etc/init.d/mariadb start  
elif [ -f "/etc/init.d/mysql" ]; then
    /etc/init.d/mysql start
else
    systemctl start mariadb
fi
DB_CREATE_SCRIPT=/opt/hirs/scripts/common/db_create.sql.ubuntu
mysql -u root < $DB_CREATE_SCRIPT
