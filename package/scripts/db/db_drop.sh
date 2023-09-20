#!/bin/bash

SRV_CNF=/etc/my.cnf.d/mariadb-server.cnf
CLIENT_CNF=/etc/my.cnf.d/client.cnf
SCRIPT_DIR=$( dirname -- "$( readlink -f -- "$0"; )";)
LOG_FILE=/dev/null
DB_ADMIN_PWD=$1

#source /etc/hirs/aca/aca.properties;
source $SCRIPT_DIR/mysql_util.sh

# Check for sudo or root user, not actually needed but a good idea 
if [ "$EUID" -ne 0 ]
     then echo "This script requires root.  Please run as root" 
     exit 1
fi

if [ -d /opt/hirs/scripts/db ]; then
    MYSQL_DIR="/opt/hirs/scripts/db"
  else
   MYSQL_DIR="$SCRIPT_DIR"
fi

echo "dropping hirs_db database"

if pgrep  mysqld >/dev/null 2>&1; then
     mysql -u root --password=$DB_ADMIN_PWD -e "FLUSH HOSTS; FLUSH LOGS; FLUSH STATUS; FLUSH PRIVILEGES; FLUSH USER_RESOURCES"
     mysql -u root --password=$DB_ADMIN_PWD -e "DROP USER 'hirs_db'@'localhost';"
     mysql -u root --password=$DB_ADMIN_PWD -e "DROP DATABASE IF EXISTS hirs_db;"
     echo "hirs_db database and hirs_db user removed"
   else
     echo "mysql is not running. DB was not removed."
fi

# reset the mysql root if the password was left in the properties fiel
if [ ! -z $DB_ADMIN_PWD ]; then
     echo "Resetting mysql root password to empty"
     mysql -u root --password=$DB_ADMIN_PWD -e "SET PASSWORD FOR "root@localhost" = PASSWORD('');"
     mysql -u "root" -e "FLUSH LOGS;"
   else
     echo "Note root password was NOT reset"
fi

# Remove key , cert and truststore entries from client.cnf andf mariadb.cnf

echo "Removing hirs cert references from mariadb configuration files"
grep -v "hirs" $SRV_CNF > tmpfile && mv tmpfile $SRV_CNF
grep -v "hirs" $CLIENT_CNF > tmpfile && mv tmpfile $CLIENT_CNF

echo "restarting mariadb"

mysql -u root -e "SHUTDOWN"
sleep 2
check_for_container
start_mysqlsd
