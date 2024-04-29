package hirs.utils.tpm.eventlog.events;

import hirs.utils.HexUtils;
import hirs.utils.tpm.eventlog.uefi.UefiConstants;
import hirs.utils.tpm.eventlog.uefi.UefiDevicePath;
import lombok.Getter;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

/**
 * Abstract class to process the DEVICE_SECURITY_EVENT_DATA_HEADER or ..HEADER2 per PFP.
 * The first 16 bytes of the event data header MUST be a String based identifier (Signature),
 * NUL-terminated, per PFP. The only currently defined Signature is "SPDM Device Sec",
 * which implies the data is a DEVICE_SECURITY_EVENT_DATA or ..DATA2.
 *
 * HEADERS defined by PFP v1.06 Rev 52.
 * Certain fields are common to both ..HEADER and ..HEADER2, and are noted below the structures.
 * <p>
 * typedef struct tdDEVICE_SECURITY_EVENT_DATA_HEADER {
 *      UINT8                           Signature[16];
 *      UINT16                          Version;
 *      UINT16                          Length;
 *      UINT32                          SpdmHashAlg;
 *      UINT32                          DeviceType;
 *      SPDM_MEASUREMENT_BLOCK          SpdmMeasurementBlock;
 *      UINT64                          DevicePathLength;
 *      UNIT8                           DevicePath[DevicePathLength]
 * } DEVICE_SECURITY_EVENT_DATA_HEADER;
 * <p>
 * typedef struct tdDEVICE_SECURITY_EVENT_DATA_HEADER2 {        - NOT IMPLEMENTED YET
 *      UINT8                           Signature[16];
 *      UINT16                          Version;
 *      UINT8                           AuthState;
 *      UINT8                           Reserved;
 *      UINT32                          Length;
 *      UINT32                          DeviceType;
 *      UINT32                          SubHeaderType;
 *      UINT32                          SubHeaderLength;
 *      UINT32                          SubHeaderUID;
 *      UINT64                          DevicePathLength;
 *      UNIT8                           DevicePath[DevicePathLength]
 * } DEVICE_SECURITY_EVENT_DATA_HEADER2;
 * <p>
 * Fields common to both ..HEADER and ..HEADER2:
 *    Signature
 *    Version
 *    DeviceType
 *    DevicePathLength
 *    DevicePath
 * <p>
 */
public abstract class DeviceSecurityEventHeader {

    /**
     * Contains the size (in bytes) of the header.
     */
    @Getter
    private Integer dSEDheaderByteSize = 0;
    /**
     * Signature (text) data.
     */
    @Getter
    private String signature = "";
    /**
     * Version determines data structure used (..DATA or ..DATA2),
     * which determines whether ..HEADER or ..HEADER2 is used
     */
    @Getter
    private String version = "";
    /**
     * Device type.
     */
    @Getter
    private int deviceType = -1;
    /**
     * UEFI Device Path Length.
     */
    @Getter
    private int devicePathLength = 0;
    /**
     * UEFI Device path.
     */
    @Getter
    private UefiDevicePath devicePath = null;
    /**
     * Is the Device Path Valid.
     */
    private boolean devicePathValid = false;

    /**
     * Device Security Event Data Device Type = no device type.
     */
    public static final int DEVICE_TYPE_NONE = 0;
    /**
     * Device Security Event Data Device Type = DEVICE_TYPE_PCI.
     */
    public static final int DEVICE_TYPE_PCI = 1;
    /**
     * Device Security Event Data Device Type = DEVICE_TYPE_USB.
     */
    public static final int DEVICE_TYPE_USB = 2;


    /**
     * DeviceSecurityEventDataHeaderBase Default Constructor.
     */
    public DeviceSecurityEventHeader() {

    }

