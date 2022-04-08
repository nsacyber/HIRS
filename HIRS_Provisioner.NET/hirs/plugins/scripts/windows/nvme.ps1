#
# This method accesses NVMe disk identify data via Windows and makes that data accessible to Powershell.
#
# Usage:  $nvme=(Get-NVMeIdentifyData $nvmeDriveNumber)
#        where $nvmeDriveNumber is one or more drive numbers on an NVMe bus
#        Drive Numbers can be found using this command: Get-PhysicalDisk | where BusType -eq NVMe).DeviceID
#        $nvme[$nvmeDriveNumber].id_ns will have NVME_IDENTIFY_NAMESPACE_DATA binary data in a byte[]
#        $nvme[$nvmeDriveNumber].id_ctrl will have NVME_IDENTIFY_CONTROLLER_DATA binary data in a byte[]
#
# Adapted from nvmetool-win-powershell released under MIT License
# https://github.com/ken-yossy/nvmetool-win-powershell/blob/main/scripts/get-subnqn.ps1
#
Function Get-NVMeIdentifyData($NvmeDriveNumbers) {
    $struct=@{}

    # If $NvmeDriveNumbers is an integer, convert to one element array
    if ($NvmeDriveNumbers -isnot [array]) {
        if ($NvmeDriveNumbers -is [int]) {
            $tmp=$NvmeDriveNumbers
            $NvmeDriveNumbers=@($tmp)
        } elseif ($NvmeDriveNumbers -is [string]) {
            try {
                $tmp=[int]$NvmeDriveNumbers
                $NvmeDriveNumbers=@($tmp)
            } catch {
                return @($struct);
            }
        } else {
            return @($struct);
        }
    }

    # https://docs.microsoft.com/en-us/windows/win32/fileio/working-with-nvme-devices#example-nvme-identify-query
    $NVME_MAX_LOG_SIZE = 0x1000 # size of either NVME_IDENTIFY_NAMESPACE_DATA and NVME_IDENTIFY_CONTROLLER_DATA
    $StoragePropertyQueryOffset=8   # FIELD_OFFSET(STORAGE_PROPERTY_QUERY, AdditionalParameters)
    $WindowsNVMeProtocolSize=40    # sizeof(STORAGE_PROTOCOL_SPECIFIC_DATA)
    $QueryOffset=($StoragePropertyQueryOffset+$WindowsNVMeProtocolSize)
    $IdentifyDataSize=$NVME_MAX_LOG_SIZE
    $IdentifyQueryBufferSize=($QueryOffset+$IdentifyDataSize)

    $KernelService=(EstablishKernelService)
    for (($i = 0); $i -lt $NvmeDriveNumbers.Count; $i++) {
        $DriveNumber=$NvmeDriveNumbers[$i]
        if ($DriveNumber -is [string]) {
            try {
                $DriveNumber=[int]($NvmeDriveNumbers[$i])
            } catch {
                continue
            }
        } elseif ($DriveNumber -isnot [int]) {
            continue
        }

        $drive=@{}
        $DeviceHandle=(EstablishDeviceHandle $KernelService $DriveNumber)
        try {
            $NsValue=[NVME_IDENTIFY_CNS_CODES]::NVME_IDENTIFY_CNS_SPECIFIC_NAMESPACE
            $CtrlValue=[NVME_IDENTIFY_CNS_CODES]::NVME_IDENTIFY_CNS_CONTROLLER

            $NamespaceSelector=1
            $NsProperty=(SetUpQueryProperties $WindowsNVMeProtocolSize $IdentifyDataSize $NsValue $NamespaceSelector)
            $CtrlProperty=(SetUpQueryProperties $WindowsNVMeProtocolSize $IdentifyDataSize $CtrlValue 0)

            $NsData=(QueryNVMeIdentify $KernelService $DeviceHandle $NsProperty $IdentifyQueryBufferSize)
            $CtrlData=(QueryNVMeIdentify $KernelService $DeviceHandle $CtrlProperty $IdentifyQueryBufferSize)

            $drive = [pscustomobject]@{
                id_ns=$NsData
                id_ctrl=$CtrlData
            }
            $struct["$DriveNumber"]=$drive
        } finally {
            [void]$KernelService::CloseHandle($DeviceHandle);
        }
    }

    return @($struct)
}

