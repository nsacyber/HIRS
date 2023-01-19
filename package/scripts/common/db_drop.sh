#!/bin/bash

# Get the current password from the perstence.properties file
file="/etc/hirs/persistence.properties"
# Change java key/value pairs into  valid bash key/value pairs
function prop {
    grep "${1}" ${file} | cut -d'=' -f2 | xargs
}

user="root"
# user=$(prop 'persistence.db.user')
pwd=$(prop 'persistence.db.password')

# drop the database
mysql -u "$user" --password="$pwd" < /opt/hirs/scripts/common/db_drop.sql
