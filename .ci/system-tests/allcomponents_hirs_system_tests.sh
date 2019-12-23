#!/bin/bash
### NOTE: This file will be moved into the PACCOR project, in the "scripts" directory.
###       It's here for review, until I get permissions to the PACCOR project

### User customizable values
APP_HOME="`dirname "$0"`"
COMPONENTS_URI="" # Specify the optional components URI field
COMPONENTS_URI_LOCAL_COPY_FOR_HASH="" # If empty, the optional hashAlgorithm and hashValue fields will not be included for the URI
PROPERTIES_URI="" # Specify the optional properties URI field
PROPERTIES_URI_LOCAL_COPY_FOR_HASH="" # If empty, the optional hashAlgorithm and hashValue fields will not be included for the URI
ENTERPRISE_NUMBERS_FILE="$APP_HOME""/enterprise-numbers"
PEN_ROOT="1.3.6.1.4.1." # OID root for the private enterprise numbers
SMBIOS_SCRIPT="$APP_HOME""/smbios.sh"
HW_SCRIPT="$APP_HOME""/hw.sh" # For components not covered by SMBIOS

### SMBIOS Type Constants
source $SMBIOS_SCRIPT
SMBIOS_TYPE_PLATFORM="1"
SMBIOS_TYPE_CHASSIS="3"
SMBIOS_TYPE_BIOS="0"
SMBIOS_TYPE_BASEBOARD="2"
SMBIOS_TYPE_CPU="4"
SMBIOS_TYPE_RAM="17"

### hw
source $HW_SCRIPT

### ComponentClass values
COMPCLASS_REGISTRY_TCG="2.23.133.18.3.1" # switch off values within SMBIOS to reveal accurate component classes
COMPCLASS_BASEBOARD="00030003" # these values are meant to be an example.  check the component class registry.
COMPCLASS_BIOS="00130003"
COMPCLASS_UEFI="00130002"
COMPCLASS_CHASSIS="00020001" # TODO:  chassis type is included in SMBIOS
COMPCLASS_CPU="00010002"
COMPCLASS_HDD="00070002"
COMPCLASS_NIC="00090002"
COMPCLASS_RAM="00060001"  # TODO: memory type is included in SMBIOS
COMPCLASS_GFX="00050002"

### JSON Structure Keywords
JSON_COMPONENTS="COMPONENTS"
JSON_COMPONENTSURI="COMPONENTSURI"
JSON_PROPERTIES="PROPERTIES"
JSON_PROPERTIESURI="PROPERTIESURI"
JSON_PLATFORM="PLATFORM"
#### JSON Component Keywords
JSON_COMPONENTCLASS="COMPONENTCLASS"
JSON_COMPONENTCLASSREGISTRY="COMPONENTCLASSREGISTRY"
JSON_COMPONENTCLASSVALUE="COMPONENTCLASSVALUE"
JSON_MANUFACTURER="MANUFACTURER"
JSON_MODEL="MODEL"
JSON_SERIAL="SERIAL"
JSON_REVISION="REVISION"
JSON_MANUFACTURERID="MANUFACTURERID"
JSON_FIELDREPLACEABLE="FIELDREPLACEABLE"
JSON_ADDRESSES="ADDRESSES"
JSON_ETHERNETMAC="ETHERNETMAC"
JSON_WLANMAC="WLANMAC"
JSON_BLUETOOTHMAC="BLUETOOTHMAC"
JSON_COMPONENTPLATFORMCERT="PLATFORMCERT"
JSON_ATTRIBUTECERTIDENTIFIER="ATTRIBUTECERTIDENTIFIER"
JSON_GENERICCERTIDENTIFIER="GENERICCERTIDENTIFIER"
JSON_ISSUER="ISSUER"
JSON_COMPONENTPLATFORMCERTURI="PLATFORMCERTURI"
JSON_STATUS="STATUS"
#### JSON Platform Keywords (Subject Alternative Name)
JSON_PLATFORMMODEL="PLATFORMMODEL"
JSON_PLATFORMMANUFACTURERSTR="PLATFORMMANUFACTURERSTR"
JSON_PLATFORMVERSION="PLATFORMVERSION"
JSON_PLATFORMSERIAL="PLATFORMSERIAL"
JSON_PLATFORMMANUFACTURERID="PLATFORMMANUFACTURERID"
#### JSON Platform URI Keywords
JSON_URI="UNIFORMRESOURCEIDENTIFIER"
JSON_HASHALG="HASHALGORITHM"
JSON_HASHVALUE="HASHVALUE"
#### JSON Properties Keywords
JSON_NAME="NAME"
JSON_VALUE="VALUE"
NOT_SPECIFIED="Not Specified"
CHASSIS_SERIAL_NUMBER="111111"
BASEBOARD_SERIAL_NUMBER="222222"
BIOS_SERIAL_NUMBER="333333"
PARSE_CPU_DATA_SERIAL_NUMBER="111222"
PARSE_RAM_DATA_SERIAL_NUMBER="222333"
PARSE_NIC_DATA_SERIAL_NUMBER="333444"
PARSE_HDD_DATA_SERIAL_NUMBER="444555"
PARSE_GFX_DATA_SERIAL_NUMBER="555666"

