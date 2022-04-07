#!/bin/bash
lshwParse () {
    type="${1}"
    str=$(lshw -c "$type" -numeric)
    numLines=$(printf "$str" | wc -l)
    items=()
    busitems=()
    
    
    parsing=""
    lineItr=0
    while read -r line; do
        lineItr=$((lineItr+1))
        lastLine=""
        if (($lineItr > $numLines)); then 
            parsing+="$line"$'\n'
            lastLine="1"
        fi
        if (printf "$line" | grep --quiet -e "^[[:space:]]*\*-.*[[:space:]]*$") || [ -n "$lastLine" ]; then
            if [ -n "$parsing" ]; then
                items+=("${parsing}")
            fi
            parsing=""
        fi
        parsing+="$line"$'\n'
    done <<< "$str"
    
    numItemsDec=$(printf "%d" "0x"${#items[@]})
    for ((i = 0 ; i < numItemsDec ; i++ )); do
        matchesType=""
        if (printf "${items[$i]}" | grep --quiet -e "^\*-"$type":\?[0-9A-Fa-f]*[[:space:]]*\(DISABLED\)\?$"); then
            matchesType="1"
        fi
        isPhysical=""
        if (printf "${items[$i]}" | grep --quiet -e "^bus info:.*$"); then
            isPhysical="1"
        fi

        if [ -n "$matchesType" ] && [ -n "$isPhysical" ]; then
            busitems+=("${items[$i]}")
        fi        
    done
}
lshwDisk () {
    lshwParse "disk"
}
lshwDisplay () {
    lshwParse "display"
}
lshwNetwork () {
    lshwParse "network"
}
lshwNumBusItems () {
    printf "${#busitems[@]}"
}
lshwGetVendorIDFromBusItem () {
    itemnumber="${1}"
    result=""
    str=$(echo "${busitems[$itemnumber]}" | grep -e "^vendor:.*[^\[]\[.\+$" | sed 's/^vendor:.*[^\[]\[\([0-9A-Fa-f]\+\)\]$/\1/')
    if [ -n "$str" ]; then
        result="0000$str"
        result="${result: -4}"
    fi
    printf "$result"
}
lshwGetProductIDFromBusItem () {
    itemnumber="${1}"
    result=""
    str=$(echo "${busitems[$itemnumber]}" | grep -e "^product:.*[^\[]\[.\+$" | sed 's/^product:.*[^\[]\[[0-9A-Fa-f]\+:\([0-9A-Fa-f]\+\)\]$/\1/')
    if [ -n "$str" ]; then
        result="0000$str"
        result="${result: -4}"
    fi
    printf "$result"
}
lshwGetVersionFromBusItem () {
    itemnumber="${1}"
    result=""
    str=$(echo "${busitems[$itemnumber]}" | grep -e "^version:.*$" | sed 's/^version: \([0-9A-Za-z]\+\)$/\1/')
    if [ -n "$str" ]; then
        result=$str
    fi
    printf "$result"
}
lshwGetSerialFromBusItem () {
    itemnumber="${1}"
    result=""
    str=$(echo "${busitems[$itemnumber]}" | grep -e "^serial:.*$" | sed 's/^serial: \([0-9A-Za-z:-]\+\)$/\1/')
    if [ -n "$str" ]; then
        result=$str
    fi
    printf "$result"
}
lshwGetLogicalNameFromBusItem () {
    itemnumber="${1}"
    result=""
    str=$(echo "${busitems[$itemnumber]}" | grep -e "^logical name:.*$" | sed 's/^logical name: \(.\+\)$/\1/')
    if [ -n "$str" ]; then
        result=$str
    fi
    printf "$result"
}
lshwGetVendorNameFromBusItem () {
    itemnumber="${1}"
    result=""
    str=$(echo "${busitems[$itemnumber]}" | grep -e "^vendor:.*$" | sed 's/^vendor: \([0-9A-Za-z -]\+\) \?\[\?.*$/\1/')
    if [ -n "$str" ]; then
        result=$str
    fi
    printf "$result"
}
lshwGetProductNameFromBusItem () {
    itemnumber="${1}"
    result=""
    str=$(echo "${busitems[$itemnumber]}" | grep -e "^product:.*$" | sed 's/^product: \([0-9A-Za-z\(\) -]\+\) \?\[\?.*$/\1/')
    if [ -n "$str" ]; then
        result=$str
    fi
    printf "$result"
}
lshwBusItemBluetoothCap () {
    itemnumber="${1}"
    result=""
    if (echo "${busitems[$itemnumber]}" | grep --quiet "capabilities.*bluetooth"); then
        result="1"
    fi
    printf "$result"
}
lshwBusItemEthernetCap () {
    itemnumber="${1}"
    result=""
    if (echo "${busitems[$itemnumber]}" | grep --quiet "capabilities.*ethernet"); then
        result="1"
    fi
    printf "$result"
}
lshwBusItemWirelessCap () {
    itemnumber="${1}"
    result=""
    if (echo "${busitems[$itemnumber]}" | grep --quiet "capabilities.*wireless"); then
        result="1"
    fi
    printf "$result"
}
ethtoolPermAddr () {
    iface="${1}"
    str=$(ethtool -P "$iface" 2> /dev/null | grep -e "^Perm.*$" | sed 's/^Permanent address: \([0-9a-f:]\+\)$/\1/')
    printf "$str"
}
#lshwParse "disk"
#lshwNetwork
#echo ${items[0]}
#echo ${#busitems[@]}
#echo ${busitems[*]}
#ven=$(lshwGetVendorIDFromBusItem "0")
#prod=$(lshwGetProductIDFromBusItem "0")
#rev=$(lshwGetVersionFromBusItem "0")
#serial=$(lshwGetSerialFromBusItem "0")
#venname=$(lshwGetVendorNameFromBusItem "0")
#prodname=$(lshwGetProductNameFromBusItem "0")
#bluetoothCap=$(lshwBusItemBluetoothCap)
#ethernetCap=$(lshwBusItemEthernetCap)
#wirelessCap=$(lshwBusItemWirelessCap)
#echo $ven
#echo $prod
#echo $rev
#echo $serial
#echo $venname
#echo $prodname
#echo $bluetoothCap
#echo $ethernetCap
#echo $wirelessCap
