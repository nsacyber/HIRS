package hirs.utils.tpm.eventlog.uefi;

import hirs.utils.HexUtils;
import lombok.Getter;

import java.nio.charset.StandardCharsets;

/**
 * Class to process a Device Path. A Device Path is a variable-length binary
 * structure that is made up of variable-length generic Device Path nodes.
 * The first Device Path node starts at byte offset zero of the Device Path.
 * The next Device Path node starts at the end of the previous Device Path node.
 * There is no limit to the number, type, or sequence of nodes in a Device Path.
 * <p>
 * Generic Device Path Node Structure:
 * Name     Byte Offset     Byte Length     Description
 * Type     0               1               Device path type (such as 0x01 - Hardware Device Path)
 * Sub-Type 1               1               Sub-Type
 * Length   2               2               Length of this structure in bytes. Length is 4+n bytes
 * Data     4               n               Specific Device Path data
 * <p>
 * EFI_DEVICE_PATH_PROTOCOL:
 * #define EFI_DEVICE_PATH_PROTOCOL_GUID \09576e91-6d3f-11d2-8e39-00a0c969723b
 * typedef struct _EFI_DEVICE_PATH_PROTOCOL {
 * UINT8 Type;
 * UINT8 SubType;
 * UINT8 Length[2];
 * } EFI_DEVICE_PATH_PROTOCOL;
 * <p>
 * Where Type is defined in the UEFI spec section 10:
 * Type 0x01  Hardware Device Path
 * Type 0x02  ACPI Device Path
 * Type 0x03  Messaging Device Path
 * Type 0x04  Media Device Path
 * Type 0x05  BIOS Boot Specification Device Path
 * Type 0x7F  End of Hardware Device Path
 * Each Type has a Subtype that may or may not be defined in the section
 * <p>
 * Only a few of the SubTypes have been implemented as there are many,
 * but only those that were reported using the test devices at hand.
 * Without test patterns, the processing may lead to an un-handled exception
 */
public class UefiDevicePath {
    /**
     * UEFI Device path type.
     */
    @Getter
    private String type = "";
    /**
     * UEFI Device path subtype.
     */
    private String subType = "";
    /**
     * UEFI Device path human-readable description.
     */
    private String devPathInfo = "";
    /**
     * UEFI Device path length.
     */
    @Getter
    private int length = 0;

    /**
     * UEFI Device path constructor.
     *
     * @param path byte array holding device path data
     */
    public UefiDevicePath(final byte[] path) {
        devPathInfo = processDevPath(path);
        byte[] lengthBytes = new byte[UefiConstants.SIZE_2];
        System.arraycopy(path, UefiConstants.OFFSET_2, lengthBytes, 0, UefiConstants.OFFSET_2);
        length = HexUtils.leReverseInt(lengthBytes);
    }

    /**
     * Converts from a char array to byte array.
     * Removes the upper byte (typically set to 0) of each char.
     *
     * @param data Character array.
     * @return byte array.
     */
    public static byte[] convertChar16tobyteArray(final byte[] data) {
        byte[] hexdata = new byte[data.length];
        int j = 0;
        for (int i = 0; i < data.length; i = i + UefiConstants.SIZE_2) {
            hexdata[j++] = data[i];
        }
        return hexdata;
    }

    /**
     * Returns the UEFI device subtype.
     *
     * @return uefi subtype
     */
    public String getSubType() {
        return subType.trim();
    }

    /**
     * Processes the UEFI device path.
     * UEFI device path is a collection of EFI_DEVICE_PATH_PROTOCOL structures of variable length.
     * length must be calculated for each device path and used as an offset.
     * devPath is terminated by 07f and 0xff per the UEFi spec.
     *
     * @param path byte array holding the Device path
     * @return Human readable string containing the device path description.
     * @throws java.io.UnsupportedEncodingException
     */
    private String processDevPath(final byte[] path) {
        StringBuilder pInfo = new StringBuilder();
        int devLength = 0;
        int pathOffset = 0;
        int devCount = 0;
        while (true) {
            Byte devPath = Byte.valueOf(path[pathOffset]);
            if ((devPath.intValue() == UefiConstants.TERMINATOR)
                    || (devPath.intValue() == UefiConstants.END_FLAG)) {
                break;
            }
            pInfo.append(processDev(path, pathOffset));
            devLength = path[pathOffset + UefiConstants.OFFSET_3] * UefiConstants.SIZE_256
                    + path[pathOffset + UefiConstants.OFFSET_2];
            pathOffset = pathOffset + devLength;
            if (pathOffset >= path.length) {
                break;
            }
        }
        return pInfo.toString();
    }