# CNS 00h
# CNS 01h
Function SetUpQueryProperties($ProtocolDataOffset, $ProtocolDataLength, $ProtocolDataRequestValue, $ProtocolDataRequestSubValue) {
    $Property      = New-Object STORAGE_PROPERTY_QUERY
    $PropertySize  = [System.Runtime.InteropServices.Marshal]::SizeOf($Property)

    $ExpectedPropertySize=(8+$ProtocolDataOffset+$ProtocolDataLength)
    if ( $PropertySize -ne $ExpectedPropertySize ) {
        echo ("Size of structure is {0}  bytes, expect {1} bytes, stop" -F $PropertySize,$ExpectedPropertySize)
        Return;
    }

    $Property.PropertyId = [STORAGE_PROPERTY_ID]::StorageAdapterProtocolSpecificProperty
    $Property.QueryType = [STORAGE_QUERY_TYPE]::PropertyStandardQuery
    $Property.ProtocolType  = [STORAGE_PROTOCOL_TYPE]::ProtocolTypeNvme
    $Property.DataType = [STORAGE_PROTOCOL_NVME_DATA_TYPE]::NVMeDataTypeIdentify

    $Property.ProtocolDataOffset = $ProtocolDataOffset;  # sizeof(STORAGE_PROTOCOL_SPECIFIC_DATA)
    $Property.ProtocolDataLength = $ProtocolDataLength; # sizeof(NVME_IDENTIFY_NAMESPACE_DATA or NVME_IDENTIFY_CONTROLLER_DATA)

    $Property.ProtocolDataRequestValue = $ProtocolDataRequestValue; # NVME_IDENTIFY_CNS_SPECIFIC_NAMESPACE or NVME_IDENTIFY_CNS_CONTROLLER
    $Property.ProtocolDataRequestSubValue = $ProtocolDataRequestSubValue; # >0 if NVME_IDENTIFY_CNS_SPECIFIC_NAMESPACE, 0 if NVME_IDENTIFY_CNS_CONTROLLER

    return $Property;
}

Function QueryNVMeIdentify($KernelService, $DeviceHandle, $Property, $BufferSize) {
    # winioctl.h
    $deviceType = 0x02d # IOCTL_STORAGE_BASE = FILE_DEVICE_MASS_STORAGE
    $function = 0x0500
    $method = 0 # METHOD_BUFFERED
    $access = 0 # FILE_ANY_ACCESS
    $ioControlCode =(($deviceType -shl 16) -bor ($access -shl 14) -bor ($function -shl 2) -bor ($method)) # IOCTL_STORAGE_QUERY_PROPERTY

    $Buffer = [System.Runtime.InteropServices.Marshal]::AllocHGlobal($BufferSize);

    try {
        $ByteRet = 0;

        [System.Runtime.InteropServices.Marshal]::StructureToPtr($Property, $Buffer, [System.Boolean]::true)
        $CallResult = $KernelService::DeviceIoControl($DeviceHandle, $ioControlCode, $Buffer, $BufferSize, $Buffer, $BufferSize, [ref]$ByteRet, [System.IntPtr]::Zero);

        $LastError = [ComponentModel.Win32Exception][Runtime.InteropServices.Marshal]::GetLastWin32Error();
        if ( $CallResult -eq 0 ) {
            echo ("DeviceIoControl() failed retrieving Value: {0} SubValue: {1}: {2}" -F $Property.ProtocolDataRequestValue,$Property.ProtocolDataRequestSubValue,"$LastError")
            Return;
        }

        if ( $ByteRet -ne $BufferSize ) {
            echo ("Data size returned ({0} bytes) is wrong for {1} SubValue: {2}; expect {3} bytes" -F $ByteRet,$Property.ProtocolDataRequestValue,$Property.ProtocolDataRequestSubValue,$BufferSize)
            Return;
        }

        $Ptr = [System.IntPtr]::Add($Buffer, ($BufferSize-$Property.ProtocolDataLength));
        $data = New-Object byte[] ($Property.ProtocolDataLength)
        [System.Runtime.InteropServices.Marshal]::Copy($Ptr, $data, 0, $Property.ProtocolDataLength)
    } finally {
        [System.Runtime.InteropServices.Marshal]::FreeHGlobal($Buffer)
    }

    return $data
}

