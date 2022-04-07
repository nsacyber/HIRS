param(
    [parameter(Mandatory=$true)]
    [ValidateNotNull()]
    [string]$filename
)

### User customizable values
$APP_HOME=(Split-Path -parent $PSCommandPath)
$PROPERTIES_URI="" # Specify the optional properties URI field
$PROPERTIES_URI_LOCAL_COPY_FOR_HASH="" # If empty, the optional hashAlgorithm and hashValue fields will not be included for the URI
$ENTERPRISE_NUMBERS_FILE="$APP_HOME/../enterprise-numbers"
$PEN_ROOT="1.3.6.1.4.1." # OID root for the private enterprise numbers
$SMBIOS_SCRIPT="$APP_HOME/SMBios.ps1"
$HW_SCRIPT="$APP_HOME/hw.ps1" # For components not covered by SMBIOS
$NVME_SCRIPT="$APP_HOME/nvme.ps1" # For NVMe components

### Load Raw SMBios Data
. $SMBIOS_SCRIPT
$smbios=(Get-SMBiosStructures)
$SMBIOS_TYPE_PLATFORM="1"
$SMBIOS_TYPE_CHASSIS="3"
$SMBIOS_TYPE_BIOS="0"
$SMBIOS_TYPE_BASEBOARD="2"
$SMBIOS_TYPE_CPU="4"
$SMBIOS_TYPE_RAM="17"

### hw
. $HW_SCRIPT
### nvme
. $NVME_SCRIPT

### ComponentClass values
$COMPCLASS_REGISTRY_TCG="2.23.133.18.3.1" # switch off values within SMBIOS to reveal accurate component classes
$COMPCLASS_BASEBOARD="00030003" # these values are meant to be an example.  check the component class registry.
$COMPCLASS_BIOS="00130003"
$COMPCLASS_UEFI="00130002"
$COMPCLASS_CHASSIS="00020001" # TODO:  chassis type is included in SMBIOS
$COMPCLASS_CPU="00010002"
$COMPCLASS_HDD="00070002"
$COMPCLASS_NIC="00090002"
$COMPCLASS_RAM="00060001"  # TODO: memory type is included in SMBIOS
$COMPCLASS_GFX="00050002"

# Progress Group IDs:
#     1: Overall progress
#     2: Component type
#     3: Function progress per component
Write-Progress -Id 1 -Activity "Setting up to gather component details" -PercentComplete 0

### JSON Structure Keywords
$JSON_COMPONENTS="COMPONENTS"
$JSON_PROPERTIES="PROPERTIES"
$JSON_PROPERTIESURI="PROPERTIESURI"
$JSON_PLATFORM="PLATFORM"
#### JSON Component Keywords
$JSON_COMPONENTCLASS="COMPONENTCLASS"
$JSON_COMPONENTCLASSREGISTRY="COMPONENTCLASSREGISTRY"
$JSON_COMPONENTCLASSVALUE="COMPONENTCLASSVALUE"
$JSON_MANUFACTURER="MANUFACTURER"
$JSON_MODEL="MODEL"
$JSON_SERIAL="SERIAL"
$JSON_REVISION="REVISION"
$JSON_MANUFACTURERID="MANUFACTURERID"
$JSON_FIELDREPLACEABLE="FIELDREPLACEABLE"
$JSON_ADDRESSES="ADDRESSES"
$JSON_ETHERNETMAC="ETHERNETMAC"
$JSON_WLANMAC="WLANMAC"
$JSON_BLUETOOTHMAC="BLUETOOTHMAC"
$JSON_COMPONENTPLATFORMCERT="PLATFORMCERT"
$JSON_ATTRIBUTECERTIDENTIFIER="ATTRIBUTECERTIDENTIFIER"
$JSON_GENERICCERTIDENTIFIER="GENERICCERTIDENTIFIER"
$JSON_ISSUER="ISSUER"
$JSON_COMPONENTPLATFORMCERTURI="PLATFORMCERTURI"
$JSON_STATUS="STATUS"
#### JSON Platform Keywords (Subject Alternative Name)
$JSON_PLATFORMMODEL="PLATFORMMODEL"
$JSON_PLATFORMMANUFACTURERSTR="PLATFORMMANUFACTURERSTR"
$JSON_PLATFORMVERSION="PLATFORMVERSION"
$JSON_PLATFORMSERIAL="PLATFORMSERIAL"
$JSON_PLATFORMMANUFACTURERID="PLATFORMMANUFACTURERID"
#### JSON Platform URI Keywords
$JSON_URI="UNIFORMRESOURCEIDENTIFIER"
$JSON_HASHALG="HASHALGORITHM"
$JSON_HASHVALUE="HASHVALUE"
#### JSON Properties Keywords
$JSON_NAME="NAME"
$JSON_VALUE="VALUE"
$NOT_SPECIFIED="Not Specified"


### JSON Structure Format
$JSON_INTERMEDIATE_FILE_OBJECT="{{
    {0}
}}"
$JSON_PLATFORM_TEMPLATE="
    `"$JSON_PLATFORM`": {{
        {0}
    }}"
$JSON_PROPERTIESURI_TEMPLATE="
    `"$JSON_PROPERTIESURI`": {{
        {0}
    }}"
$JSON_COMPONENTSURI_TEMPLATE="
    `"$JSON_COMPONENTSURI`": {{
        {0}
    }}"
$JSON_PROPERTY_ARRAY_TEMPLATE="
    `"$JSON_PROPERTIES`": [{0}
    ]"
$JSON_COMPONENT_ARRAY_TEMPLATE="
    `"$JSON_COMPONENTS`": [{0}
    ]"
$JSON_COMPONENT_TEMPLATE="
        {{
            {0}
        }}"
$JSON_PROPERTY_TEMPLATE="
        {{
            `"$JSON_NAME`": `"{0}`",
            `"$JSON_VALUE`": `"{1}`"
        }}
"
$JSON_ADDRESSES_TEMPLATE=" `"$JSON_ADDRESSES`": [{0}]"
$JSON_ETHERNETMAC_TEMPLATE=" {{
                `"$JSON_ETHERNETMAC`": `"{0}`" }} "
