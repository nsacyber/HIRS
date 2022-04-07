#!/bin/bash
# Gather descriptors for NVMe devices  
nvmeParse () {
    str=$(nvme list -o json)
    count=$(echo "$str" | jq '.Devices | length')
    nvmedevices=()

    for ((i = 0 ; i < count ; i++ )); do
        elementJson=$(echo "$str" | jq .Devices["$i"])
        nvmedevices+=("$elementJson")        
    done
}
nvmeNumDevices () {
    printf "${#nvmedevices[@]}"
}
nvmeGetModelNumberForDevice () {
    dev="${1}"
    str=$(echo "${nvmedevices[$dev]}" | jq -r .ModelNumber)
    printf "$str"
}
nvmeGetSerialNumberForDevice () {
    dev="${1}"
    str=$(echo "${nvmedevices[$dev]}" | jq -r .SerialNumber)
    printf "$str"
}
nvmeGetEuiForDevice () {
    dev="${1}"
    devNode=$(echo "${nvmedevices[$dev]}" | jq -r .DevicePath)
    str=$(nvme id-ns "$devNode" -o json | jq -r .eui64)
    printf "$str"
}
nvmeGetNguidForDevice () {
    dev="${1}"
    devNode=$(echo "${nvmedevices[$dev]}" | jq -r .DevicePath)
    str=$(nvme id-ns "$devNode" -o json | jq -r .nguid)
    printf "$str"
}