Function EstablishKernelService() {
    $KernelService = Add-Type -Name 'Kernel32' -Namespace 'Win32' -PassThru -MemberDefinition @"
    [DllImport("kernel32.dll", CharSet = CharSet.Unicode, SetLastError = true)]
    public static extern Microsoft.Win32.SafeHandles.SafeFileHandle CreateFile(
        string fileName,
        System.IO.FileAccess fileAccess,
        System.IO.FileShare fileShare,
        IntPtr securityAttributes,
        System.IO.FileMode creationDisposition,
        System.IO.FileAttributes fileAttributes,
        IntPtr templateFile);

    [DllImport("kernel32.dll", SetLastError = true)]
    public static extern bool DeviceIoControl(
        Microsoft.Win32.SafeHandles.SafeFileHandle hDevice,
        int ioControlCode,
        IntPtr inBuffer,
        int inBufferSize,
        IntPtr outBuffer,
        int outBufferSize,
        ref int bytesReturned,
        IntPtr overlapped);

    [DllImport("kernel32.dll", SetLastError=true)]
    public static extern bool CloseHandle(Microsoft.Win32.SafeHandles.SafeFileHandle hDevice);
"@
    Add-Type -TypeDefinition @"
    using System;
    using System.Runtime.InteropServices;

    // The following definitions are from winioctl.h
    public enum STORAGE_PROPERTY_ID {
        StorageDeviceProperty = 0,
        StorageAdapterProperty,
        StorageDeviceIdProperty,
        StorageDeviceUniqueIdProperty,
        StorageDeviceWriteCacheProperty,
        StorageMiniportProperty,
        StorageAccessAlignmentProperty,
        StorageDeviceSeekPenaltyProperty,
        StorageDeviceTrimProperty,
        StorageDeviceWriteAggregationProperty,
        StorageDeviceDeviceTelemetryProperty,
        StorageDeviceLBProvisioningProperty,
        StorageDevicePowerProperty,
        StorageDeviceCopyOffloadProperty,
        StorageDeviceResiliencyProperty,
        StorageDeviceMediumProductType,
        StorageAdapterRpmbProperty,
        StorageAdapterCryptoProperty,
        StorageDeviceIoCapabilityProperty = 48,
        StorageAdapterProtocolSpecificProperty,
        StorageDeviceProtocolSpecificProperty,
        StorageAdapterTemperatureProperty,
        StorageDeviceTemperatureProperty,
        StorageAdapterPhysicalTopologyProperty,
        StorageDevicePhysicalTopologyProperty,
        StorageDeviceAttributesProperty,
        StorageDeviceManagementStatus,
        StorageAdapterSerialNumberProperty,
        StorageDeviceLocationProperty,
        StorageDeviceNumaProperty,
        StorageDeviceZonedDeviceProperty,
        StorageDeviceUnsafeShutdownCount,
        StorageDeviceEnduranceProperty
    }

    public enum STORAGE_QUERY_TYPE {
        PropertyStandardQuery = 0,
        PropertyExistsQuery,
        PropertyMaskQuery,
        PropertyQueryMaxDefined
    }

    public enum STORAGE_PROTOCOL_TYPE {
        ProtocolTypeUnknown = 0x00,
        ProtocolTypeScsi,
        ProtocolTypeAta,
        ProtocolTypeNvme,
        ProtocolTypeSd,
        ProtocolTypeUfs,
        ProtocolTypeProprietary = 0x7E,
        ProtocolTypeMaxReserved = 0x7F
    }

    public enum STORAGE_PROTOCOL_NVME_DATA_TYPE {
        NVMeDataTypeUnknown = 0,
        NVMeDataTypeIdentify,
        NVMeDataTypeLogPage,
        NVMeDataTypeFeature
    }

    // The following definitions are from nvme.h
    public enum NVME_IDENTIFY_CNS_CODES {
        NVME_IDENTIFY_CNS_SPECIFIC_NAMESPACE = 0,
        NVME_IDENTIFY_CNS_CONTROLLER = 1,
        NVME_IDENTIFY_CNS_ACTIVE_NAMESPACES = 2,
        NVME_IDENTIFY_CNS_DESCRIPTOR_NAMESPACE = 3,
        NVME_IDENTIFY_CNS_NVM_SET = 4
    }

    // winioctl.h
    [StructLayout(LayoutKind.Sequential, Pack=1)]
    public struct STORAGE_PROPERTY_QUERY {
        public STORAGE_PROPERTY_ID PropertyId;
        public STORAGE_QUERY_TYPE QueryType;
        // AdditionalParameters below = STORAGE_PROTOCOL_SPECIFIC_DATA
        public STORAGE_PROTOCOL_TYPE ProtocolType;
        public STORAGE_PROTOCOL_NVME_DATA_TYPE DataType;
        public NVME_IDENTIFY_CNS_CODES   ProtocolDataRequestValue;
        public int ProtocolDataRequestSubValue;  // Data sub request value
        public int ProtocolDataOffset;           // The offset of data buffer is from beginning of this data structure.
        public int ProtocolDataLength;
        public int FixedProtocolReturnData;
        public int ProtocolDataRequestSubValue2; // First additional data sub request value
        public int ProtocolDataRequestSubValue3; // Second additional data sub request value
        public int Reserved;

        [MarshalAs(UnmanagedType.ByValArray, SizeConst = 4096)]
        public Byte[] IdentifyControllerData;
    }
"@
    return $KernelService
}