### JSON Structure Format
JSON_INTERMEDIATE_FILE_OBJECT='{
    %s
}'
JSON_PLATFORM_TEMPLATE='
    \"'"$JSON_PLATFORM"'\": {
        %s
    }'
JSON_PROPERTIESURI_TEMPLATE='
    \"'"$JSON_PROPERTIESURI"'\": {
        %s
    }'
JSON_COMPONENTSURI_TEMPLATE='
    \"'"$JSON_COMPONENTSURI"'\": {
        %s
    }'
JSON_PROPERTY_ARRAY_TEMPLATE='
    \"'"$JSON_PROPERTIES"'\": [%s
    ]'
JSON_COMPONENT_ARRAY_TEMPLATE='
    \"'"$JSON_COMPONENTS"'\": [%s
    ]'
JSON_COMPONENT_TEMPLATE='
        {
            %s
        }'
JSON_PROPERTY_TEMPLATE='
        {
            \"'"$JSON_NAME"'\": \"%s\",
            \"'"$JSON_VALUE"'\": \"%s\"
        }
'
JSON_ADDRESSES_TEMPLATE=' \"'"$JSON_ADDRESSES"'\": [%s]'
JSON_ETHERNETMAC_TEMPLATE=' {
                \"'"$JSON_ETHERNETMAC"'\": \"%s\" } '
JSON_WLANMAC_TEMPLATE=' {
                \"'"$JSON_WLANMAC"'\": \"%s\" } '
JSON_BLUETOOTHMAC_TEMPLATE=' {
                \"'"$JSON_BLUETOOTHMAC"'\": \"%s\" } '
JSON_COMPONENTCLASS_TEMPLATE=' \"'"$JSON_COMPONENTCLASS"'\": {
        \"'"$JSON_COMPONENTCLASSREGISTRY"'\": \"%s\",
        \"'"$JSON_COMPONENTCLASSVALUE"'\": \"%s\"
    }'
JSON_ATTRIBUTECERTIDENTIFIER_TEMPLATE=' \"'"$JSON_ATTRIBUTECERTIDENTIFIER"'\": {
        \"'"$JSON_HASHALG"'\": \"%s\",
        \"'"$JSON_HASHVALUE"'\": \"%s\"
    },'
JSON_GENERICCERTIDENTIFIER_TEMPLATE=' \"'"$JSON_GENERICCERTIDENTIFIER"'\": {
        \"'"$JSON_ISSUER"'\": \"%s\",
        \"'"$JSON_SERIAL"'\": \"%s\"
    },'
JSON_COMPONENTPLATFORMCERT_TEMPLATE='
    \"'"$JSON_COMPONENTPLATFORMCERT"'\": {
        %s
    }'
JSON_COMPONENTPLATFORMCERTURI_TEMPLATE='
    \"'"$JSON_COMPONENTPLATFORMCERTURI"'\": {
        %s
    }'
JSON_STATUS_TEMPLATE='
    \"'"$JSON_STATUS"'\": {

    }'