    /**
     * DeviceSecurityEventDataHeaderBase Constructor.
     *
     * @param dSEDbytes byte array holding the DeviceSecurityEventData.
     */
    public DeviceSecurityEventHeader(final byte[] dSEDbytes) {

        byte[] signatureBytes = new byte[UefiConstants.SIZE_16];
        System.arraycopy(dSEDbytes, 0, signatureBytes, 0, UefiConstants.SIZE_16);
        signature = new String(signatureBytes, StandardCharsets.UTF_8)
                .substring(0, UefiConstants.SIZE_15);

        byte[] versionBytes = new byte[UefiConstants.SIZE_2];
        System.arraycopy(dSEDbytes, UefiConstants.OFFSET_16, versionBytes, 0,
                UefiConstants.SIZE_2);
        version = HexUtils.byteArrayToHexString(versionBytes);

    }

    /**
     * Parse the device type from the Device Security Event Data Header/Header2.
     *
     * @param dSEDbytes byte array holding the DeviceSecurityEventData/Data2.
     * @param startByte starting byte of device type (depends on header fields before it).
     */
    public void extractDeviceType(final byte[] dSEDbytes, int startByte) {

        // get the device type ID
        byte[] deviceTypeBytes = new byte[UefiConstants.SIZE_4];
        System.arraycopy(dSEDbytes, startByte, deviceTypeBytes, 0,
                UefiConstants.SIZE_4);
        deviceType = HexUtils.leReverseInt(deviceTypeBytes);
    }

    /**
     * Parse the device path from the Device Security Event Data Header/Header2.
     * Also, determine final length of header (will be used to extract the next data structure).
     *
     * @param dSEDbytes byte array holding the DeviceSecurityEventData/Data2.
     * @param startByte starting byte of device path (depends on header fields before it).
     */
    public void extractDevicePathAndFinalSize(final byte[] dSEDbytes, int startByte)
            throws UnsupportedEncodingException {

        // get the device path length
        byte[] devicePathLengthBytes = new byte[UefiConstants.SIZE_8];
        System.arraycopy(dSEDbytes, startByte, devicePathLengthBytes, 0,
                UefiConstants.SIZE_8);
        int devicePathLength = HexUtils.leReverseInt(devicePathLengthBytes);

        // get the device path
        if (devicePathLength != 0) {
            startByte = startByte + UefiConstants.SIZE_8;
            byte[] devPathBytes = new byte[devicePathLength];
            System.arraycopy(dSEDbytes, startByte, devPathBytes,
                    0, devicePathLength);
            devicePath = new UefiDevicePath(devPathBytes);
            devicePathValid = true;
        }

        // header total size
        dSEDheaderByteSize = startByte + devicePathLength;
    }

    /**
     * Returns the device type via a lookup.
     * Lookup based upon section 10.2.7.2, Table 19, in the PFP 1.06 v52 spec.
     *
     * @param deviceTypeInt int to convert to string
     * @return name of the device type
     */
    public String deviceTypeToString(final int deviceTypeInt) {
        String deviceTypeStr;
        switch (deviceTypeInt) {
            case DEVICE_TYPE_NONE:
                deviceTypeStr = "No device type";
                break;
            case DEVICE_TYPE_PCI:
                deviceTypeStr = "PCI";
                break;
            case DEVICE_TYPE_USB:
                deviceTypeStr = "USB";
                break;
            default:
                deviceTypeStr = "Unknown or invalid Device Type";
        }
        return deviceTypeStr;
    }

    /**
     * Returns a human readable description of the data common to header structures.
     *
     * @return a description of this structure.
     */
    public String headerCommonInfoToString() {
        String dsedHeaderCommonInfo = "";

        dsedHeaderCommonInfo += "\n   SPDM Device Type = " + deviceTypeToString(deviceType);
        if (devicePathValid) {
            dsedHeaderCommonInfo += "\n   SPDM Device Path =\n";
            dsedHeaderCommonInfo += devicePath;
        }
        else {
            dsedHeaderCommonInfo += "\n   SPDM Device Path = Unknown or invalid";
        }

        return dsedHeaderCommonInfo;
    }
}