Function EstablishDeviceHandle($KernelService, $DriveNumber) {
    $fileAccess = [System.IO.FileAccess]::ReadWrite
    $fileShare = [System.IO.FileShare]::ReadWrite
    $fileMode = [System.IO.FileMode]::Open
    $fileAttributes = [System.IO.FileAttributes]::Device

    $DeviceHandle = $KernelService::CreateFile("\\.\PhysicalDrive$DriveNumber", $fileAccess, $fileShare, [System.IntPtr]::Zero, $fileMode, $fileAttributes, [System.IntPtr]::Zero);

    $LastError = [ComponentModel.Win32Exception][Runtime.InteropServices.Marshal]::GetLastWin32Error()
    if ($DeviceHandle -eq [System.IntPtr]::Zero) {
         echo ("CreateFile failed: {0}" -F $LastError);
         Return;
    }

    return $DeviceHandle
}

Function HasIdentifyData($nvme, $DriveNumber) {
    $result=$false
    if (($nvme -ne $null) -and ($nvme["$DriveNumber"] -ne $null) -and (!($nvme["$DriveNumber"] -Match "failed"))) {
        $result=$true
    }
    return $result
}
Function HasIdentifyNamespaceData($nvme, $DriveNumber) {
    $result=$false
    if ((HasIdentifyData $nvme $DriveNumber) -and ($nvme["$DriveNumber"].id_ns.Count -gt 0)) {
        $result=$true
    }
    return $result
}
Function HasIdentifyControllerData($nvme, $DriveNumber) {
    $result=$false
    if ((HasIdentifyData $nvme $DriveNumber) -and ($nvme["$DriveNumber"].id_ctrl.Count -gt 0)) {
        $result=$true
    }
    return $result
}