### JSON Constructor Aides
jsonComponentClass () {
    printf "$JSON_COMPONENTCLASS_TEMPLATE" "${1}" "${2}"
}
jsonManufacturer () {
    manufacturer=$(printf '\"'"$JSON_MANUFACTURER"'\": \"%s\"' "${1}")
    #tmpManufacturerId=$(queryForPen "${1}")
    #if [ -n "$tmpManufacturerId" ] && [ "$tmpManufacturerId" != "$PEN_ROOT" ]; then
    #    tmpManufacturerId=$(jsonManufacturerId "$tmpManufacturerId")
    #    manufacturer="$manufacturer"",""$tmpManufacturerId"
    #fi
    printf "$manufacturer"
}
jsonModel () {
    printf '\"'"$JSON_MODEL"'\": \"%s\"' "${1}"
}
jsonSerial () {
    printf '\"'"$JSON_SERIAL"'\": \"%s\"' "${1}"
}
jsonRevision () {
    printf '\"'"$JSON_REVISION"'\": \"%s\"' "${1}"
}
jsonManufacturerId () {
    printf '\"'"$JSON_MANUFACTURERID"'\": \"%s\"' "${1}"
}
jsonFieldReplaceable () {
    printf '\"'"$JSON_FIELDREPLACEABLE"'\": \"%s\"' "${1}"
}
jsonEthernetMac () {
    printf "$JSON_ETHERNETMAC_TEMPLATE" "${1}"
}
jsonWlanMac () {
    printf "$JSON_WLANMAC_TEMPLATE" "${1}"
}
jsonBluetoothMac () {
    printf "$JSON_BLUETOOTHMAC_TEMPLATE" "${1}"
}
jsonPlatformModel () {
    printf '\"'"$JSON_PLATFORMMODEL"'\": \"%s\"' "${1}"
}
jsonPlatformManufacturerStr () {
    manufacturer=$(printf '\"'"$JSON_PLATFORMMANUFACTURERSTR"'\": \"%s\"' "${1}")
    #tmpManufacturerId=$(queryForPen "${1}")
    #if [ -n "$tmpManufacturerId" ] && [ "$tmpManufacturerId" != "$PEN_ROOT" ]; then
    #    tmpManufacturerId=$(jsonPlatformManufacturerId "$tmpManufacturerId")
    #    manufacturer="$manufacturer"",""$tmpManufacturerId"
    #fi
    printf "$manufacturer"
}
jsonPlatformVersion () {
    printf '\"'"$JSON_PLATFORMVERSION"'\": \"%s\"' "${1}"
}
jsonPlatformSerial () {
    printf '\"'"$JSON_PLATFORMSERIAL"'\": \"%s\"' "${1}"
}
jsonPlatformManufacturerId () {
    printf '\"'"$JSON_PLATFORMMANUFACTURERID"'\": \"%s\"' "${1}"
}
queryForPen () {
    pen=$(grep -B 1 "^[ \t]*""${1}""$" "$ENTERPRISE_NUMBERS_FILE" | sed -n '1p' | tr -d [:space:])
    printf "%s%s" "$PEN_ROOT" "$pen"
}
jsonProperty () {
    if [ -n "${1}" ] && [ -n "${2}" ]; then
        if [ -n "${3}" ]; then
            printf "$JSON_PROPERTY_TEMPLATE" "${1}" "${2}" "${3}"
        else
            printf "$JSON_PROPERTY_TEMPLATE" "${1}" "${2}"
        fi
    fi
}
jsonUri () {
    printf '\"'"$JSON_URI"'\": \"%s\"' "${1}"
}
jsonHashAlg () {
    printf '\"'"$JSON_HASHALG"'\": \"%s\"' "${1}"
}
jsonHashValue () {
    printf '\"'"$JSON_HASHVALUE"'\": \"%s\"' "${1}"
}
toCSV () {
    old="$IFS"
    IFS=','
    value="$*"
    value=$(printf "$value" | tr -s , | sed -e '1s/^[,]*//' | sed -e '$s/[,]*$//')
    printf "$value"
}
jsonAddress () {
    printf "$JSON_ADDRESSES_TEMPLATE" "$(toCSV "$@")"
}
jsonComponent () {
    printf "$JSON_COMPONENT_TEMPLATE" "$(toCSV "$@")"
}
jsonComponentArray () {
    printf "$JSON_COMPONENT_ARRAY_TEMPLATE" "$(toCSV "$@")"
}
jsonPropertyArray () {
    if [ "$#" -ne 0 ]; then
        printf "$JSON_PROPERTY_ARRAY_TEMPLATE" "$(toCSV "$@")"
    fi
}
jsonPlatformObject () {
    printf "$JSON_PLATFORM_TEMPLATE" "$(toCSV "$@")"
}
jsonComponentsUri () {
    if [ -n "$COMPONENTS_URI" ]; then
        componentsUri=$(jsonUri "$COMPONENTS_URI")
        componentsUriDetails=""
        if [ -n "$PROPERTIES_URI_LOCAL_COPY_FOR_HASH" ]; then
            hashAlg="2.16.840.1.101.3.4.2.1" # SHA256, see https://tools.ietf.org/html/rfc5754 for other common hash algorithm IDs
            hashValue=$(sha256sum "$COMPONENTS_URI_LOCAL_COPY_FOR_HASH" | sed -r 's/^([0-9a-f]+).*/\1/' | tr -d [:space:] | xxd -r -p | base64 -w 0)
            hashAlgStr=$(jsonHashAlg "$hashAlg")
            hashValueStr=$(jsonHashValue "$hashValue")
            propertiesUriDetails="$hashAlgStr"",""$hashValueStr"
        fi
    printf "$JSON_COMPONENTSURI_TEMPLATE" "$(toCSV "$componentsUri" "$componentsUriDetails")"
    fi
}
jsonPropertiesUri () {
    if [ -n "$PROPERTIES_URI" ]; then
        propertiesUri=$(jsonUri "$PROPERTIES_URI")
        propertiesUriDetails=""
        if [ -n "$PROPERTIES_URI_LOCAL_COPY_FOR_HASH" ]; then
            hashAlg="2.16.840.1.101.3.4.2.1" # SHA256, see https://tools.ietf.org/html/rfc5754 for other common hash algorithm IDs
            hashValue=$(sha256sum "$PROPERTIES_URI_LOCAL_COPY_FOR_HASH" | sed -r 's/^([0-9a-f]+).*/\1/' | tr -d [:space:] | xxd -r -p | base64 -w 0)
            hashAlgStr=$(jsonHashAlg "$hashAlg")
            hashValueStr=$(jsonHashValue "$hashValue")
            propertiesUriDetails="$hashAlgStr"",""$hashValueStr"
        fi| sed 's/^[ \t]*//;s/[ \t]*$//'
    printf "$JSON_PROPERTIESURI_TEMPLATE" "$(toCSV "$propertiesUri" "$propertiesUriDetails")"
    fi
}
jsonIntermediateFile () {
    printf "$JSON_INTERMEDIATE_FILE_OBJECT" "$(toCSV "$@")"
}
standardizeMACAddr () {
    mac=$(printf "${1}" | tr -d "[[:space:]]:-" | awk '{ print toupper($0) }')
    printf "$mac"
}



## Some of the commands below require root.
if [ "$EUID" -ne 0 ]
  then echo "Please run as root"
  exit
fi

