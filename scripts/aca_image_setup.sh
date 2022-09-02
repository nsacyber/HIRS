#!/bin/bash

# Setup db

if [[ $(pgrep -c -u mysql mysqld) -ne 0 ]]; then
    echo "shutting down ..."
    usr/bin/mysqladmin -u root shutdown -p;
 fi
 
/usr/libexec/mariadb-prepare-db-dir
nohup /usr/bin/mysqld_safe --basedir=/usr &>/dev/null &
MYSQLD_PID=$(pgrep -u mysql mysqld)
/usr/libexec/mariadb-wait-ready $MYSQLD_PID

mysql -fu root < /opt/hirs/scripts/common/db_create.sql.el7
mysql -fu root < /opt/hirs/scripts/common/secure_mysql.sql

# Start tomcat
/usr/libexec/tomcat/server start
