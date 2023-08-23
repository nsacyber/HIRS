#!/bin/bash

SCRIPT_DIR=$( dirname -- "$( readlink -f -- "$0"; )"; )

source /etc/hirs/aca/aca.properties;

if [ -z $mysql_admin_password ]; then
  read -p "Enter mysql root password" DB_ADMIN_PWD
  else 
      DB_ADMIN_PWD=$mysql_admin_password
fi

if [ -z $hirs_db_password ]; then
  read -p "Enter mysql root password" hirs_db_password
  else 
      HIRS_DB_PWD=$hirs_db_password
fi

echo "HIRS_DB_PWD is $HIRS_DB_PWD"
echo "DB_ADMIN_PWD is $DB_ADMIN_PWD"

# check if hirs_db user exists
RESULT="$(mysql -u root --password=$DB_ADMIN_PWD -sse "SELECT EXISTS(SELECT 1 FROM mysql.user WHERE user = 'hirs_db')")"

if [ "$RESULT" = 1 ]; then
  echo "hirs_db user found, dropping hirs-db user"
  mysql -u root --password=$DB_ADMIN_PWD -e "DROP USER 'hirs_db'@'localhost'"
  if [ $? -ne 0 ]; then
     echo "Removing the existing hirs_db user failed"
     else
     echo "Removing the existing hirs_db was successful"
  fi
  else
  echo "no hirs_db user found, creating one..."
fi

echo "Creating hirs_db user"
mysql -u root --password=$DB_ADMIN_PWD -e "CREATE USER 'hirs_db'@'localhost' IDENTIFIED BY 'hirs_db';" 
mysql -u root --password=$DB_ADMIN_PWD -e "ALTER USER 'hirs_db'@'localhost' IDENTIFIED BY '"$HIRS_DB_PWD"'; FLUSH PRIVILEGES;"
mysql -u root --password=$DB_ADMIN_PWD -e "GRANT ALL ON hirs_db.* TO 'hirs_db'@'localhost' REQUIRE X509;"
mysql -u root --password=$DB_ADMIN_PWD -e "FLUSH PRIVILEGES;"

echo "Checking hirs_db user..."
# check user
mysql -u hirs_db --password=$HIRS_DB_PWD -e "SHOW DATABASES;";