$JSON_WLANMAC_TEMPLATE=" {{
                `"$JSON_WLANMAC`": `"{0}`" }} "
$JSON_BLUETOOTHMAC_TEMPLATE=" {{
                `"$JSON_BLUETOOTHMAC`": `"{0}`" }} "
$JSON_COMPONENTCLASS_TEMPLATE=" `"$JSON_COMPONENTCLASS`": {{
        `"$JSON_COMPONENTCLASSREGISTRY`": `"{0}`",
        `"$JSON_COMPONENTCLASSVALUE`": `"{1}`"
    }}"
$JSON_ATTRIBUTECERTIDENTIFIER_TEMPLATE=" `"$JSON_ATTRIBUTECERTIDENTIFIER`": {{
        `"$JSON_HASHALG`": `"{0}`",
        `"$JSON_HASHVALUE`": `"{1}`"
    }},"
$JSON_GENERICCERTIDENTIFIER_TEMPLATE=" `"$JSON_GENERICCERTIDENTIFIER`": {{
        `"$JSON_ISSUER`": `"{0}`",
        `"$JSON_SERIAL`": `"{1}`"
    }},"
$JSON_COMPONENTPLATFORMCERT_TEMPLATE="
    `"$JSON_COMPONENTPLATFORMCERT`": {{
        {0}
    }}"
$JSON_COMPONENTPLATFORMCERTURI_TEMPLATE='
    `"$JSON_COMPONENTPLATFORMCERTURI`": {{
        {0}
    }}'
$JSON_STATUS_TEMPLATE="
    `"$JSON_STATUS`": {{

    }}"

