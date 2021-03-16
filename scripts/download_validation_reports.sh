#!/bin/bash

#User input parameters:
#$1 filter start date 'yyyy-mm-dd'
#$2 filter end date 'yyyy-mm-dd'
#$3 ACA address, default is localhost if not given

if [ -z "$3" ]
 then
  endpoint="https://localhost:8443/HIRS_AttestationCAPortal/portal/validation-reports"
 else
  endpoint="https://$3:8443/HIRS_AttestationCAPortal/portal/validation-reports"
fi
echo "$endpoint"
content=$(curl --insecure $endpoint/list)
rawTimes=$(jq -r '.data | map(.createTime | tostring) | join(",")' <<< "$content")
createTimes=""
for i in ${rawTimes//,/ }
do
	createTimes+="$(date -u +"%Y-%m-%d %H:%M:%S" -d @"$(($i/1000))"),"
done
deviceNames=$(jq -r '.data | map(.device.name) | join(",")' <<< "$content")
echo "Create times: $createTimes"
echo "Device names: $deviceNames"
curl --data "dateStart=$1&dateEnd=$2&createTimes=$createTimes&deviceNames=$deviceNames" --insecure $endpoint/download