    /**
     * Processes a specific UEFI device path, only limited set of types and subtypes are supported.
     * Current types processed include Hardware Device Path, ACPI Device Path,
     * Messaging Device Path, and Media Device Path.
     *
     * @param path   path
     * @param offset offset
     * @return human-readable string representing the UEFI device path
     * @throws java.io.UnsupportedEncodingException
     */
    private String processDev(final byte[] path, final int offset) {
        String devInfo = "      ";
        int devPath = path[offset];
        byte unknownSubType = path[offset + UefiConstants.OFFSET_1];
        switch (path[offset]) {
            case UefiConstants.DEV_HW:
                type = "Hardware Device Path";
                if (devPath == UefiConstants.DEVPATH_HARWARE) {
                    devInfo += type + ":\n" + pciSubType(path, offset);
                }
                break;
            case UefiConstants.DEV_ACPI:
                type = "ACPI Device Path";
                devInfo += type + ":\n" + acpiSubType(path, offset);
                break;
            case UefiConstants.DEV_MSG:
                type = "Messaging Device Path";
                if (path[offset + UefiConstants.OFFSET_1] == UefiConstants.DEV_SUB_SATA) {
                    devInfo += type + ":\n" + sataSubType(path, offset);
                } else if (path[offset + UefiConstants.OFFSET_1] == UefiConstants.DEV_SUB_NVM) {
                    devInfo += type + ":\n" + nvmSubType(path, offset);
                } else if (path[offset + UefiConstants.OFFSET_1] == UefiConstants.DEV_SUB_USB) {
                    devInfo += type + ":\n" + usbSubType(path, offset);
                } else {
                    devInfo += "UEFI Messaging Device Path Type " + Integer.valueOf(unknownSubType) + "\n";
                }
                break;
            case UefiConstants.DEV_MEDIA:
                type = "Media Device Path";
                if (path[offset + UefiConstants.OFFSET_1] == 0x01) {
                    devInfo += type + ":\n" + hardDriveSubType(path, offset);
                } else if (path[offset + UefiConstants.OFFSET_1] == UefiConstants.DEVPATH_VENDOR) {
                    devInfo += type + ":\n" + vendorSubType(path, offset);
                } else if (path[offset + UefiConstants.OFFSET_1] == UefiConstants.DEVPATH_FILE) {
                    devInfo += type + ":\n" + filePathSubType(path, offset);
                } else if (path[offset + UefiConstants.OFFSET_1] == UefiConstants.DEVPATH_PWIG_FILE) {
                    devInfo += type + ":\n" + piwgFirmVolFile(path, offset);
                } else if (path[offset + UefiConstants.OFFSET_1] == UefiConstants.DEVPATH_PWIG_VOL) {
                    devInfo += type + ":\n" + piwgFirmVolPath(path, offset);
                } else {
                    devInfo += "UEFI Media Device Path Type " + Integer.valueOf(unknownSubType) + "\n";
                }
                break;
            case UefiConstants.DEV_BIOS:
                type = "BIOS Device Path";
                devInfo += type + ":\n" + biosDevicePath(path, offset);
                break;
            case UefiConstants.TERMINATOR:
                devInfo += "End of Hardware Device Path\n";
                break;
            default:
                devInfo += "UEFI Device Path Type " + Integer.valueOf(unknownSubType) + "\n";
        }
        return devInfo;
    }

    /**
     * processes the ACPI UEFI device subtype.
     *
     * @param path   path
     * @param offset offset
     * @return acpi device info
     */
    private String acpiSubType(final byte[] path, final int offset) {
        subType = "        Sub Type = ACPI\n";
        switch (path[offset + UefiConstants.OFFSET_1]) {
            case 0x01:  // standard version
                subType += acpiShortSubType(path, offset);
                break;
            case 0x02:
                subType = "(expanded version):\n";
                // tbd
                break;
            default:
                subType = "Invalid ACPI Device Path sub type\n";
        }
        return subType;
    }