### Gather platform details for the subject alternative name
dmidecodeParseTypeAssumeOneHandle "$SMBIOS_TYPE_PLATFORM"
platformManufacturer=$(dmidecodeGetString $(dmidecodeGetByte "0x4"))
platformModel=$(dmidecodeGetString $(dmidecodeGetByte "0x5"))
platformVersion=$(dmidecodeGetString $(dmidecodeGetByte "0x6"))
platformSerial=$(dmidecodeGetString $(dmidecodeGetByte "0x7"))

if [[ -z "${platformManufacturer// }" ]]; then
    platformManufacturer="$NOT_SPECIFIED"
fi
platformManufacturer=$(echo "$platformManufacturer" | sed 's/^[ \t]*//;s/[ \t]*$//')
platformManufacturer=$(jsonPlatformManufacturerStr "$platformManufacturer")

if [[ -z "${platformModel// }" ]]; then
    platformModel="$NOT_SPECIFIED"
fi
platformModel=$(echo "$platformModel" | sed 's/^[ \t]*//;s/[ \t]*$//')
platformModel=$(jsonPlatformModel "$platformModel")

if [[ -z "${platformVersion// }" ]]; then
    platformVersion="$NOT_SPECIFIED"
fi
platformVersion=$(echo "$platformVersion" | sed 's/^[ \t]*//;s/[ \t]*$//')
platformVersion=$(jsonPlatformVersion "$platformVersion")

if ! [[ -z "${platformSerial// }" ]]; then
    platformSerial=$(echo "$platformSerial" | sed 's/^[ \t]*//;s/[ \t]*$//')
    platformSerial=$(jsonPlatformSerial "$platformSerial")
fi
platform=$(jsonPlatformObject "$platformManufacturer" "$platformModel" "$platformVersion" "$platformSerial")



### Gather component details
dmidecodeParseTypeAssumeOneHandle "$SMBIOS_TYPE_CHASSIS"
chassisClass=$(jsonComponentClass "$COMPCLASS_REGISTRY_TCG" "$COMPCLASS_CHASSIS")
chassisManufacturer=$(dmidecodeGetString $(dmidecodeGetByte "0x4"))
chassisModel=$(dmidecodeGetByte "0x5")
chassisModel=$(printf "%d" "0x""$chassisModel") # Convert to decimal
chassisSerial=$(dmidecodeGetString $(dmidecodeGetByte "0x7"))
chassisRevision=$(dmidecodeGetString $(dmidecodeGetByte "0x6"))

if [[ -z "${chassisManufacturer// }" ]]; then
    chassisManufacturer="$NOT_SPECIFIED"
fi
chassisManufacturer=$(echo "$chassisManufacturer" | sed 's/^[ \t]*//;s/[ \t]*$//')
chassisManufacturer=$(jsonManufacturer "$chassisManufacturer")

if [[ -z "${chassisModel// }" ]]; then
    chassisModel="$NOT_SPECIFIED"
fi
chassisModel=$(echo "$chassisModel" | sed 's/^[ \t]*//;s/[ \t]*$//')
chassisModel=$(jsonModel "$chassisModel")

chassisOptional=""
if ! [[ -z "${chassisSerial// }" ]]; then
    chassisSerial=$(echo "$chassisSerial" | sed 's/^[ \t]*//;s/[ \t]*$//')
    chassisSerial=$(jsonSerial "$chassisSerial")
    chassisOptional="$chassisOptional"",""$chassisSerial"
fi
if ! [[ -z "${chassisRevision// }" ]]; then
    chassisRevision=$(echo "$chassisRevision" | sed 's/^[ \t]*//;s/[ \t]*$//')
    chassisRevision=$(jsonRevision "$chassisRevision")
    chassisOptional="$chassisOptional"",""$chassisRevision"
fi
chassisOptional=$(printf "$chassisOptional" | cut -c2-)
# Use default SN#
if [[ -z "${chassisOptional// }" ]]; then
	chassisSerial=$(jsonSerial "$CHASSIS_SERIAL_NUMBER")
    chassisOptional="$chassisOptional"",""$chassisSerial"
fi

componentChassis=$(jsonComponent "$chassisClass" "$chassisManufacturer" "$chassisModel" "$chassisOptional")

### Gather baseboard details
dmidecodeParseTypeAssumeOneHandle "$SMBIOS_TYPE_BASEBOARD"
baseboardClass=$(jsonComponentClass "$COMPCLASS_REGISTRY_TCG" "$COMPCLASS_BASEBOARD")
baseboardManufacturer=$(dmidecodeGetString $(dmidecodeGetByte "0x4"))
baseboardModel=$(dmidecodeGetString $(dmidecodeGetByte "0x5"))
baseboardSerial=$(dmidecodeGetString $(dmidecodeGetByte "0x7"))
baseboardRevision=$(dmidecodeGetString $(dmidecodeGetByte "0x6"))
baseboardFeatureFlags=$(dmidecodeGetByte "0x9")
baseboardFeatureFlags=$(printf "%d" "0x""$baseboardFeatureFlags") # Convert to decimal
baseboardReplaceableIndicator="28"
baseboardFieldReplaceableAnswer="false"
if (((baseboardFeatureFlags&baseboardReplaceableIndicator)!=0)); then
    baseboardFieldReplaceableAnswer="true"
fi
baseboardFieldReplaceable=$(jsonFieldReplaceable "$baseboardFieldReplaceableAnswer")

if [[ -z "${baseboardManufacturer// }" ]]; then
    baseboardManufacturer="$NOT_SPECIFIED"
