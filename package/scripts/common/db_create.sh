#!/bin/bash
#
###############################################################################
# HIRS DB creation
# Environment variables used:
# a. HIRS_MYSQL_ROOT_PWD: Set this variable if mysql root password is already set
# b. HIRS_DB_PWD: Set the pwd if default password to hirs_db user needs to be changed
# HIRS_MYSQL_ROOT_NEW_PWD wil be ignored if HIRS_MYSQL_ROOT_EXSITING_PWD is set.
################################################################################

# Set Mysql HIRS DB  password
if [ -z $HIRS_DB_PWD ]; then
   HIRS_DB_PWD="hirs_db"
fi
# Save hirs_db mysql user password to the properties file
mkdir -p /etc/hirs
echo "hibernate.connection.username="hirs_db"" > /etc/hirs/hibernate.properties
echo "hibernate.connection.password=$HIRS_DB_PWD" >> /etc/hirs/hibernate.properties

# Test the root password, error if the password doesnt work

if [ -z ${HIRS_MYSQL_ROOT_PWD} ]; then
    echo "HIRS_MYSQL_ROOT_PWD environment variable not set"
    mysql -fu root  -e 'quit'  &> /dev/null;
else
   echo "Using $HIRS_MYSQL_ROOT_PWD as the mysql root password"
   $(mysql -u root -p$HIRS_MYSQL_ROOT_PWD -e 'quit'  &> /dev/null);
fi
if [ $? -eq 0 ]; then
 echo "root password verified" 
else
 echo "MYSQL root password was not the default, not supplied,  or was incorrect"
 echo "      please set the HIRS_MYSQL_ROOT_PWD system variable and retry."
 echo "      ********** ACA Mysql setup aborted ********" ;
 exit 1;
fi

echo "HIRS_DB_PWD is $HIRS_DB_PWD"
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
mysql -u root --password=$HIRS_MYSQL_ROOT_PWD < /opt/hirs/scripts/common/db_create.sql
mysql -u root --password=$HIRS_MYSQL_ROOT_PWD < /opt/hirs/scripts/common/secure_mysql.sql
mysql -u root --password=$HIRS_MYSQL_ROOT_PWD -e "ALTER USER 'hirs_db'@'localhost' IDENTIFIED BY '"$HIRS_DB_PWD"'; FLUSH PRIVILEGES;";

