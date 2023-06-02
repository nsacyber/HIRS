#!/bin/bash

echo "dropping hirs database"

if pgrep  mysqld >/dev/null 2>&1; then
  if [ -z ${HIRS_MYSQL_ROOT_PWD} ]; then
    mysql -u "root" < /opt/hirs/scripts/db/db_drop.sql
  else
    mysql -u "root" -p$HIRS_MYSQL_ROOT_PWD  < /opt/hirs/scripts/db/db_drop.sq1
  fi
fi