fi
baseboardManufacturer=$(echo "$baseboardManufacturer" | sed 's/^[ \t]*//;s/[ \t]*$//')
baseboardManufacturer=$(jsonManufacturer "$baseboardManufacturer")

if [[ -z "${baseboardModel// }" ]]; then
    baseboardModel="$NOT_SPECIFIED"
fi
baseboardModel=$(echo "$baseboardModel" | sed 's/^[ \t]*//;s/[ \t]*$//')
baseboardModel=$(jsonModel "$baseboardModel")

baseboardOptional=""
if ! [[ -z "${baseboardSerial// }" ]]; then
    baseboardSerial=$(echo "$baseboardSerial" | sed 's/^[ \t]*//;s/[ \t]*$//')
    baseboardSerial=$(jsonSerial "$baseboardSerial")
    baseboardOptional="$baseboardOptional"",""$baseboardSerial"
fi
if ! [[ -z "${baseboardRevision// }" ]]; then
    baseboardRevision=$(echo "$baseboardRevision" | sed 's/^[ \t]*//;s/[ \t]*$//')
    baseboardRevision=$(jsonRevision "$baseboardRevision")
    baseboardOptional="$baseboardOptional"",""$baseboardRevision"
fi
baseboardOptional=$(printf "$baseboardOptional" | cut -c2-)
# Use default SN#
if [[ -z "${baseboardOptional// }" ]]; then
	baseboardSerial=$(jsonSerial "$BASEBOARD_SERIAL_NUMBER")
    baseboardOptional="$baseboardOptional"",""$baseboardSerial"
fi

componentBaseboard=$(jsonComponent "$baseboardClass" "$baseboardManufacturer" "$baseboardModel" "$baseboardFieldReplaceable" "$baseboardOptional")

### Gather BIOS details
dmidecodeParseTypeAssumeOneHandle "$SMBIOS_TYPE_BIOS"
biosClass=$(jsonComponentClass "$COMPCLASS_REGISTRY_TCG" "$COMPCLASS_BIOS")
biosManufacturer=$(dmidecodeGetString $(dmidecodeGetByte "0x4"))
biosModel=""
biosSerial=""
biosRevision=$(dmidecodeGetString $(dmidecodeGetByte "0x5"))

if [[ -z "${biosManufacturer// }" ]]; then
    biosManufacturer="$NOT_SPECIFIED"
fi
biosManufacturer=$(echo "$biosManufacturer" | sed 's/^[ \t]*//;s/[ \t]*$//')
biosManufacturer=$(jsonManufacturer "$biosManufacturer")

if [[ -z "${biosModel// }" ]]; then
    biosModel="$NOT_SPECIFIED"
fi
biosModel=$(echo "$biosModel" | sed 's/^[ \t]*//;s/[ \t]*$//')
biosModel=$(jsonModel "$biosModel")

biosOptional=""
if ! [[ -z "${biosSerial// }" ]]; then
    biosSerial=$(echo "$biosSerial" | sed 's/^[ \t]*//;s/[ \t]*$//')
    biosSerial=$(jsonSerial "$biosSerial")
    biosOptional="$biosOptional"",""$biosSerial"
fi
if ! [[ -z "${biosRevision// }" ]]; then
    biosRevision=$(echo "$biosRevision" | sed 's/^[ \t]*//;s/[ \t]*$//')
    biosRevision=$(jsonRevision "$biosRevision")
    biosOptional="$biosOptional"",""$biosRevision"
fi
biosOptional=$(printf "$biosOptional" | cut -c2-)
# Use default SN#
if [[ -z "${biosOptional// }" ]]; then
	biosSerial=$(jsonSerial "$BIOS_SERIAL_NUMBER")
    biosOptional="$biosOptional"",""$biosSerial"
fi

componentBios=$(jsonComponent "$biosClass" "$biosManufacturer" "$biosModel" "$biosOptional")