    /**
     * Processes the ACPI short subtype.
     *
     * @param path   path
     * @param offset offset
     * @return short acpi info.
     */
    private String acpiShortSubType(final byte[] path, final int offset) {
        subType = "";
        byte[] hid = new byte[UefiConstants.SIZE_4];
        System.arraycopy(path, UefiConstants.OFFSET_4 + offset, hid, 0, UefiConstants.SIZE_4);
        subType += "        _HID = " + HexUtils.byteArrayToHexString(hid) + "\n";
        System.arraycopy(path, 2 * UefiConstants.SIZE_4 + offset, hid, 0, UefiConstants.SIZE_4);
        String uid = HexUtils.byteArrayToHexString(hid);
        if (uid.contains("00000000")) {
            uid = "No _UID exists for this device";
        }
        subType += "        _UID = " + uid + "\n";
        return subType;
    }

    /**
     * Processes the PCI subType.
     *
     * @param path   path
     * @param offset offset
     * @return pci device info.
     */
    private String pciSubType(final byte[] path, final int offset) {
        subType = "        Sub Type = PCI\n";
        subType += "        PCI Function Number = ";
        subType += String.format("0x%x", path[offset + UefiConstants.SIZE_4]);
        subType += "\n        PCI Device Number = ";
        subType += String.format("0x%x", path[offset + UefiConstants.SIZE_5]);
     //   subType += "\n";
        return subType;
    }

    /**
     * processes the SATA subtype.
     *
     * @param path   path
     * @param offset offset
     * @return SATA drive info.
     */
    private String sataSubType(final byte[] path, final int offset) {
        subType = "        Sub Type = SATA\n";
        subType += "        SATA: HBA Port Number = ";
        byte[] data = new byte[UefiConstants.SIZE_2];
        System.arraycopy(path, UefiConstants.OFFSET_4 + offset, data, 0, UefiConstants.SIZE_2);
        subType += HexUtils.byteArrayToHexString(data);
        System.arraycopy(path, UefiConstants.OFFSET_6 + offset, data, 0, UefiConstants.SIZE_2);
        subType += " Port Multiplier  = " + HexUtils.byteArrayToHexString(data);
        System.arraycopy(path, UefiConstants.OFFSET_8 + offset, data, 0, UefiConstants.SIZE_2);
        subType += " Logical Unit Number  = " + HexUtils.byteArrayToHexString(data);
      //  subType += "\n";
        return subType;
    }

    /**
     * Processes the hard drive subtype.
     *
     * @param path   path
     * @param offset offset
     * @return hard drive info.
     */
    private String hardDriveSubType(final byte[] path, final int offset) {
        subType = "        Sub Type = Hard Drive\n";
        subType += "        Partition Number = ";
        byte[] partnumber = new byte[UefiConstants.SIZE_4];
        System.arraycopy(path, UefiConstants.OFFSET_4 + offset, partnumber,
                0, UefiConstants.SIZE_4);
        subType += HexUtils.byteArrayToHexString(partnumber);
        byte[] data = new byte[UefiConstants.SIZE_8];
        System.arraycopy(path, UefiConstants.OFFSET_8 + offset, data, 0,
                UefiConstants.SIZE_8);
        subType += "\n        Partition Start = " + HexUtils.byteArrayToHexString(data);
        System.arraycopy(path, UefiConstants.OFFSET_16 + offset, data, 0,
                UefiConstants.SIZE_8);
        subType += "\n        Partition Size = " + HexUtils.byteArrayToHexString(data);
        byte[] signature = new byte[UefiConstants.SIZE_16];
        System.arraycopy(path, UefiConstants.OFFSET_24 + offset, signature, 0,
                UefiConstants.SIZE_16);
        subType += "\n        Partition Signature = ";
        if (path[UefiConstants.OFFSET_41 + offset] == UefiConstants.DRIVE_SIG_NONE) {
            subType += "None";
        } else if (path[UefiConstants.OFFSET_41 + offset] == UefiConstants.DRIVE_SIG_32BIT) {
            subType += HexUtils.byteArrayToHexString(signature);
        } else if (path[UefiConstants.OFFSET_41 + offset] == UefiConstants.DRIVE_SIG_GUID) {
            UefiGuid guid = new UefiGuid(signature);
            subType += guid.toString();
        } else {
            subType += "invalid partition signature type";
        }
        subType += "\n        Partition Format = ";
        if (path[UefiConstants.OFFSET_40 + offset] == UefiConstants.DRIVE_TYPE_PC_AT) {
            subType += "PC-AT compatible legacy MBR";
        } else if (path[UefiConstants.OFFSET_40 + offset] == UefiConstants.DRIVE_TYPE_GPT) {
            subType += "GUID Partition Table";
        } else {
            subType += "Invalid partition table type";
        }
    //    subType += "\n";
        return subType;
    }

