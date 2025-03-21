package hirs.utils.tpm.eventlog.events;

import hirs.utils.HexUtils;
import hirs.utils.tpm.eventlog.uefi.UefiConstants;
import hirs.utils.tpm.eventlog.uefi.UefiDevicePath;
import lombok.Getter;

import java.nio.charset.StandardCharsets;

/**
 * Abstract class to process the DEVICE_SECURITY_EVENT_DATA_HEADER or ..HEADER2 per PFP.
 * The first 16 bytes of the event data header MUST be a String based identifier (Signature),
 * NUL-terminated, per PFP. The only currently defined Signature is "SPDM Device Sec",
 * which implies the data is a DEVICE_SECURITY_EVENT_DATA or ..DATA2.
 * <p>
 * HEADERS defined by PFP v1.06 Rev 52.
 * Certain fields are common to both ..HEADER and ..HEADER2, and are noted below the structures.
 * <p>
 * typedef struct tdDEVICE_SECURITY_EVENT_DATA_HEADER {
 * .     UINT8                           Signature[16];
 * .     UINT16                          Version;
 * .     UINT16                          Length;
 * .     UINT32                          SpdmHashAlg;
 * .     UINT32                          DeviceType;
 * .     SPDM_MEASUREMENT_BLOCK          SpdmMeasurementBlock;
 * .     UINT64                          DevicePathLength;
 * .     UNIT8                           DevicePath[DevicePathLength]
 * } DEVICE_SECURITY_EVENT_DATA_HEADER;
 * <p>
 * typedef struct tdDEVICE_SECURITY_EVENT_DATA_HEADER2 {        - NOT IMPLEMENTED YET
 * .     UINT8                           Signature[16];
 * .     UINT16                          Version;
 * .     UINT8                           AuthState;
 * .     UINT8                           Reserved;
 * .     UINT32                          Length;
 * .     UINT32                          DeviceType;
 * .     UINT32                          SubHeaderType;
 * .     UINT32                          SubHeaderLength;
 * .     UINT32                          SubHeaderUID;
 * .     UINT64                          DevicePathLength;
 * .     UNIT8                           DevicePath[DevicePathLength]
 * } DEVICE_SECURITY_EVENT_DATA_HEADER2;
 * <p>
 * Fields common to both ..HEADER and ..HEADER2:
 * .  Signature
 * .  Version
 * .  DeviceType
 * .  DevicePathLength
 * .  DevicePath
 * <p>
 */
public abstract class DeviceSecurityEventHeader {

    /**
     * UEFI Device Path Length.
     */
    @Getter
    private static final int DEVICE_PATH_LENGTH = 0;
    /**
     * Contains the size (in bytes) of the header.
     */
    @Getter
    private Integer dsedHeaderLength = 0;
    /**
     * Signature (text) data.
     */
    @Getter
    private String signature = "";
    /**
     * Version determines data structure used (..DATA or ..DATA2).
     * This determines whether ..HEADER or ..HEADER2 is used.
     */
    @Getter
    private String version = "";
    /**
     * Device type.
     */
    @Getter
    private int deviceType = -1;
    /**
     * UEFI Device path.
     */
    @Getter
    private UefiDevicePath devicePath = null;

    /**
     * DeviceSecurityEventDataHeader Constructor.
     *
     * @param dSEDbytes byte array holding the DeviceSecurityEventData.
     */
    public DeviceSecurityEventHeader(final byte[] dSEDbytes) {

        byte[] signatureBytes = new byte[UefiConstants.SIZE_16];
        System.arraycopy(dSEDbytes, 0, signatureBytes, 0, UefiConstants.SIZE_16);
        signature = new String(signatureBytes, StandardCharsets.UTF_8);
        signature = signature.replaceAll("[^\\P{C}\t\r\n]", ""); // remove null characters

        byte[] versionBytes = new byte[UefiConstants.SIZE_2];
        System.arraycopy(dSEDbytes, UefiConstants.OFFSET_16, versionBytes, 0,
                UefiConstants.SIZE_2);
        version = HexUtils.byteArrayToHexString(versionBytes);
    }

    /**
     * Parse the device type from the Device Security Event Data Header/Header2.
     *
     * @param dsedBytes byte array holding the DeviceSecurityEventData/Data2.
     * @param startByte starting byte of device type (depends on header fields before it).
     */
    public void extractDeviceType(final byte[] dsedBytes, final int startByte) {

        // get the device type ID
        byte[] deviceTypeBytes = new byte[UefiConstants.SIZE_4];
        System.arraycopy(dsedBytes, startByte, deviceTypeBytes, 0,
                UefiConstants.SIZE_4);
        deviceType = HexUtils.leReverseInt(deviceTypeBytes);
    }

    /**
     * Parse the device path from the Device Security Event Data Header/Header2.
     * Also, determine final length of header (will be used to extract the next data structure).
     *
     * @param dsedBytes byte array holding the DeviceSecurityEventData/Data2.
     * @param startByte starting byte of device path (depends on header fields before it).
     */
    public void extractDevicePathAndFinalSize(final byte[] dsedBytes, final int startByte) {

        int startByteUpdated = startByte;

        // get the device path length
        final int devicePathLengthBytesSize = 8;
        byte[] devicePathLengthBytes = new byte[devicePathLengthBytesSize];
        System.arraycopy(dsedBytes, startByteUpdated, devicePathLengthBytes, 0, devicePathLengthBytesSize);
        int retrievedDevicePathLength = HexUtils.leReverseInt(devicePathLengthBytes);

        // get the device path
        if (retrievedDevicePathLength > 0) {
            final int startByteUpdatedOffset = 8;
            startByteUpdated = startByteUpdated + startByteUpdatedOffset;
            byte[] devPathBytes = new byte[retrievedDevicePathLength];
            System.arraycopy(dsedBytes, startByteUpdated, devPathBytes,
                    0, retrievedDevicePathLength);
            devicePath = new UefiDevicePath(devPathBytes);
        }

        // header total size
        dsedHeaderLength = startByteUpdated + retrievedDevicePathLength;
    }

    /**
     * Returns the device type via a lookup.
     * Lookup based upon section 10.2.7.2, Table 19, in the PFP 1.06 v52 spec.
     *
     * @param deviceTypeInt int to convert to string
     * @return name of the device type
     */
    public String deviceTypeToString(final int deviceTypeInt) {
        return switch (deviceTypeInt) {
            case DeviceSecurityEventDataDeviceContext.DEVICE_TYPE_NONE -> "No device type";
            case DeviceSecurityEventDataDeviceContext.DEVICE_TYPE_PCI -> "PCI";
            case DeviceSecurityEventDataDeviceContext.DEVICE_TYPE_USB -> "USB";
            default -> "Unknown or invalid Device Type";
        };
    }

    /**
     * Returns a human-readable description of the data common to header structures.
     *
     * @return a description of this structure.
     */
    public String toString() {
        String dsedHeaderCommonInfo = "";

        dsedHeaderCommonInfo += "   SPDM Device Type = " + deviceTypeToString(deviceType) + "\n";
        if (devicePath != null) {
            dsedHeaderCommonInfo += "   SPDM Device Path:\n";
            dsedHeaderCommonInfo += devicePath;
        } else {
            dsedHeaderCommonInfo += "   SPDM Device Path = Unknown or invalid\n";
        }

        return dsedHeaderCommonInfo;
    }
}