parseCpuData () {

    dmidecodeHandles "$SMBIOS_TYPE_CPU"

    notReplaceableIndicator="6"
    tmpData=""
    numHandles=$(dmidecodeNumHandles)
    class=$(jsonComponentClass "$COMPCLASS_REGISTRY_TCG" "$COMPCLASS_CPU")

    for ((i = 0 ; i < numHandles ; i++ )); do
        dmidecodeParseHandle "${tableHandles[$i]}"

    manufacturer=$(dmidecodeGetString $(dmidecodeGetByte "0x7"))
    model=$(dmidecodeGetByte "0x6")
        model=$(printf "%d" "0x""$model") # Convert to decimal
    serial=$(dmidecodeGetString $(dmidecodeGetByte "0x20"))
    revision=$(dmidecodeGetString $(dmidecodeGetByte "0x10"))
        processorUpgrade=$(dmidecodeGetByte "0x19")
        processorUpgrade=$(printf "%d" "0x""$processorUpgrade") # Convert to decimal

    if [[ -z "${manufacturer// }" ]]; then
        manufacturer="$NOT_SPECIFIED"
    fi
    manufacturer=$(echo "$manufacturer" | sed 's/^[ \t]*//;s/[ \t]*$//')
    manufacturer=$(jsonManufacturer "$manufacturer")

    if [[ -z "${model// }" ]]; then
        model="$NOT_SPECIFIED"
    fi
    model=$(echo "$model" | sed 's/^[ \t]*//;s/[ \t]*$//')
    model=$(jsonModel "$model")

    optional=""
    if ! [[ -z "${serial// }" ]]; then
        serial=$(echo "$serial" | sed 's/^[ \t]*//;s/[ \t]*$//')
        serial=$(jsonSerial "$serial")
        optional="$optional"",""$serial"
    fi
    if ! [[ -z "${revision// }" ]]; then
        revision=$(echo "$revision" | sed 's/^[ \t]*//;s/[ \t]*$//')
        revision=$(jsonRevision "$revision")
        optional="$optional"",""$revision"
    fi
    optional=$(printf "$optional" | cut -c2-)
	# Use default SN#
	if [[ -z "${optional// }" ]]; then
		serial=$(jsonSerial "$PARSE_CPU_DATA_SERIAL_NUMBER")
	    optional="$optional"",""$serial"
	    PARSE_CPU_DATA_SERIAL_NUMBER=$((PARSE_CPU_DATA_SERIAL_NUMBER + 1))
	fi

        replaceable="true"
        if [ $processorUpgrade -eq $notReplaceableIndicator ]; then
            replaceable="false"
        fi
        replaceable=$(jsonFieldReplaceable "$replaceable")

        newCpuData=$(jsonComponent "$class" "$manufacturer" "$model" "$replaceable" "$optional")
        tmpData="$tmpData"",""$newCpuData"
    done

    # remove leading comma
    tmpData=$(printf "$tmpData" | cut -c2-)

    printf "$tmpData"
}

parseRamData () {
    dmidecodeHandles "$SMBIOS_TYPE_RAM"

    replaceable=$(jsonFieldReplaceable "true")
    tmpData=""
    numHandles=$(dmidecodeNumHandles)
    class=$(jsonComponentClass "$COMPCLASS_REGISTRY_TCG" "$COMPCLASS_RAM")

    for ((i = 0 ; i < numHandles ; i++ )); do
        dmidecodeParseHandle "${tableHandles[$i]}"

    manufacturer=$(dmidecodeGetString $(dmidecodeGetByte "0x17"))
    model=$(dmidecodeGetString $(dmidecodeGetByte "0x1A"))
    serial=$(dmidecodeGetString $(dmidecodeGetByte "0x18"))
    revision=$(dmidecodeGetString $(dmidecodeGetByte "0x19"))

        if ([[ -z "${manufacturer// }" ]] && [[ -z "${model// }" ]] && [[ -z "${serial// }" ]] && [[ -z "${revision// }" ]]); then
            continue
        fi

    if [[ -z "${manufacturer// }" ]]; then
        manufacturer="$NOT_SPECIFIED"
    fi
    manufacturer=$(echo "$manufacturer" | sed 's/^[ \t]*//;s/[ \t]*$//')
    manufacturer=$(jsonManufacturer "$manufacturer")

    if [[ -z "${model// }" ]]; then
        model="$NOT_SPECIFIED"
    fi
    model=$(echo "$model" | sed 's/^[ \t]*//;s/[ \t]*$//')
    model=$(jsonModel "$model")

    optional=""
    if ! [[ -z "${serial// }" ]]; then
        serial=$(echo "$serial" | sed 's/^[ \t]*//;s/[ \t]*$//')
        serial=$(jsonSerial "$serial")
        optional="$optional"",""$serial"
    fi
    if ! [[ -z "${revision// }" ]]; then
        revision=$(echo "$revision" | sed 's/^[ \t]*//;s/[ \t]*$//')
        revision=$(jsonRevision "$revision")
        optional="$optional"",""$revision"
    fi
    optional=$(printf "$optional" | cut -c2-)
	# Use default SN#
	if [[ -z "${optional// }" ]]; then
		serial=$(jsonSerial "$PARSE_RAM_DATA_SERIAL_NUMBER")
	    optional="$optional"",""$serial"
	    PARSE_RAM_DATA_SERIAL_NUMBER=$((PARSE_RAM_DATA_SERIAL_NUMBER + 1))
	fi

        newRamData=$(jsonComponent "$class" "$manufacturer" "$model" "$replaceable" "$optional")
        tmpData="$tmpData"",""$newRamData"
    done

    # remove leading comma
    tmpData=$(printf "$tmpData" | cut -c2-)

    printf "$tmpData"
}