    /**
     * Process the File path subtype.
     *
     * @param path   path
     * @param offset offset
     * @return file path info.
     */
    private String filePathSubType(final byte[] path, final int offset) {
        subType = "        Sub Type = File Path\n";
        subType += "        File Path = ";
        byte[] lengthBytes = new byte[UefiConstants.SIZE_2];
        System.arraycopy(path, 2 + offset, lengthBytes, 0, UefiConstants.SIZE_2);
        int subTypeLength = HexUtils.leReverseInt(lengthBytes);
        byte[] filePath = new byte[subTypeLength];
        System.arraycopy(path, UefiConstants.OFFSET_4 + offset, filePath,
                0, subTypeLength);
        byte[] fileName = convertChar16tobyteArray(filePath);
        subType += new String(fileName, StandardCharsets.UTF_8);
      //  subType += "\n";
        return subType;
    }

    /**
     * Process a vendor subtype on a Media Type.
     * Length of this structure in bytes. Length is 20 + n bytes
     * Vendor-assigned GUID that defines the data that follows.
     * Vendor-defined variable size data.
     *
     * @param path   path
     * @param offset offset
     * @return vendor device info.
     */
    private String vendorSubType(final byte[] path, final int offset) {
        subType = "        Sub Type = Vendor\n";
        subType += "        Vendor Subtype GUID = ";
        byte[] lengthBytes = new byte[UefiConstants.SIZE_2];
        System.arraycopy(path, UefiConstants.OFFSET_2 + offset, lengthBytes,
                0, UefiConstants.SIZE_2);
        int subTypeLength = HexUtils.leReverseInt(lengthBytes);
        byte[] guidData = new byte[UefiConstants.SIZE_16];
        System.arraycopy(path, UefiConstants.OFFSET_4 + offset, guidData,
                0, UefiConstants.SIZE_16);
        UefiGuid guid = new UefiGuid(guidData);
        subType += guid + " ";
        if (subTypeLength - UefiConstants.SIZE_16 > 0) {
            byte[] vendorData = new byte[subTypeLength - UefiConstants.SIZE_16];
            System.arraycopy(path, UefiConstants.OFFSET_20
                    + offset, vendorData, 0, subTypeLength - UefiConstants.SIZE_16);
            subType += " : Vendor Data = " + HexUtils.byteArrayToHexString(vendorData);
        } else {
            subType += " : No Vendor Data present";
        }
    //    subType += "\n";
        return subType;
    }

    /**
     * Returns USB device info.
     * UEFI Specification, Version 2.8.
     *
     * @param path   path
     * @param offset offset
     * @return USB device info.
     */
    private String usbSubType(final byte[] path, final int offset) {
        subType = "        Sub Type = USB\n";
        subType += "        port = " + Integer.valueOf(path[offset + UefiConstants.OFFSET_4]);
        subType += " interface = " + Integer.valueOf(path[offset + UefiConstants.OFFSET_5]);
        byte[] lengthBytes = new byte[UefiConstants.SIZE_2];
        System.arraycopy(path, UefiConstants.OFFSET_2 + offset, lengthBytes,
                0, UefiConstants.SIZE_2);
        int subTypeLength = HexUtils.leReverseInt(lengthBytes);
        byte[] usbData = new byte[subTypeLength];
        System.arraycopy(path, UefiConstants.OFFSET_4 + offset, usbData,
                0, subTypeLength);
     //   subType += "\n";
        // Todo add further USB processing ...
        return subType;
    }

