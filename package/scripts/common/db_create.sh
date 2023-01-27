#!/bin/bash
#
###############################################################################
# HIRS DB creation
# Environment variables used:
# a. HIRS_MYSQL_ROOT_EXSITING_PWD: set this variable if mysql root password is already set
# b. HIRS_MYSQL_ROOT_NEW_PWD: set this variable if install needs to set new pwd
# c. HIRS_DB_PWD: Set the pwd if default password to hirs_db user needs to be changed
# HIRS_MYSQL_ROOT_NEW_PWD wil be ignored if HIRS_MYSQL_ROOT_EXSITING_PWD is set.
################################################################################

# Set Mysql root password
if [ ! -z $HIRS_MYSQL_ROOT_EXSITING_PWD ]; then
   HIRS_MYSQL_ROOT_PWD=$HIRS_MYSQL_ROOT_EXSITING_PWD;
elif [ ! -z $HIRS_MYSQL_ROOT_NEW_PWD ]; then
   HIRS_MYSQL_ROOT_PWD=$HIRS_MYSQL_ROOT_NEW_PWD;
else
   HIRS_MYSQL_ROOT_PWD="root";
fi

echo "HIRS_DB_PWD is $HIRS_DB_PWD"
echo "HIRS_MYSQL_ROOT_EXSITING_PWD is $HIRS_MYSQL_ROOT_EXSITING_PWD"
echo "HIRS_MYSQL_ROOT_NEW_PWD is $HIRS_MYSQL_ROOT_NEW_PWD"
echo "HIRS_MYSQL_ROOT_PWD is $HIRS_MYSQL_ROOT_PWD"

# Check if we're in a Docker container
if [ -f /.dockerenv ]; then
    DOCKER_CONTAINER=true
else
    DOCKER_CONTAINER=false
fi

# Check if mysql is already running, if not initialize
if [[ $(pgrep -c -u mysql mysqld) -eq 0 ]]; then
# Check if running in a container
   if [ $DOCKER_CONTAINER  = true ]; then
   # if in Docker container, avoid services that invoke the D-Bus
       echo "ACA is running in a container..."
       # Check if mariadb is setup
       if [ ! -d "/var/lib/mysql/mysql/" ]; then
           echo "Installing mariadb"
           /usr/bin/mysql_install_db 
           chown -R mysql:mysql /var/lib/mysql/  
       fi
       echo "Starting mysql...."
       #nohup /usr/bin/mysqld_safe > /dev/null 2>&1 &
       chown -R mysql:mysql /var/log/mariadb
       /usr/bin/mysqld_safe &
   else
       SQL_SERVICE=`/opt/hirs/scripts/common/get_db_service.sh`
       systemctl $SQL_SERVICE enable
       systemctl $SQL_SERVICE start
   fi
fi

# Wait for mysql to start before continuing.
echo "Checking mysqld status..."
while ! mysqladmin ping -h "$localhost" --silent; do
  sleep 1;
done

# Create the hirs_db database
echo "Creating HIRS Database..." 

if [ ! -z $HIRS_MYSQL_ROOT_EXSITING_PWD ]; then
   echo "processing with hirs root set"
   mysql -u root --password=$HIRS_MYSQL_ROOT_PWD < /opt/hirs/scripts/common/db_create.sql
   mysql -u root --password=$HIRS_MYSQL_ROOT_PWD < /opt/hirs/scripts/common/secure_mysql.sql
else 
   echo "processing with hirs root NOT set"
   mysql -u root < /opt/hirs/scripts/common/db_create.sql
   mysql -u root < /opt/hirs/scripts/common/secure_mysql.sql
   mysqladmin -u root --silent password $HIRS_MYSQL_ROOT_PWD || true > /dev/null 2>&1
fi

if [ ! -z $HIRS_DB_PWD ]; then
   echo "Setting hirs_db password"
   mysql -u root --password=$HIRS_MYSQL_ROOT_PWD -e "ALTER USER 'hirs_db'@'localhost' IDENTIFIED BY '"$HIRS_DB_PWD"'; FLUSH PRIVILEGES;";
fi