# Write script to parse multiple responses
# Network:
# lshw description: type of address.
#                 : Ethernet interface, Wireless interface, Bluetooth wireless interface
#           vendor: manufacturer
#          product: model
#           serial: address & serial number
#          version: revision
#
# Example:
# ADDRESS1=$(jsonEthernetMac "AB:CD:EE:EE:DE:34")
# ADDR_LIST=$(jsonAddress "$ADDRESS1" "$ADDRESS2")
parseNicData () {
    lshwNetwork

    replaceable=$(jsonFieldReplaceable "true")
    tmpData=""
    numHandles=$(lshwNumBusItems)
    class=$(jsonComponentClass "$COMPCLASS_REGISTRY_TCG" "$COMPCLASS_NIC")

    for ((i = 0 ; i < numHandles ; i++ )); do
        manufacturer=$(lshwGetVendorIDFromBusItem "$i")
    model=$(lshwGetProductIDFromBusItem "$i")
    serialConstant=$(lshwGetSerialFromBusItem "$i")
        serialConstant=$(standardizeMACAddr "${serialConstant}")
        serial=""
    revision=$(lshwGetVersionFromBusItem "$i")

        if [[ -z "${manufacturer// }" ]] && [[ -z "${model// }" ]] && (! [[ -z "${serialConstant// }" ]] || ! [[ -z "${revision// }" ]]); then
            manufacturer=$(lshwGetVendorNameFromBusItem "$i")
        model=$(lshwGetProductNameFromBusItem "$i")
        fi

    if [[ -z "${manufacturer// }" ]]; then
        manufacturer="$NOT_SPECIFIED"
    fi
    manufacturer=$(echo "$manufacturer" | sed 's/^[ \t]*//;s/[ \t]*$//')
    manufacturer=$(jsonManufacturer "$manufacturer")

    if [[ -z "${model// }" ]]; then
        model="$NOT_SPECIFIED"
    fi
    model=$(echo "$model" | sed 's/^[ \t]*//;s/[ \t]*$//')
    model=$(jsonModel "$model")

    optional=""
    if ! [[ -z "${serialConstant// }" ]]; then
        serial=$(echo "$serialConstant" | sed 's/^[ \t]*//;s/[ \t]*$//')
        serial=$(jsonSerial "$serialConstant")
        optional="$optional"",""$serial"
    fi
    if ! [[ -z "${revision// }" ]]; then
        revision=$(echo "$revision" | sed 's/^[ \t]*//;s/[ \t]*$//' | awk '{ print toupper($0) }')
        revision=$(jsonRevision "$revision")
        optional="$optional"",""$revision"
    fi
        bluetoothCap=$(lshwBusItemBluetoothCap "$i")
        ethernetCap=$(lshwBusItemEthernetCap "$i")
        wirelessCap=$(lshwBusItemWirelessCap "$i")

        if ([ -n "$bluetoothCap" ] || [ -n "$ethernetCap" ] || [ -n "$wirelessCap" ]) && ! [[ -z "${serialConstant// }" ]]; then
            thisAddress=
            if [ -n "$wirelessCap" ]; then
                thisAddress=$(jsonWlanMac "$serialConstant")
            elif [ -n "$bluetoothCap" ]; then
                thisAddress=$(jsonBluetoothMac "$serialConstant")
            elif [ -n "$ethernetCap" ]; then
                thisAddress=$(jsonEthernetMac "$serialConstant")
            fi
            if [ -n "$thisAddress" ]; then
                thisAddress=$(jsonAddress "$thisAddress")
                optional="$optional"",""$thisAddress"
            fi
        fi
    optional=$(printf "$optional" | cut -c2-)
	# Use default SN#
	if [[ -z "${optional// }" ]]; then
		serial=$(jsonSerial "$PARSE_NIC_DATA_SERIAL_NUMBER")
	    optional="$optional"",""$serial"
	    PARSE_NIC_DATA_SERIAL_NUMBER=$((PARSE_NIC_DATA_SERIAL_NUMBER + 1))
	fi

        newNicData=$(jsonComponent "$class" "$manufacturer" "$model" "$replaceable" "$optional")
        tmpData="$tmpData"",""$newNicData"
    done

    # remove leading comma
    tmpData=$(printf "$tmpData" | cut -c2-)

    printf "$tmpData"
}

parseHddData () {
    lshwDisk

    replaceable=$(jsonFieldReplaceable "true")
    tmpData=""
    numHandles=$(lshwNumBusItems)
    class=$(jsonComponentClass "$COMPCLASS_REGISTRY_TCG" "$COMPCLASS_HDD")

    for ((i = 0 ; i < numHandles ; i++ )); do
        manufacturer=$(lshwGetVendorIDFromBusItem "$i")
    model=$(lshwGetProductIDFromBusItem "$i")
    serial=$(lshwGetSerialFromBusItem "$i")
    revision=$(lshwGetVersionFromBusItem "$i")

        if [[ -z "${manufacturer// }" ]] && [[ -z "${model// }" ]] && (! [[ -z "${serial// }" ]] || ! [[ -z "${revision// }" ]]); then
            model=$(lshwGetProductNameFromBusItem "$i")
            manufacturer=""
            revision="" # Seeing inconsistent behavior cross-OS for this case, will return
        fi

    if [[ -z "${manufacturer// }" ]]; then
        manufacturer="$NOT_SPECIFIED"
    fi
    manufacturer=$(echo "$manufacturer" | sed 's/^[ \t]*//;s/[ \t]*$//')
    manufacturer=$(jsonManufacturer "$manufacturer")

    if [[ -z "${model// }" ]]; then
        model="$NOT_SPECIFIED"
    fi
    model=$(echo "$model" | sed 's/^[ \t]*//;s/[ \t]*$//')
    model=$(jsonModel "$model")

    optional=""
    if ! [[ -z "${serial// }" ]]; then
        serial=$(echo "$serial" | sed 's/^[ \t]*//;s/[ \t]*$//')
        serial=$(jsonSerial "$serial")
        optional="$optional"",""$serial"
    fi
    if ! [[ -z "${revision// }" ]]; then
        revision=$(echo "$revision" | sed 's/^[ \t]*//;s/[ \t]*$//' | awk '{ print toupper($0) }')
        revision=$(jsonRevision "$revision")
        optional="$optional"",""$revision"
    fi
    optional=$(printf "$optional" | cut -c2-)
	# Use default SN#
	if [[ -z "${optional// }" ]]; then
		serial=$(jsonSerial "PARSE_HDD_DATA_SERIAL_NUMBER")
	    optional="$optional"",""$serial"
	    PARSE_HDD_DATA_SERIAL_NUMBER=$((PARSE_HDD_DATA_SERIAL_NUMBER + 1))
	fi

        newHddData=$(jsonComponent "$class" "$manufacturer" "$model" "$replaceable" "$optional")
        tmpData="$tmpData"",""$newHddData"
    done

    # remove leading comma
    tmpData=$(printf "$tmpData" | cut -c2-)

    printf "$tmpData"
}