    /**
     * Returns NVM device info.
     * UEFI Specification, Version 2.8.
     * Name space Identifier (NSID) and IEEE Extended Unique Identifier (EUI-64):
     * See Links to UEFI Related Documents
     * (http://uefi.org/uefi under the headings NVM Express Specification.
     *
     * @param path   path
     * @param offset offset
     * @return NVM device info.
     */
    private String nvmSubType(final byte[] path, final int offset) {
        subType = "        Sub Type = NVM\n";
        subType += "        NVM Express Namespace = ";
        byte[] lengthBytes = new byte[UefiConstants.SIZE_2];
        System.arraycopy(path, UefiConstants.OFFSET_2 + offset, lengthBytes,
                0, UefiConstants.SIZE_2);
        int subTypeLength = HexUtils.leReverseInt(lengthBytes);
        byte[] nvmData = new byte[subTypeLength];
        System.arraycopy(path, UefiConstants.OFFSET_4 + offset, nvmData,
                0, subTypeLength);
        subType += HexUtils.byteArrayToHexString(nvmData);
    //    subType += "\n";
        return subType;
    }

    /**
     * BIOS Device Type definition.
     * From Appendix A of the BIOS Boot Specification.
     * Only processes the Device type.
     * Status bootHandler pointer, and description String pointer are ignored.
     *
     * @param path   byte array holding the device path.
     * @param offset offset
     * @return String that represents the UEFI defined BIOS Device Type.
     */
    private String biosDevicePath(final byte[] path, final int offset) {
        subType = "        Sub Type = Bios Device Path\n";
        subType += "        Legacy BIOS : Type = ";
        Byte pathType = Byte.valueOf(path[offset + 1]);
        switch (pathType.intValue()) {
            case UefiConstants.DEVPATH_BIOS_RESERVED:
                subType += "Reserved";
                break;
            case UefiConstants.DEVPATH_BIOS_FLOPPY:
                subType += "Floppy";
                break;
            case UefiConstants.DEVPATH_BIOS_HD:
                subType += "Hard Disk";
                break;
            case UefiConstants.DEVPATH_BIOS_CD:
                subType += "CD-ROM";
                break;
            case UefiConstants.DEVPATH_BIOS_PCM:
                subType += "PCMCIA";
                break;
            case UefiConstants.DEVPATH_BIOS_USB:
                subType += "USB";
                break;
            case UefiConstants.DEVPATH_BIOS_EN:
                subType += "Embedded network";
                break;
            case UefiConstants.DEVPATH_BIOS_BEV:
                subType +=
                        "Bootstrap Entry Vector (BEV) from an Option ROM";
                break;
            default:
                subType += "Unknown";
                break;
        }
//subType += "\n";
        return subType;
    }

    /**
     * Returns PIWG firmware volume info.
     * UEFI Specification, Version 2.8.
     * PIWG Firmware File Section 10.3.5.6:
     * Contents are defined in the UEFI PI Specification.
     *
     * @param path   path
     * @param offset offset
     * @return String that represents the PIWG Firmware Volume Path
     */
    private String piwgFirmVolFile(final byte[] path, final int offset) {
        subType = "        Sub Type = PIWG Firmware Volume File\n";
        byte[] guidData = new byte[UefiConstants.SIZE_16];
        System.arraycopy(path, UefiConstants.OFFSET_4 + offset, guidData,
                0, UefiConstants.SIZE_16);
        UefiGuid guid = new UefiGuid(guidData);
        subType += guid.toString();
    //    subType += "\n";
        return subType;
    }

    /**
     * Returns PIWG firmware file info.
     * UEFI Specification, Version 2.8.
     * PIWG Firmware Volume Section 10.3.5.7:
     * Contents are defined in the UEFI PI Specification.
     *
     * @param path   path
     * @param offset offset
     * @return String that represents the PIWG Firmware Volume Path
     */
    private String piwgFirmVolPath(final byte[] path, final int offset) {
        subType = "        Sub Type = PIWG Firmware Volume Path\n";
        byte[] guidData = new byte[UefiConstants.SIZE_16];
        System.arraycopy(path, UefiConstants.OFFSET_4 + offset, guidData,
                0, UefiConstants.SIZE_16);
        UefiGuid guid = new UefiGuid(guidData);
        subType += guid.toString();
   ///     subType += "\n";
        return subType;
    }

    /**
     * Returns a string that represents the UEFi Device path.
     *
     * @return UEFi Device path.
     */
    public String toString() {
        return devPathInfo;
    }
}