# NVME_IDENTIFY_NAMESPACE_DATA
#  119: 104 Namespace Globally Unique Identifier (NGUID)
#  127: 120 IEEE Extended Unique Identifier (EUI64)
Function NvmeGetNguidForDevice($nvme, $DriveNumber) {
    $str=""
    if (HasIdentifyNamespaceData $nvme $DriveNumber) {
        $str=(($nvme["$DriveNumber"].id_ns[104..119]|ForEach-Object ToString X2) -join '')
    }
    return "$str"
}
Function NvmeGetEuiForDevice($nvme, $DriveNumber) {
    $str=""
    if (HasIdentifyNamespaceData $nvme $DriveNumber) {
        $str=(($nvme["$DriveNumber"].id_ns[120..127]|ForEach-Object ToString X2) -join '')
    }
    return "$str"
}
# NVME_IDENTIFY_CONTROLLER_DATA
#   23:   4 Serial Number (SN)
#   63:  24 Model Number (MN)
#   71:  64 Firmware Revision (FR)
# 1023: 768 NVM Subsystem NVMe Qualified Name (SUBNQN)
Function NvmeGetSerialNumberForDevice($nvme, $DriveNumber) {
    $str=""
    if (HasIdentifyControllerData $nvme $DriveNumber) {
        $str=("{0}" -F [System.Text.Encoding]::ASCII.GetString($nvme["$DriveNumber"].id_ctrl[4..23]))
    }
    return "$str"
}
Function NvmeGetModelNumberForDeviceNumber($nvme, $DriveNumber) {
    $str=""
    if (HasIdentifyControllerData $nvme $DriveNumber) {
        $str=("{0}" -F [System.Text.Encoding]::ASCII.GetString($nvme["$DriveNumber"].id_ctrl[24..63]))
    }
    return "$str"
}
Function NvmeGetFirmwareRevisionForDeviceNumber($nvme, $DriveNumber) {
    $str=""
    if (HasIdentifyControllerData $nvme $DriveNumber) {
        $str=("{0}" -F [System.Text.Encoding]::ASCII.GetString($nvme["$DriveNumber"].id_ctrl[64..71]))
    }
    return "$str"
}
Function NvmeGetSubnqnForDevice($nvme, $DriveNumber) {
    $str=""
    if (HasIdentifyControllerData $nvme $DriveNumber) {
        $str=("{0}" -F [System.Text.Encoding]::ASCII.GetString($nvme["$DriveNumber"].id_ctrl[768..1023]))
    }
    return "$str"
}


# Example:
#$nvmeDriveNumber="2"
#$nvme=(Get-NVMeIdentifyData $nvmeDriveNumber)
#echo (HasIdentifyData $nvme $nvmeDriveNumber)
#echo (HasIdentifyNamespaceData $nvme $nvmeDriveNumber)
#echo (HasIdentifyControllerData $nvme $nvmeDriveNumber)
#$nvmeDriveNumbers=((Get-PhysicalDisk | where BusType -eq NVMe).DeviceID)
#$nvmeDriveNumber=$nvmeDriveNumbers[0]
#$nvme=(Get-NVMeIdentifyData $nvmeDriveNumbers)
#echo $nvmeDriveNumbers
#echo $nvme
#echo (HasIdentifyData $nvme $nvmeDriveNumber)
#echo (HasIdentifyNamespaceData $nvme $nvmeDriveNumber)
#echo (HasIdentifyControllerData $nvme $nvmeDriveNumber)
#echo ("Namespace Globally Unique Identifier (NGUID): {0}" -F (NvmeGetNguidForDevice $nvme $nvmeDriveNumber))
#echo ("IEEE Extended Unique Identifier (EUI64): {0}" -F (NvmeGetEuiForDevice $nvme $nvmeDriveNumber))
#echo ("Serial Number (SN): {0}" -F (NvmeGetSerialNumberForDevice $nvme $nvmeDriveNumber))
#echo ("Model Number (MN): {0}" -F (NvmeGetModelNumberForDeviceNumber $nvme $nvmeDriveNumber))
#echo ("Firmware Revision (FR): {0}" -F (NvmeGetFirmwareRevisionForDeviceNumber $nvme $nvmeDriveNumber))
#echo ("NVM Subsystem NVMe Qualified Name (SUBNQN): {0}" -F (NvmeGetSubnqnForDevice $nvme $nvmeDriveNumber))