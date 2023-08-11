#!/bin/bash

PASS=$1
SCRIPT_DIR=$( dirname -- "$( readlink -f -- "$0"; )";)

 if [ -d /opt/hirs/scripts/db ]; then
    MYSQL_DIR="/opt/hirs/scripts/db"
  else
   MYSQL_DIR="$SCRIPT_DIR"
fi

echo "dropping hirs database"

 
if pgrep  mysqld >/dev/null 2>&1; then
  if [ -z ${PASS} ]; then
    mysql -u "root" < $MYSQL_DIR/db_drop.sql
  else
    mysql -u "root" -p$PASS  < $MYSQL_DIR/db_drop.sql
  fi
fi
