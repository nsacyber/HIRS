#!/bin/bash

#User input parameters:
#$1 filter start date 'yyyy-mm-dd'
#$2 filter end date 'yyyy-mm-dd'
#$3 ACA address, default is localhost if not given

#check for getopt(1) on local system
getopt --test > /dev/null
if [[ ${PIPESTATUS[0]} -ne 4 ]]
then
	echo "getopt is required to use this script, please ensure installation!"
else
	echo "getopt detected"
fi

#set parameter names and call getopts on inputsi, then parse/assign arguments
SHORTOPTS=m:s:
LONGOPTS=start-date:,end-date:,ip:,system-only,component-only,manufacturer:,serial:
PARSED=$(getopt --options=$SHORTOPTS --longoptions=$LONGOPTS --name "$0" -- "$@")
if [[ ${PIPESTATUS[0]} -ne 0 ]]
then 
	exit 2
fi
eval set -- "$PARSED"
startDate=
endDate=
ip=localhost
system=
component=
manufacturer=
serial=
while true
do
	case "$1" in
		--start-date)
			startDate="$2"
			shift 2
			;;
		--end-date)
			endDate="$2"
			shift 2
			;;
		--ip)
			ip="$2"
			shift 2
			;;
		--system-only)
			system=true
			shift
			;;
		--component-only)
			component=true
			shift
			;;
		-m|--manufacturer)
			manufacturer="$2"
			shift 2
			;;
		-s|--serial)
			serial="$2"
			shift 2
			;;
		--)
			shift
			break
			;;
		*)
			echo "Programming error"
			exit 3
			;;
	esac
done

echo "start date: $startDate, end date: $endDate, ip: $ip, system: $system, component: $component, manufacturer: $manufacturer, serial: $serial"

#call ACA for validation report
endpoint="https://$ip:8443/HIRS_AttestationCAPortal/portal/validation-reports"
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
curl --data "dateStart=$startDate&dateEnd=$endDate&createTimes=$createTimes&deviceNames=$deviceNames&system=$system&component=$component&manufacturer=$manufacturer&serial=$serial" --insecure $endpoint/download