### JSON Constructor Aides
function HexToByteArray { # Powershell doesn't have a built in BinToHex function
    Param ([String] $str )

    if ($str.Length % 2 -ne 0) {
        $str="0$str"
    }

    if ($str.Length -ne 0) {
        ,@($str -split '([a-f0-9]{2})' | foreach-object {
            if ($_) {
                [System.Convert]::ToByte($_,16)
            }
        })
    }
}
function jsonComponentClass () {
    echo ("$JSON_COMPONENTCLASS_TEMPLATE" -f "$($args[0])","$($args[1])")
}
function jsonManufacturer () {
    $manufacturer=("`"$JSON_MANUFACTURER`": `"{0}`"" -f "$($args[0])")
    #$tmpManufacturerId=(queryForPen "$($args[0])")
    #if (($tmpManufacturerId) -and ("$tmpManufacturerId" -ne "$PEN_ROOT")) {
    #    $tmpManufacturerId=(jsonManufacturerId "$tmpManufacturerId")
    #    $manufacturer="$manufacturer,$tmpManufacturerId"
    #}
    echo "$manufacturer"
}
function jsonModel () {
    echo ("`"$JSON_MODEL`": `"{0}`"" -f "$($args[0])")
}
function jsonSerial () {
    echo ("`"$JSON_SERIAL`": `"{0}`"" -f "$($args[0])")
}
function jsonRevision () {
    echo ("`"$JSON_REVISION`": `"{0}`"" -f "$($args[0])")
}
function jsonManufacturerId () {
    echo ("`"$JSON_MANUFACTURERID`": `"{0}`"" -f "$($args[0])")
}
function jsonFieldReplaceable () {
    echo ("`"$JSON_FIELDREPLACEABLE`": `"{0}`"" -f "$($args[0])")
}
function jsonEthernetMac () {
    echo ("$JSON_ETHERNETMAC_TEMPLATE" -f "$($args[0])")
}
function jsonWlanMac () {
    echo ("$JSON_WLANMAC_TEMPLATE" -f "$($args[0])")
}
function jsonBluetoothMac () {
    echo ("$JSON_BLUETOOTHMAC_TEMPLATE" -f "$($args[0])")
}
function jsonPlatformModel () {
    echo ("`"$JSON_PLATFORMMODEL`": `"{0}`"" -f "$($args[0])")
}
function jsonPlatformManufacturerStr () {
    $manufacturer=("`"$JSON_PLATFORMMANUFACTURERSTR`": `"{0}`"" -f "$($args[0])")
    #$tmpManufacturerId=(queryForPen "$($args[0])")
    #if (($tmpManufacturerId) -and ("$tmpManufacturerId" -ne "$PEN_ROOT")) {
    #    $tmpManufacturerId=(jsonPlatformManufacturerId "$tmpManufacturerId")
    #    $manufacturer="$manufacturer,$tmpManufacturerId"
    #}
    echo "$manufacturer"
}
function jsonPlatformVersion () {
    echo ("`"$JSON_PLATFORMVERSION`": `"{0}`"" -f "$($args[0])")
}
function jsonPlatformSerial () {
    echo ("`"$JSON_PLATFORMSERIAL`": `"{0}`"" -f "$($args[0])")
}
function jsonPlatformManufacturerId () {
    echo ("`"$JSON_PLATFORMMANUFACTURERID`": `"{0}`"" -f "$($args[0])")
}
function queryForPen () {
    Write-Progress -Id 3 -ParentId 2 -Activity "Searching for PEN..."
    $result=$PEN_ROOT
    if($args[0]) {
        $penObject=(Get-Content "$ENTERPRISE_NUMBERS_FILE" | Select-String -Pattern "^[ \t]*$($args[0])`$" -Context 1)
        if ($penObject) {
            Write-Progress -Id 3 -ParentId 2 -Activity "Searching for PEN..." -CurrentOperation "Found"
            $pen=$penObject.Context.PreContext[0]
            $result+="$pen"
        }
    }
    Write-Progress -Id 3 -ParentId 2 -Activity "Searching for PEN..." -PercentComplete 100
    echo $result
}
function jsonProperty () {
    if ($args.Length -eq 2) {
        echo ("$JSON_PROPERTY_TEMPLATE" -f "$($args[0])","$($args[1])")
    }
}
function jsonUri () {
    echo ("`"$JSON_URI`": `"{0}`"" -f "$($args[0])")
}
function jsonHashAlg () {
    echo ("`"$JSON_HASHALG`": `"{0}`"" -f "$($args[0])")
}
function jsonHashValue () {
    echo ("`"$JSON_HASHVALUE`": `"{0}`"" -f "$($args[0])")
}
function toCSV () {
    if ($args.Length -ne 0) {
        Write-Progress -Id 3 -ParentId 2 -Activity "CSV..." -PercentComplete 0

        $size = $args[0].Length
        for ($i=0; $i -lt $size; $i++) {
            Write-Progress -Id 3 -ParentId 2 -Activity "CSV..." -PercentComplete (($i / $size) * 100)

            $item=($args[0].Get($i))

            if ($item) {
                $value="$value,$($args[0].Get($i))"
            }
        }
        echo "$value".Trim(" ", ",")
        Write-Progress -Id 3 -ParentId 2 -Activity "CSV..." -PercentComplete 100
    }
}
function jsonAddress () {
    echo ("$JSON_ADDRESSES_TEMPLATE" -f "$(toCSV($args))")
}
function jsonComponent () {
    echo ("$JSON_COMPONENT_TEMPLATE" -f "$(toCSV($args))")
}
function jsonComponentArray () {
    echo ("$JSON_COMPONENT_ARRAY_TEMPLATE" -f "$(toCSV($args))")
}
function jsonPropertyArray () {
    echo ("$JSON_PROPERTY_ARRAY_TEMPLATE" -f "$(toCSV($args))")
}
function jsonPlatformObject () {
    echo ("$JSON_PLATFORM_TEMPLATE" -f "$(toCSV($args))")
}
function jsonComponentsUri () {
    if ($COMPONENTS_URI) {
        $componentsUri=(jsonUri "$COMPONENTS_URI")
        $componentsUriDetails=""
        if ($COMPONENTS_URI_LOCAL_COPY_FOR_HASH) {
            $hashAlg="2.16.840.1.101.3.4.2.1" # SHA256, see https://tools.ietf.org/html/rfc5754 for other common hash algorithm IDs
            $hashValue=([System.Convert]::ToBase64String($(HexToByteArray $(Get-FileHash "$COMPONENTS_URI_LOCAL_COPY_FOR_HASH"  -Algorithm SHA256).Hash.Trim())))
            $hashAlgStr=(jsonHashAlg "$hashAlg")
            $hashValueStr=(jsonHashValue "$hashValue")
            $componentsUriDetails="$hashAlgStr"",""$hashValueStr"
        }
    echo ("$JSON_COMPONENTSURI_TEMPLATE" -f "$(toCSV("$componentsUri","$componentsUriDetails"))")
    }
}
function jsonPropertiesUri () {
    if ($PROPERTIES_URI) {
        $propertiesUri=(jsonUri "$PROPERTIES_URI")
        $propertiesUriDetails=""
        if ($PROPERTIES_URI_LOCAL_COPY_FOR_HASH) {
            $hashAlg="2.16.840.1.101.3.4.2.1" # SHA256, see https://tools.ietf.org/html/rfc5754 for other common hash algorithm IDs
            $hashValue=([System.Convert]::ToBase64String($(HexToByteArray $(Get-FileHash "$PROPERTIES_URI_LOCAL_COPY_FOR_HASH"  -Algorithm SHA256).Hash.Trim())))
            $hashAlgStr=(jsonHashAlg "$hashAlg")
            $hashValueStr=(jsonHashValue "$hashValue")
            $propertiesUriDetails="$hashAlgStr,$hashValueStr"
        }
        echo ("$JSON_PROPERTIESURI_TEMPLATE" -f "$(toCSV("$propertiesUri","$propertiesUriDetails"))")
    }
}
function jsonIntermediateFile () {
    echo ("$JSON_INTERMEDIATE_FILE_OBJECT" -f "$(toCSV($args))")
}

Write-Progress -Id 1 -Activity "Gathering component details" -PercentComplete 10

Write-Progress -Id 2 -ParentId 1 -Activity "Gathering platform information" -CurrentOperation "Querying" -PercentComplete 0
### Gather platform details for the subject alternative name
$platformManufacturer=(Get-SMBiosString $smbios "$SMBIOS_TYPE_PLATFORM" 0x4)
$platformModel=(Get-SMBiosString $smbios "$SMBIOS_TYPE_PLATFORM" 0x5)
$platformVersion=(Get-SMBiosString $smbios "$SMBIOS_TYPE_PLATFORM" 0x6)
$platformSerial=(Get-SMBiosString $smbios "$SMBIOS_TYPE_PLATFORM" 0x7)

Write-Progress -Id 2 -ParentId 1 -Activity "Gathering platform information" -CurrentOperation "Cleaning output" -PercentComplete 40
if ([string]::IsNullOrEmpty($platformManufacturer) -or ($platformManufacturer.Trim().Length -eq 0)) {
    $platformManufacturer="$NOT_SPECIFIED"
}
$platformManufacturer=$(jsonPlatformManufacturerStr "$platformManufacturer".Trim())

Write-Progress -Id 2 -ParentId 1 -Activity "Gathering platform information" -CurrentOperation "Cleaning output" -PercentComplete 55
if ([string]::IsNullOrEmpty($platformModel) -or ($platformModel.Trim().Length -eq 0)) {
    $platformModel="$NOT_SPECIFIED"
}
$platformModel=$(jsonPlatformModel "$platformModel".Trim())

Write-Progress -Id 2 -ParentId 1 -Activity "Gathering platform information" -CurrentOperation "Cleaning output" -PercentComplete 70
if ([string]::IsNullOrEmpty($platformVersion) -or ($platformVersion.Trim().Length -eq 0)) {
    $platformVersion="$NOT_SPECIFIED"
}
$platformVersion=(jsonPlatformVersion "$platformVersion".Trim())

Write-Progress -Id 2 -ParentId 1 -Activity "Gathering platform information" -CurrentOperation "Cleaning output" -PercentComplete 85
if (![string]::IsNullOrEmpty($platformSerial) -and ($platformSerial.Trim().Length -ne 0)) {
    $platformSerial=(jsonPlatformSerial "$platformSerial".Trim())
}
$platform=(jsonPlatformObject "$platformManufacturer" "$platformModel" "$platformVersion" "$platformSerial")
Write-Progress -Id 2 -ParentId 1 -Activity "Gathering platform information" -CurrentOperation "Done" -PercentComplete 100

### Gather component details
Write-Progress -Id 1 -Activity "Gathering component details" -PercentComplete 20

Write-Progress -Id 2 -ParentId 1 -Activity "Gathering chassis information" -CurrentOperation "Querying" -PercentComplete 0
$chassisClass=(jsonComponentClass "$COMPCLASS_REGISTRY_TCG" "$COMPCLASS_CHASSIS")
$chassisManufacturer=(Get-SMBiosString $smbios "$SMBIOS_TYPE_CHASSIS" 0x4)
$chassisModel=[string]($smbios["$SMBIOS_TYPE_CHASSIS"].data[0x5])
$chassisSerial=(Get-SMBiosString $smbios "$SMBIOS_TYPE_CHASSIS" 0x7)
$chassisRevision=(Get-SMBiosString $smbios "$SMBIOS_TYPE_CHASSIS" 0x6)

Write-Progress -Id 2 -ParentId 1 -Activity "Gathering chassis information" -CurrentOperation "Cleaning output" -PercentComplete 40
if ([string]::IsNullOrEmpty($chassisManufacturer) -or ($chassisManufacturer.Trim().Length -eq 0)) {
    $chassisManufacturer="$NOT_SPECIFIED"
}
$chassisManufacturer=$(jsonManufacturer "$chassisManufacturer".Trim())

Write-Progress -Id 2 -ParentId 1 -Activity "Gathering chassis information" -CurrentOperation "Cleaning output" -PercentComplete 55
if ([string]::IsNullOrEmpty($chassisModel) -or ($chassisModel.Trim().Length -eq 0)) {
    $chassisModel="$NOT_SPECIFIED"
}
$chassisModel=$(jsonModel "$chassisModel".Trim())

Write-Progress -Id 2 -ParentId 1 -Activity "Gathering chassis information" -CurrentOperation "Cleaning output" -PercentComplete 70
if (![string]::IsNullOrEmpty($chassisSerial) -and ($chassisSerial.Trim().Length -ne 0)) {
    $chassisSerial=(jsonSerial "$chassisSerial".Trim())
}

Write-Progress -Id 2 -ParentId 1 -Activity "Gathering chassis information" -CurrentOperation "Cleaning output" -PercentComplete 85
if (![string]::IsNullOrEmpty($chassisRevision) -and ($chassisRevision.Trim().Length -ne 0)) {
    $chassisRevision=(jsonRevision "$chassisRevision".Trim())
}
$componentChassis=(jsonComponent "$chassisClass" "$chassisManufacturer" "$chassisModel" "$chassisSerial" "$chassisRevision")
Write-Progress -Id 2 -ParentId 1 -Activity "Gathering chassis information" -CurrentOperation "Done" -PercentComplete 100

### Gather baseboard details
Write-Progress -Id 1 -Activity "Gathering component details" -PercentComplete 30

Write-Progress -Id 2 -ParentId 1 -Activity "Gathering baseboard information" -CurrentOperation "Querying" -PercentComplete 0
$baseboardClass=(jsonComponentClass "$COMPCLASS_REGISTRY_TCG" "$COMPCLASS_BASEBOARD")
$baseboardManufacturer=(Get-SMBiosString $smbios "$SMBIOS_TYPE_BASEBOARD" 0x4)
$baseboardModel=(Get-SMBiosString $smbios "$SMBIOS_TYPE_BASEBOARD" 0x5)
$baseboardSerial=(Get-SMBiosString $smbios "$SMBIOS_TYPE_BASEBOARD" 0x7)
$baseboardRevision=(Get-SMBiosString $smbios "$SMBIOS_TYPE_BASEBOARD" 0x6)
$baseboardFeatureFlags=$smbios["$SMBIOS_TYPE_BASEBOARD"].data[0x9]
$baseboardReplaceableIndicator=0x1C # from Table 14

Write-Progress -Id 2 -ParentId 1 -Activity "Gathering baseboard information" -CurrentOperation "Cleaning output" -PercentComplete 40
$baseboardFieldReplaceableAnswer="false"
if ("$baseboardFeatureFlags" -band "$baseboardReplaceableIndicator") {
    $baseboardFieldReplaceableAnswer="true"
}
$baseboardFieldReplaceable=(jsonFieldReplaceable "$baseboardFieldReplaceableAnswer")

Write-Progress -Id 2 -ParentId 1 -Activity "Gathering baseboard information" -CurrentOperation "Cleaning output" -PercentComplete 52
if ([string]::IsNullOrEmpty($baseboardManufacturer) -or ($baseboardManufacturer.Trim().Length -eq 0)) {
    $baseboardManufacturer="$NOT_SPECIFIED"
}
$baseboardManufacturer=$(jsonManufacturer "$baseboardManufacturer".Trim())

Write-Progress -Id 2 -ParentId 1 -Activity "Gathering baseboard information" -CurrentOperation "Cleaning output" -PercentComplete 64
if ([string]::IsNullOrEmpty($baseboardModel) -or ($baseboardModel.Trim().Length -eq 0)) {
    $baseboardModel="$NOT_SPECIFIED"
}
$baseboardModel=$(jsonModel "$baseboardModel".Trim())

Write-Progress -Id 2 -ParentId 1 -Activity "Gathering baseboard information" -CurrentOperation "Cleaning output" -PercentComplete 76
if (![string]::IsNullOrEmpty($baseboardSerial) -and ($baseboardSerial.Trim().Length -ne 0)) {
    $baseboardSerial=(jsonSerial "$baseboardSerial".Trim())
}

Write-Progress -Id 2 -ParentId 1 -Activity "Gathering baseboard information" -CurrentOperation "Cleaning output" -PercentComplete 88
if (![string]::IsNullOrEmpty($baseboardRevision) -and ($baseboardRevision.Trim().Length -ne 0)) {
    $baseboardRevision=(jsonRevision "$baseboardRevision".Trim())
}
$componentBaseboard=(jsonComponent "$baseboardClass" "$baseboardManufacturer" "$baseboardModel" "$baseboardFieldReplaceable" "$baseboardSerial" "$baseboardRevision")

Write-Progress -Id 2 -ParentId 1 -Activity "Gathering baseboard information" -CurrentOperation "Done" -PercentComplete 100

### Gather BIOS details
Write-Progress -Id 1 -Activity "Gathering component details" -PercentComplete 30

Write-Progress -Id 2 -ParentId 1 -Activity "Gathering BIOS information" -CurrentOperation "Querying" -PercentComplete 0
$biosClass=(jsonComponentClass "$COMPCLASS_REGISTRY_TCG" "$COMPCLASS_BIOS")
$biosManufacturer=(Get-SMBiosString $smbios "$SMBIOS_TYPE_BIOS" 0x4)
$biosModel=""
$biosSerial=""
$biosRevision=(Get-SMBiosString $smbios "$SMBIOS_TYPE_BIOS" 0x5)

Write-Progress -Id 2 -ParentId 1 -Activity "Gathering BIOS information" -CurrentOperation "Cleaning output" -PercentComplete 40
if ([string]::IsNullOrEmpty($biosManufacturer) -or ($biosManufacturer.Trim().Length -eq 0)) {
    $biosManufacturer="$NOT_SPECIFIED"
}
$biosManufacturer=$(jsonManufacturer "$biosManufacturer".Trim())

Write-Progress -Id 2 -ParentId 1 -Activity "Gathering BIOS information" -CurrentOperation "Cleaning output" -PercentComplete 55
if ([string]::IsNullOrEmpty($biosModel) -or ($biosModel.Trim().Length -eq 0)) {
    $biosModel="$NOT_SPECIFIED"
}
$biosModel=$(jsonModel "$biosModel".Trim())

Write-Progress -Id 2 -ParentId 1 -Activity "Gathering BIOS information" -CurrentOperation "Cleaning output" -PercentComplete 70
if (![string]::IsNullOrEmpty($biosSerial) -and ($biosSerial.Trim().Length -ne 0)) {
    $biosSerial=(jsonSerial "$biosSerial".Trim())
}

Write-Progress -Id 2 -ParentId 1 -Activity "Gathering BIOS information" -CurrentOperation "Cleaning output" -PercentComplete 85
if (![string]::IsNullOrEmpty($biosRevision) -and ($biosRevision.Trim().Length -ne 0)) {
    $biosRevision=(jsonRevision "$biosRevision".Trim())
}
$componentBios=(jsonComponent "$biosClass" "$biosManufacturer" "$biosModel" "$biosSerial" "$biosRevision")

Write-Progress -Id 2 -ParentId 1 -Activity "Gathering baseboard information" -CurrentOperation "Done" -PercentComplete 100

### Gather CPU details
Write-Progress -Id 1 -Activity "Gathering component details" -PercentComplete 40

Write-Progress -Id 2 -ParentId 1 -Activity "Gathering CPU information" -CurrentOperation "Querying" -PercentComplete 0
function parseCpuData() {
    $RS=@($smbios["$SMBIOS_TYPE_CPU"])
    $component=""
    $numRows=$RS.Count
    $processorNotUpgradableIndicator=0x6

    for($i=0;$i -lt $numRows;$i++) {
        Write-Progress -Id 2 -ParentId 1 -Activity "Gathering CPU information" -CurrentOperation ("Cleaning output for CPU " + ($i+1)) -PercentComplete ((($i+1) / $numRows) * 100)

        $cpuClass=(jsonComponentClass "$COMPCLASS_REGISTRY_TCG" "$COMPCLASS_CPU")
        $tmpManufacturer=(Get-SMBiosString $RS $i 0x7)
        $tmpModel=[string]($RS[$i].data[0x6]) # Enum value for Family
        $tmpSerial=(Get-SMBiosString $RS $i 0x20)
        $tmpRevision=(Get-SMBiosString $RS $i 0x10)
        $tmpUpgradeMethod=$RS[$i].data[0x19] # Enum for Processor Upgrade

        if ([string]::IsNullOrEmpty($tmpManufacturer) -or ($tmpManufacturer.Trim().Length -eq 0)) {
            $tmpManufacturer="$NOT_SPECIFIED"
        }
        $tmpManufacturer=$(jsonManufacturer "$tmpManufacturer".Trim())

        if ([string]::IsNullOrEmpty($tmpModel) -or ($tmpModel.Trim().Length -eq 0)) {
            $tmpModel="$NOT_SPECIFIED"
        }
        $tmpModel=$(jsonModel "$tmpModel".Trim())

        if (![string]::IsNullOrEmpty($tmpSerial) -and ($tmpSerial.Trim().Length -ne 0)) {
            $tmpSerial=(jsonSerial "$tmpSerial".Trim())
        } else {
            $tmpSerial=""
        }

        if (![string]::IsNullOrEmpty($tmpRevision) -and ($tmpRevision.Trim().Length -ne 0)) {
            $tmpRevision=(jsonRevision "$tmpRevision".Trim())
        } else {
            $tmpRevision=""
        }

        if ("$tmpUpgradeMethod" -eq "$processorNotUpgradableIndicator") {
            $tmpUpgradeMethod="false"
        } else {
            $tmpUpgradeMethod="true"
        }
        $replaceable=(jsonFieldReplaceable "$tmpUpgradeMethod")

        $tmpComponent=(jsonComponent $cpuClass $tmpManufacturer $tmpModel $replaceable $tmpSerial $tmpRevision)
        $component+="$tmpComponent,"
    }
    Write-Progress -Id 2 -ParentId 1 -Activity "Gathering CPU information" -CurrentOperation "Done" -PercentComplete 100
    return "$component".Trim(",")
}

### Gather RAM details
Write-Progress -Id 1 -Activity "Gathering component details" -PercentComplete 50

Write-Progress -Id 2 -ParentId 1 -Activity "Gathering RAM information" -CurrentOperation "Querying" -PercentComplete 0
function parseRamData() {
    $RS=@($smbios["$SMBIOS_TYPE_RAM"])
    $component=""
    $replaceable=(jsonFieldReplaceable "true") # Looking for reliable indicator
    $numRows=$RS.Count

    for($i=0;$i -lt $numRows;$i++) {
        Write-Progress -Id 2 -ParentId 1 -Activity "Gathering RAM information" -CurrentOperation ("Cleaning output for Memory Chip " + ($i+1)) -PercentComplete ((($i+1) / $numRows) * 100)

        $ramClass=(jsonComponentClass "$COMPCLASS_REGISTRY_TCG" "$COMPCLASS_RAM")
        $tmpManufacturer=(Get-SMBiosString $RS $i 0x17)
        $tmpModel=(Get-SMBiosString $RS $i 0x1A)
        $tmpSerial=(Get-SMBiosString $RS $i 0x18)
        $tmpRevision=(Get-SMBiosString $RS $i 0x19)

        if ([string]::IsNullOrEmpty($tmpManufacturer) -and [string]::IsNullOrEmpty($tmpModel) -and [string]::IsNullOrEmpty($tmpSerial) -and [string]::IsNullOrEmpty($tmpRevision)) {
            Continue;
        }

        if ([string]::IsNullOrEmpty($tmpManufacturer) -or ($tmpManufacturer.Trim().Length -eq 0)) {
            $tmpManufacturer="$NOT_SPECIFIED"
        }
        $tmpManufacturer=$(jsonManufacturer "$tmpManufacturer".Trim())

        if ([string]::IsNullOrEmpty($tmpModel) -or ($tmpModel.Trim().Length -eq 0)) {
            $tmpModel="$NOT_SPECIFIED"
        }
        $tmpModel=$(jsonModel "$tmpModel".Trim())

        if (![string]::IsNullOrEmpty($tmpSerial) -and ($tmpSerial.Trim().Length -ne 0)) {
            $tmpSerial=(jsonSerial "$tmpSerial".Trim())
        } else {
            $tmpSerial=""
        }

        if (![string]::IsNullOrEmpty($tmpRevision) -and ($tmpRevision.Trim().Length -ne 0)) {
            $tmpRevision=(jsonRevision "$tmpRevision".Trim())
        } else {
            $tmpRevision=""
        }
        $tmpComponent=(jsonComponent $ramClass $tmpManufacturer $tmpModel $replaceable $tmpSerial $tmpRevision)
        $component+="$tmpComponent,"
    }

    Write-Progress -Id 2 -ParentId 1 -Activity "Gathering RAM information" -CurrentOperation "Done" -PercentComplete 100
    return "$component".Trim(",")
}

### Gather NIC details
Write-Progress -Id 1 -Activity "Gathering component details" -PercentComplete 60

Write-Progress -Id 2 -ParentId 1 -Activity "Gathering NIC information" -CurrentOperation "Querying CIM" -PercentComplete 0
function parseNicData() {
    $RS=@(Get-NetAdapter | select MacAddress,PhysicalMediaType,PNPDeviceID | where {($_.PhysicalMediaType -eq "Native 802.11" -or "802.3") -and ($_.PNPDeviceID -Match "^(PCI)\\.*$")})
    $component=""
    $replaceable=(jsonFieldReplaceable "true")
    $numRows=$RS.Count

    for($i=0;$i -lt $numRows;$i++) {
        Write-Progress -Id 2 -ParentId 1 -Activity "Gathering NIC information" -CurrentOperation ("Cleaning output for NIC " + ($i+1)) -PercentComplete ((($i+1) / $numRows) * 100)

        $nicClass=(jsonComponentClass "$COMPCLASS_REGISTRY_TCG" "$COMPCLASS_NIC")

        $pnpDevID=""
        if(isPCI($RS[$i].PNPDeviceID)) {
            $pnpDevID=(pciParse $RS[$i].PNPDeviceID)
        } else {
            Continue
        }

        $tmpManufacturer=$pnpDevID.vendor # PCI Vendor ID
        $tmpModel=$pnpDevID.product  # PCI Device Hardware ID
        $tmpSerialConstant=($RS[$i].MacAddress)
        $tmpSerialConstant=(standardizeMACAddr $tmpSerialConstant)
        $tmpSerial=""
        $tmpRevision=$pnpDevID.revision
        $tmpMediaType=$RS[$i].PhysicalMediaType
        $thisAddress=""

        if ([string]::IsNullOrEmpty($tmpManufacturer) -or ($tmpManufacturer.Trim().Length -eq 0)) {
            $tmpManufacturer="$NOT_SPECIFIED"
        }
        $tmpManufacturer=$(jsonManufacturer "$tmpManufacturer".Trim())


        if ([string]::IsNullOrEmpty($tmpModel) -or ($tmpModel.Trim().Length -eq 0)) {
            $tmpModel="$NOT_SPECIFIED"
        }
        $tmpModel=$(jsonModel "$tmpModel".Trim())



        if (![string]::IsNullOrEmpty($tmpSerialConstant) -and ($tmpSerialConstant.Trim().Length -ne 0)) {
            $tmpSerial=(jsonSerial "$tmpSerialConstant".Trim())
        } else {
            $tmpSerial=""
        }


        if (![string]::IsNullOrEmpty($tmpRevision) -and ($tmpRevision.Trim().Length -ne 0)) {
            $tmpRevision=(jsonRevision "$tmpRevision".Trim())
        } else {
            $tmpRevision=""
        }

        if ($tmpMediaType -and $tmpSerial) {
            if ("$tmpMediaType" -match "^.*802[.]11.*$") {
                $thisAddress=(jsonWlanMac $tmpSerialConstant)
            }
            elseif ("$tmpMediaType" -match "^.*[Bb]lue[Tt]ooth.*$") {
                $thisAddress=(jsonBluetoothMac $tmpSerialConstant)
            }
            elseif ("$tmpMediaType" -match "^.*802[.]3.*$") {
                $thisAddress=(jsonEthernetMac $tmpSerialConstant)
            }
            if ($thisAddress) {
                $thisAddress=(jsonAddress "$thisAddress")
            }
        }

        $tmpComponent=(jsonComponent $nicClass $tmpManufacturer $tmpModel $replaceable $tmpSerial $tmpRevision $thisAddress)
        $component+="$tmpComponent,"
    }

    Write-Progress -Id 2 -ParentId 1 -Activity "Gathering NIC information" -CurrentOperation "Done" -PercentComplete 100
    return "$component".Trim(",")
}

### Gather HDD details
Write-Progress -Id 1 -Activity "Gathering component details" -PercentComplete 70

Write-Progress -Id 2 -ParentId 1 -Activity "Gathering HDD information" -CurrentOperation "Querying" -PercentComplete 0
function parseHddData() {
    #$RS=(Get-CimInstance -ClassName CIM_DiskDrive | select serialnumber,mediatype,pnpdeviceid,manufacturer,model | where mediatype -eq "Fixed hard disk media")
    $RS=(Get-PhysicalDisk | select serialnumber,mediatype,manufacturer,model,bustype | where BusType -ne NVMe)
    $component=""
    $replaceable=(jsonFieldReplaceable "true")
    $numRows=0
    if ($RS -ne $null) {
		if ($RS -is [array]) {
			$numRows=($RS.Count)
		} else {
			$numRows=1
        }
    }
    for($i=0;$i -lt $numRows;$i++) {
        Write-Progress -Id 2 -ParentId 1 -Activity "Gathering Hard Disk information" -CurrentOperation ("Cleaning output for HDD " + ($i+1)) -PercentComplete ((($i+1) / $numRows) * 100)

        $hddClass=(jsonComponentClass "$COMPCLASS_REGISTRY_TCG" "$COMPCLASS_HDD")

        #$pnpDevID=""
        #if(isIDE($RS[$i].PNPDeviceID)) {
        #   $pnpDevID=(ideDiskParse $RS[$i].PNPDeviceID)
        #} elseif(isSCSI($RS[$i].PNPDeviceID)) {
        #   $pnpDevID=(scsiDiskParse $RS[$i].PNPDeviceID)
        #} else {
        #  Continue
        #}

        #if(($pnpDevID -eq $null) -or (($pnpDevID -eq "(Standard disk drives)") -and ($pnpDevID.product -eq $null))) {
		#   $regex="^.{,16}$"
        #    $pnpDevID=[pscustomobject]@{
        #       product=($RS[$i].model -replace '^(.{0,16}).*$','$1')  # Strange behavior for this case, will return
        #   }
        #}

        #$tmpManufacturer=$pnpDevID.vendor 
        #$tmpModel=$pnpDevID.product 
        #$tmpSerial=$RS[$i].serialnumber
        #$tmpRevision=$pnpDevID.revision

        $tmpManufacturer=$RS[$i].manufacturer
        $tmpModel=($RS[$i].model -replace '^(.{0,16}).*$','$1')
        $tmpSerial=$RS[$i].serialnumber

        if ([string]::IsNullOrEmpty($tmpManufacturer) -or ($tmpManufacturer.Trim().Length -eq 0) -or ($tmpManufacturer.Trim() -eq "NVMe")) {
            $tmpManufacturer="$NOT_SPECIFIED"
        }
        $tmpManufacturer=$(jsonManufacturer "$tmpManufacturer".Trim())

        if ([string]::IsNullOrEmpty($tmpModel) -or ($tmpModel.Trim().Length -eq 0)) {
            $tmpModel="$NOT_SPECIFIED"
        }
        $tmpModel=$(jsonModel "$tmpModel".Trim())

        if (![string]::IsNullOrEmpty($tmpSerial) -and ($tmpSerial.Trim().Length -ne 0)) {
            $tmpSerial=(jsonSerial "$tmpSerial".Trim())
        } else {
            $tmpSerial=""
        }

        if (![string]::IsNullOrEmpty($tmpRevision) -and ($tmpRevision.Trim().Length -ne 0)) {
            $tmpRevision=(jsonRevision "$tmpRevision".Trim())
        } else {
            $tmpRevision=""
        }

        $tmpComponent=(jsonComponent $hddClass $tmpManufacturer $tmpModel $replaceable $tmpSerial $tmpRevision)
        $component+="$tmpComponent,"
    }

    Write-Progress -Id 2 -ParentId 1 -Activity "Gathering Hard Disk information" -CurrentOperation "Done" -PercentComplete 100
    return "$component".Trim(",")
}

function parseNvmeData() {
    $RS=((Get-PhysicalDisk | where BusType -eq NVMe).DeviceID)
    $component=""
    $replaceable=(jsonFieldReplaceable "true") # Looking for reliable indicator

    $hddClass=(jsonComponentClass "$COMPCLASS_REGISTRY_TCG" "$COMPCLASS_HDD")

    $nvme=(Get-NVMeIdentifyData $RS)
    $numRows=$RS.Count

    for($i=0;$i -lt $numRows;$i++) {
        Write-Progress -Id 2 -ParentId 1 -Activity "Gathering NVMe Disk information" -CurrentOperation ("Cleaning output for NVMe Disk " + ($i+1)) -PercentComplete ((($i+1) / $numRows) * 100)

        $tmpManufacturer=""
        $tmpModel=(NvmeGetModelNumberForDeviceNumber $nvme $RS[$i])
        $tmpSerial=(NvmeGetNguidForDevice $nvme $RS[$i]) 
        if ("$tmpSerial" -match "^[0]+$") {
            $tmpSerial=(NvmeGetEuiForDevice $nvme $RS[$i])
        }

        if ([string]::IsNullOrEmpty($tmpManufacturer) -and [string]::IsNullOrEmpty($tmpModel) -and [string]::IsNullOrEmpty($tmpSerial) -and [string]::IsNullOrEmpty($tmpRevision)) {
            Continue;
        }

        if ([string]::IsNullOrEmpty($tmpManufacturer) -or ($tmpManufacturer.Trim().Length -eq 0)) {
            $tmpManufacturer="$NOT_SPECIFIED"
        }
        $tmpManufacturer=$(jsonManufacturer "$tmpManufacturer".Trim())

        if ([string]::IsNullOrEmpty($tmpModel) -or ($tmpModel.Trim().Length -eq 0)) {
            $tmpModel="$NOT_SPECIFIED"
        } else {
            $tmpModel=($tmpModel -replace '^(.{0,16}).*$','$1') # Reformatting for consistency for now.
        }
        $tmpModel=$(jsonModel "$tmpModel".Trim())

        if (![string]::IsNullOrEmpty($tmpSerial) -and ($tmpSerial.Trim().Length -ne 0)) {
            $tmpSerial=("$tmpSerial".Trim())
            $tmpSerial=($tmpSerial -replace "(.{4})", '$1_' -replace "_$", '.') # Reformatting for consistency for now.
            $tmpSerial=(jsonSerial $tmpSerial)
        } else {
            $tmpSerial=""
        }

        if (![string]::IsNullOrEmpty($tmpRevision) -and ($tmpRevision.Trim().Length -ne 0)) {
            $tmpRevision=(jsonRevision "$tmpRevision".Trim())
        } else {
            $tmpRevision=""
        }
        $tmpComponent=(jsonComponent $hddClass $tmpManufacturer $tmpModel $replaceable $tmpSerial $tmpRevision)
        $component+="$tmpComponent,"
    }

    Write-Progress -Id 2 -ParentId 1 -Activity "Gathering NVMe Disk information" -CurrentOperation "Done" -PercentComplete 100
    return "$component".Trim(",")
}

### Gather GFX details
Write-Progress -Id 1 -Activity "Gathering component details" -PercentComplete 70

Write-Progress -Id 2 -ParentId 1 -Activity "Gathering GFX information" -CurrentOperation "Querying" -PercentComplete 0
function parseGfxData() {
    $RS=(Get-CimInstance -ClassName CIM_VideoController | select pnpdeviceid )
    $component=""
    $replaceable=(jsonFieldReplaceable "true")
    $numRows=1
    if ($RS.Count -gt 1) {
        $numRows=($RS.Count)
    }
    for($i=0;$i -lt $numRows;$i++) {
        Write-Progress -Id 2 -ParentId 1 -Activity "Gathering Graphics information" -CurrentOperation ("Cleaning output for HDD " + ($i+1)) -PercentComplete ((($i+1) / $numRows) * 100)

        $gfxClass=(jsonComponentClass "$COMPCLASS_REGISTRY_TCG" "$COMPCLASS_GFX")

        $pnpDevID=""
        if(isPCI($RS[$i].PNPDeviceID)) {
            $pnpDevID=(pciParse $RS[$i].PNPDeviceID)
        } else {
            Continue
        }

        $tmpManufacturer=$pnpDevID.vendor # PCI Vendor ID
        $tmpModel=$pnpDevID.product  # PCI Device Hardware ID
        $tmpRevision=$pnpDevID.revision
        # CIM Class does not contain serialnumber

        if ([string]::IsNullOrEmpty($tmpManufacturer) -or ($tmpManufacturer.Trim().Length -eq 0)) {
            $tmpManufacturer="$NOT_SPECIFIED"
        }
        $tmpManufacturer=$(jsonManufacturer "$tmpManufacturer".Trim())

        if ([string]::IsNullOrEmpty($tmpModel) -or ($tmpModel.Trim().Length -eq 0)) {
            $tmpModel="$NOT_SPECIFIED"
        }
        $tmpModel=$(jsonModel "$tmpModel".Trim())

        if (![string]::IsNullOrEmpty($tmpRevision) -and ($tmpRevision.Trim().Length -ne 0)) {
            $tmpRevision=(jsonRevision "$tmpRevision".Trim())
        } else {
            $tmpRevision=""
        }

        $tmpComponent=(jsonComponent $gfxClass $tmpManufacturer $tmpModel $replaceable $tmpRevision)
        $component+="$tmpComponent,"
    }

    Write-Progress -Id 2 -ParentId 1 -Activity "Gathering Graphics information" -CurrentOperation "Done" -PercentComplete 100
    return "$component".Trim(",")
}

### Collate the component details
$componentsCPU=$(parseCpuData)
$componentsRAM=$(parseRamData)
$componentsNIC=$(parseNicData)
$componentsHDD=$(parseHddData)
$componentsNVMe=$(parseNvmeData)
$componentsGFX=$(parseGfxData)
$componentArray=(jsonComponentArray "$componentChassis" "$componentBaseboard" "$componentBios" "$componentsCPU" "$componentsRAM" "$componentsNIC" "$componentsHDD" "$componentsNVMe" "$componentsGFX")

### Gather property details
Write-Progress -Id 1 -Activity "Gathering properties" -PercentComplete 80
$osCaption=((wmic os get caption /value | Select-String -Pattern "^.*=(.*)$").Matches.Groups[1].ToString().Trim())
$property1=(jsonProperty "caption" "$osCaption")  ## Example1
$property2= ## Example2

### Collate the property details
$propertyArray=(jsonPropertyArray "$property1")

### Collate the URI details, if parameters above are blank, the fields will be excluded from the final JSON structure
$componentsUri=""
if ($COMPONENTS_URI) {
    $componentsUri=(jsonComponentsUri)
}
$propertiesUri=""
if ($PROPERTIES_URI) {
    $propertiesUri=(jsonPropertiesUri)
}

Write-Progress -Id 1 -Activity "Forming final output" -PercentComplete 90
### Construct the final JSON object
$FINAL_JSON_OBJECT=(jsonIntermediateFile "$platform" "$componentArray" "$componentsUri" "$propertyArray" "$propertiesUri")

Write-Progress -Id 1 -Activity "Done" -PercentComplete 100
[IO.File]::WriteAllText($filename, "$FINAL_JSON_OBJECT")