parseGfxData () {
    lshwDisplay

    replaceable=$(jsonFieldReplaceable "true")
    tmpData=""
    numHandles=$(lshwNumBusItems)
    class=$(jsonComponentClass "$COMPCLASS_REGISTRY_TCG" "$COMPCLASS_GFX")

    for ((i = 0 ; i < numHandles ; i++ )); do
        manufacturer=$(lshwGetVendorIDFromBusItem "$i")
    model=$(lshwGetProductIDFromBusItem "$i")
    serial=$(lshwGetSerialFromBusItem "$i")
    revision=$(lshwGetVersionFromBusItem "$i")

        if [[ -z "${manufacturer// }" ]] && [[ -z "${model// }" ]] && (! [[ -z "${serial// }" ]] || ! [[ -z "${revision// }" ]]); then
            manufacturer=$(lshwGetVendorNameFromBusItem "$i")
        model=$(lshwGetProductNameFromBusItem "$i")
        fi

    if [[ -z "${manufacturer// }" ]]; then
        manufacturer="$NOT_SPECIFIED"
    fi
    manufacturer=$(echo "$manufacturer" | sed 's/^[ \t]*//;s/[ \t]*$//')
    manufacturer=$(jsonManufacturer "$manufacturer")

    if [[ -z "${model// }" ]]; then
        model="$NOT_SPECIFIED"
    fi
    model=$(echo "$model" | sed 's/^[ \t]*//;s/[ \t]*$//')
    model=$(jsonModel "$model")

    optional=""
    if ! [[ -z "${serial// }" ]]; then
        serial=$(echo "$serial" | sed 's/^[ \t]*//;s/[ \t]*$//')
        serial=$(jsonSerial "$serial")
        optional="$optional"",""$serial"
    fi
    if ! [[ -z "${revision// }" ]]; then
        revision=$(echo "$revision" | sed 's/^[ \t]*//;s/[ \t]*$//' | awk '{ print toupper($0) }')
        revision=$(jsonRevision "$revision")
        optional="$optional"",""$revision"
    fi
    optional=$(printf "$optional" | cut -c2-)
	# Use default SN#
	if [[ -z "${optional// }" ]]; then
		serial=$(jsonSerial "PARSE_GFX_DATA_SERIAL_NUMBER")
	    optional="$optional"",""$serial"
	    PARSE_GFX_DATA_SERIAL_NUMBER=$((PARSE_GFX_DATA_SERIAL_NUMBER + 1))
	fi

        newGfxData=$(jsonComponent "$class" "$manufacturer" "$model" "$replaceable" "$optional")
        tmpData="$tmpData"",""$newGfxData"
    done

    # remove leading comma
    tmpData=$(printf "$tmpData" | cut -c2-)

    printf "$tmpData"
}


### Gather property details
property1=$(jsonProperty "uname -r" "$(uname -r)")  ## Example1
property2=$(jsonProperty "OS Release" "$(grep 'PRETTY_NAME=' /etc/os-release | sed 's/[^=]*=//' | sed -e 's/^[[:space:]\"]*//' | sed -e 's/[[:space:]\"]*$//')") ## Example2

### Collate the component details
componentsCPU=$(parseCpuData)
componentsRAM=$(parseRamData)
componentsNIC=$(parseNicData)
componentsHDD=$(parseHddData)
componentsGFX=$(parseGfxData)
componentArray=$(jsonComponentArray "$componentChassis" "$componentBaseboard" "$componentBios" "$componentsCPU" "$componentsRAM" "$componentsNIC" "$componentsHDD" "$componentsGFX")

### Collate the property details
propertyArray=$(jsonPropertyArray "$property1" "$property2")

### Construct the final JSON object
FINAL_JSON_OBJECT=$(jsonIntermediateFile "$platform" "$componentArray" "$propertyArray")

### Collate the URI details, if parameters above are blank, the fields will be excluded from the final JSON structure
if [ -n "$COMPONENTS_URI" ]; then
    componentsUri=$(jsonComponentsUri)
    FINAL_JSON_OBJECT="$FINAL_JSON_OBJECT"",""$componentsUri"
fi
if [ -n "$PROPERTIES_URI" ]; then
    propertiesUri=$(jsonPropertiesUri)
    FINAL_JSON_OBJECT="$FINAL_JSON_OBJECT"",""$propertiesUri"
fi

printf "$FINAL_JSON_OBJECT""\n\n"


