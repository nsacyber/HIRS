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
SHORTOPTS=m:s:h
LONGOPTS=start-date:,end-date:,ip:,system-only,component-only,manufacturer:,serial:,help
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

helpText="\n\n\nHELP MENU\n\nThe following options are available:\n--start-date\t\t<yyyy-mm-dd>\tDefault: 1970-01-01\tThe earliest date to return validation reports from.\n"
helpText+="--end-date\t\t<yyyy-mm-dd>\tDefault: current time\tThe latest date to return validation reports from.\n"
helpText+="--ip\t\t\t<ACA address>\tDefault: localhost\tThe IP address where the ACA is located.\n"
helpText+="--system-only\t\t\t\t\t\t\tReturn only system information from validation reports.\n"
helpText+="--component-only\t\t\t\t\t\tReturn only component information from validation reports.\n"
helpText+="-m|--manufacturer\t<manufacturer's name>\t\t\tReturn only the validation report of the device from this manufacturer.\n"
helpText+="-s|--serial\t\t<serial number>\t\t\t\tReturn only the validation report of the device with this serial number.\n"

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
		-h|--help)
			printf "$helpText"
			exit 0
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

#echo "start date: $startDate, end date: $endDate, ip: $ip, system: $system, component: $component, manufacturer: $manufacturer, serial: $serial"

#call ACA for validation report
endpoint="https://$ip:8443/HIRS_AttestationCAPortal/portal/validation-reports"
echo "$endpoint"
content=$(curl --insecure $endpoint/list)

#Parse JSON response for create times and device names
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
