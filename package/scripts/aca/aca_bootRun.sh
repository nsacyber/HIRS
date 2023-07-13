#!/bin/bash
#####################################################################################
#
# Script to run ACA using the gradle spring pluing bootRun command with password set
#
#
####################################################################################

PASS_FILE="/etc/hirs/aca/application.properties"

declare -A props

if [ -f $PASS_FILE ]; then
  while IFS="=" read -r key value; do
    #echo "key is $key, value is $value"
    if [ ! -z "$key" ]; then
        props["$key"]="$value"
    fi
  done < "$PASS_FILE"
else
  echo "error reading $PASS_FILE"
  exit 1
fi

#echo "server_ssl_trust-store-password = " ${props["server.ssl.trust-store-password"]}
#echo "server_ssl_key-store-password = " ${props["server.ssl.key-store-password"]}

echo "Starting HIRS ACA on https://localhost:8443/HIRS_AttestationCAPortal/portal/index"

./gradlew bootRun --args="--server.ssl.trust-store-password=${props["server.ssl.trust-store-password"]} --server.ssl.key-store-password=${props["server.ssl.key-store-password"]}"
