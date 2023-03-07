package hirs.utils.tpm.eventlog.uefi;

import hirs.utils.HexUtils;
import lombok.Getter;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

/**
 * Class to process EFI_DEVICE_PATH_PROTOCOL which is referred to as the UEFI_DEVICE_PATH
 * <p>
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
 * Each Type has a sub-type that may or may no be defined in the section
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
     * UEFI Device path sub-type.
     */
    private String subType = "";
    /**
     * UEFI Device path human readable description.
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
     * @throws java.io.UnsupportedEncodingException if path byte array contains unexpected values
     */
    public UefiDevicePath(final byte[] path) throws UnsupportedEncodingException {
        devPathInfo = processDevPath(path);
        byte[] lengthBytes = new byte[UefiConstants.SIZE_2];
        System.arraycopy(path, UefiConstants.OFFSET_2, lengthBytes, 0, UefiConstants.OFFSET_2);
        length = HexUtils.leReverseInt(lengthBytes);
    }

    /**
     * Returns the UEFI device sub-type.
     *
     * @return uefi sub-type
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
    private String processDevPath(final byte[] path) throws UnsupportedEncodingException {
        StringBuilder pInfo = new StringBuilder();
        int devLength = 0, pathOffset = 0, devCount = 0;
        while (true) {
            Byte devPath = Byte.valueOf(path[pathOffset]);
            if ((devPath.intValue() == UefiConstants.TERMINATOR)
                    || (devPath.intValue() == UefiConstants.END_FLAG)) {
                break;
            }
            if (devCount++ > 0) {
                pInfo.append("\n");
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
     * @param path
     * @param offset
     * @return human readable string representing the UEFI device path
     * @throws java.io.UnsupportedEncodingException
     */
    private String processDev(final byte[] path, final int offset)
            throws UnsupportedEncodingException {
        String devInfo = "    ";
        int devPath = path[offset];
        byte unknownSubType = path[offset + UefiConstants.OFFSET_1];
        switch (path[0 + offset]) {
            case UefiConstants.DEV_HW:
                type = "Hardware Device Path";
                if (devPath == UefiConstants.DEVPATH_HARWARE) {
                    devInfo += type + ": " + pciSubType(path, offset);
                }
                break;
            case UefiConstants.DEV_ACPI:
                type = "ACPI Device Path";
                devInfo += type + ": " + acpiSubType(path, offset);
                break;
            case UefiConstants.DEV_MSG:
                type = "Messaging Device Path";
                if (path[offset + UefiConstants.OFFSET_1] == UefiConstants.DEV_SUB_SATA) {
                    devInfo += type + ": " + sataSubType(path, offset);
                } else if (path[offset + UefiConstants.OFFSET_1] == UefiConstants.DEV_SUB_NVM) {
                    devInfo += type + ": " + nvmSubType(path, offset);
                } else if (path[offset + UefiConstants.OFFSET_1] == UefiConstants.DEV_SUB_USB) {
                    devInfo += type + ": " + usbSubType(path, offset);
                } else {
                    devInfo += "UEFI Messaging Device Path Type " + Integer.valueOf(unknownSubType);
                }
                break;
            case UefiConstants.DEV_MEDIA:
                type = "Media Device Path";
                if (path[offset + UefiConstants.OFFSET_1] == 0x01) {
                    devInfo += type + ": " + hardDriveSubType(path, offset);
                } else if (path[offset + UefiConstants.OFFSET_1] == UefiConstants.DEVPATH_VENDOR) {
                    devInfo += type + ": " + vendorSubType(path, offset);
                } else if (path[offset + UefiConstants.OFFSET_1] == UefiConstants.DEVPATH_FILE) {
                    devInfo += type + ": " + filePathSubType(path, offset);
                } else if (path[offset + UefiConstants.OFFSET_1] == UefiConstants.DEVPATH_PWIG_FILE) {
                    devInfo += type + ": " + piwgFirmVolFile(path, offset);
                } else if (path[offset + UefiConstants.OFFSET_1] == UefiConstants.DEVPATH_PWIG_VOL) {
                    devInfo += type + ": " + piwgFirmVolPath(path, offset);
                } else {
                    devInfo += "UEFI Media Device Path Type " + Integer.valueOf(unknownSubType);
                }
                break;
            case UefiConstants.DEV_BIOS:
                type = "BIOS Device Path";
                devInfo += type + ": " + biosDevicePath(path, offset);
                break;
            case UefiConstants.TERMINATOR:
                devInfo += "End of Hardware Device Path";
                break;
            default:
                devInfo += "UEFI Device Path Type " + Integer.valueOf(unknownSubType);
        }
        return devInfo;
    }

    /**
     * processes the ACPI UEFI device subtype.
     *
     * @param path
     * @param offset
     * @return acpi device info
     */
    private String acpiSubType(final byte[] path, final int offset) {
        subType = "";
        switch (path[offset + UefiConstants.OFFSET_1]) {
            case 0x01:
                subType = "(Short): ";
                subType += acpiShortSubType(path, offset);
                break;
            case 0x02:
                subType = "Expanded ACPI Device Path";
                break;
            default:
                subType = "Invalid ACPI Device Path sub type";
        }
        return subType;
    }

    /**
     * Processes the ACPI short subtype.
     *
     * @param path
     * @param offset
     * @return short acpi info.
     */
    private String acpiShortSubType(final byte[] path, final int offset) {
        subType = "";
        byte[] hid = new byte[UefiConstants.SIZE_4];
        System.arraycopy(path, UefiConstants.OFFSET_4 + offset, hid, 0, UefiConstants.SIZE_4);
        subType += "_HID = " + HexUtils.byteArrayToHexString(hid);
        System.arraycopy(path, 2 * UefiConstants.SIZE_4 + offset, hid, 0, UefiConstants.SIZE_4);
        subType += "_UID = " + HexUtils.byteArrayToHexString(hid);
        return subType;
    }

    /**
     * Processes the PCI subType.
     *
     * @param path
     * @param offset
     * @return pci device info.
     */
    private String pciSubType(final byte[] path, final int offset) {
        subType = "PCI: PCI Function Number = ";
        subType += String.format("0x%x", path[offset + UefiConstants.SIZE_4]);
        subType += " PCI Device Number = ";
        subType += String.format("0x%x", path[offset + UefiConstants.SIZE_5]);
        return subType;
    }

    /**
     * processes the SATA sub type.
     *
     * @param path
     * @param offset
     * @return SATA drive info.
     */
    private String sataSubType(final byte[] path, final int offset) {
        subType = "SATA: HBA Port Number = ";
        byte[] data = new byte[UefiConstants.SIZE_2];
        System.arraycopy(path, UefiConstants.OFFSET_4 + offset, data, 0, UefiConstants.SIZE_2);
        subType += HexUtils.byteArrayToHexString(data);
        System.arraycopy(path, UefiConstants.OFFSET_6 + offset, data, 0, UefiConstants.SIZE_2);
        subType += " Port Multiplier  = " + HexUtils.byteArrayToHexString(data);
        System.arraycopy(path, UefiConstants.OFFSET_8 + offset, data, 0, UefiConstants.SIZE_2);
        subType += " Logical Unit Number  = " + HexUtils.byteArrayToHexString(data);
        return subType;
    }

    /**
     * Processes the hard drive sub type.
     *
     * @param path
     * @param offset
     * @return hard drive info.
     */
    private String hardDriveSubType(final byte[] path, final int offset) {
        subType = "Partition Number = ";
        byte[] partnumber = new byte[UefiConstants.SIZE_4];
        System.arraycopy(path, UefiConstants.OFFSET_4 + offset, partnumber, 0, UefiConstants.SIZE_4);
        subType += HexUtils.byteArrayToHexString(partnumber);
        byte[] data = new byte[UefiConstants.SIZE_8];
        System.arraycopy(path, UefiConstants.OFFSET_8 + offset, data, 0, UefiConstants.SIZE_8);
        subType += " Partition Start = " + HexUtils.byteArrayToHexString(data);
        System.arraycopy(path, UefiConstants.OFFSET_16 + offset, data, 0, UefiConstants.SIZE_8);
        subType += " Partition Size = " + HexUtils.byteArrayToHexString(data);
        byte[] signature = new byte[UefiConstants.SIZE_16];
        System.arraycopy(path, UefiConstants.OFFSET_24 + offset, signature, 0, UefiConstants.SIZE_16);
        subType += "\n       Partition Signature = ";
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
        subType += " Partition Format = ";
        if (path[UefiConstants.OFFSET_40 + offset] == UefiConstants.DRIVE_TYPE_PC_AT) {
            subType += " PC-AT compatible legacy MBR";
        } else if (path[UefiConstants.OFFSET_40 + offset] == UefiConstants.DRIVE_TYPE_GPT) {
            subType += " GUID Partition Table";
        } else {
            subType += " Invalid partition table type";
        }
        return subType;
    }

    /**
     * Process the File path sub type.
     *
     * @param path
     * @param offset
     * @return file path info.
     */
    private String filePathSubType(final byte[] path, final int offset) {
        subType = "File Path = ";
        byte[] lengthBytes = new byte[UefiConstants.SIZE_2];
        System.arraycopy(path, 2 + offset, lengthBytes, 0, UefiConstants.SIZE_2);
        int subTypeLength = HexUtils.leReverseInt(lengthBytes);
        byte[] filePath = new byte[subTypeLength];
        System.arraycopy(path, UefiConstants.OFFSET_4 + offset, filePath, 0, subTypeLength);
        byte[] fileName = convertChar16tobyteArray(filePath);
        subType += new String(fileName, StandardCharsets.UTF_8);
        return subType;
    }

    /**
     * Process a vendor sub-type on a Media Type.
     * Length of this structure in bytes. Length is 20 + n bytes
     * Vendor-assigned GUID that defines the data that follows.
     * Vendor-defined variable size data.
     *
     * @param path
     * @param offset
     * @return vendor device info.
     */
    private String vendorSubType(final byte[] path, final int offset) {
        subType = "Vendor Subtype GUID = ";
        byte[] lengthBytes = new byte[UefiConstants.SIZE_2];
        System.arraycopy(path, UefiConstants.OFFSET_2 + offset, lengthBytes, 0, UefiConstants.SIZE_2);
        int subTypeLength = HexUtils.leReverseInt(lengthBytes);
        byte[] guidData = new byte[UefiConstants.SIZE_16];
        System.arraycopy(path, UefiConstants.OFFSET_4 + offset, guidData, 0, UefiConstants.SIZE_16);
        UefiGuid guid = new UefiGuid(guidData);
        subType += guid.toString() + " ";
        if (subTypeLength - UefiConstants.SIZE_16 > 0) {
            byte[] vendorData = new byte[subTypeLength - UefiConstants.SIZE_16];
            System.arraycopy(path, UefiConstants.OFFSET_20
                    + offset, vendorData, 0, subTypeLength - UefiConstants.SIZE_16);
            subType += " : Vendor Data = " + HexUtils.byteArrayToHexString(vendorData);
        } else {
            subType += " : No Vendor Data pesent";
        }
        return subType;
    }

    /**
     * Returns USB device info.
     * UEFI Specification, Version 2.8.
     *
     * @param path
     * @param offset
     * @return USB device info.
     */
    private String usbSubType(final byte[] path, final int offset) {
        subType = " USB ";
        subType += " port = " + Integer.valueOf(path[offset + UefiConstants.OFFSET_4]);
        subType += " interface = " + Integer.valueOf(path[offset + UefiConstants.OFFSET_5]);
        byte[] lengthBytes = new byte[UefiConstants.SIZE_2];
        System.arraycopy(path, UefiConstants.OFFSET_2 + offset, lengthBytes, 0, UefiConstants.SIZE_2);
        int subTypeLength = HexUtils.leReverseInt(lengthBytes);
        byte[] usbData = new byte[subTypeLength];
        System.arraycopy(path, UefiConstants.OFFSET_4 + offset, usbData, 0, subTypeLength);
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
     * @param path
     * @param offset
     * @return NVM device info.
     */
    private String nvmSubType(final byte[] path, final int offset) {
        subType = "NVM Express Namespace = ";
        byte[] lengthBytes = new byte[UefiConstants.SIZE_2];
        System.arraycopy(path, UefiConstants.OFFSET_2 + offset, lengthBytes, 0, UefiConstants.SIZE_2);
        int subTypeLength = HexUtils.leReverseInt(lengthBytes);
        byte[] nvmData = new byte[subTypeLength];
        System.arraycopy(path, UefiConstants.OFFSET_4 + offset, nvmData, 0, subTypeLength);
        subType += HexUtils.byteArrayToHexString(nvmData);
        return subType;
    }

    /**
     * BIOS Device Type definition.
     * From Appendix A of the BIOS Boot Specification.
     * Only processes the Device type.
     * Status bootHandler pointer, and description String pointer are ignored.
     *
     * @param path byte array holding the device path.
     * @return String that represents the UEFI defined BIOS Device Type.
     */
    private String biosDevicePath(final byte[] path, final int offset) {
        subType = "Legacy BIOS : Type = ";
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
        return subType;
    }

    /**
     * Returns PIWG firmware volume info.
     * UEFI Specification, Version 2.8.
     * PIWG Firmware File Section 10.3.5.6:
     * Contents are defined in the UEFI PI Specification.
     *
     * @param path
     * @param offset
     * @return String that represents the PIWG Firmware Volume Path
     */
    private String piwgFirmVolFile(final byte[] path, final int offset) {
        subType = "PIWG Firmware File ";
        byte[] guidData = new byte[UefiConstants.SIZE_16];
        System.arraycopy(path, UefiConstants.OFFSET_4 + offset, guidData, 0, UefiConstants.SIZE_16);
        UefiGuid guid = new UefiGuid(guidData);
        subType += guid.toString();
        return subType;
    }

    /**
     * Returns PIWG firmware file info.
     * UEFI Specification, Version 2.8.
     * PIWG Firmware Volume Section 10.3.5.7:
     * Contents are defined in the UEFI PI Specification.
     *
     * @param path
     * @param offset
     * @return String that represents the PIWG Firmware Volume Path
     */
    private String piwgFirmVolPath(final byte[] path, final int offset) {
        subType = "PIWG Firmware Volume ";
        byte[] guidData = new byte[UefiConstants.SIZE_16];
        System.arraycopy(path, UefiConstants.OFFSET_4 + offset, guidData, 0, UefiConstants.SIZE_16);
        UefiGuid guid = new UefiGuid(guidData);
        subType += guid.toString();
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
}
