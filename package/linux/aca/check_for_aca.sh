#!/bin/bash
########################################################################################
# Checks for ACA portal page on the local device
# Waits for tomcat (ACA) to respond or times out after 20 seconds
#
#########################################################################################

ACA_URL="https://localhost:8443/HIRS_AttestationCAPortal/portal/index"
echo "Waiting for tomcat..."
  count=0
  until [ "`curl --silent --connect-timeout 1 --insecure -I  $ACA_URL | grep -c 'Date'`" == 1 ] || [[ $count -gt 20 ]]; do
        ((count++))
        sleep 1
  done
   if [[ $count -gt 20 ]]; then
     echo "Timed out waiting for tomcat to respond"
   else
     echo "Tomcat (ACA) started"
  fi