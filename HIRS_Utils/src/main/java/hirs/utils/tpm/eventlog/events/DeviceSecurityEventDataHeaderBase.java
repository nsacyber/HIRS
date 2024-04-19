package hirs.utils.tpm.eventlog.events;

import hirs.utils.HexUtils;
import hirs.utils.tpm.eventlog.spdm.SpdmHa;
import hirs.utils.tpm.eventlog.spdm.SpdmMeasurementBlock;
import hirs.utils.tpm.eventlog.uefi.UefiConstants;
import lombok.Getter;

import java.nio.charset.StandardCharsets;

/**
 * Class to process the DEVICE_SECURITY_EVENT_DATA_HEADER or ..HEADER2 per PFP.
 * The first 16 bytes of the event data header MUST be a String based identifier (Signature),
 * NUL-terminated, per PFP. The only currently defined Signature is "SPDM Device Sec",
 * which implies the data is a DEVICE_SECURITY_EVENT_DATA or ..DATA2.
 * DEVICE_SECURITY_EVENT_DATA_HEADER contains the measurement(s) and hash algorithm identifier
 * returned by the SPDM "GET_MEASUREMENTS" function.
 *
 * HEADERS defined by PFP v1.06 Rev 52:
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
 *
 * SPDM_MEASUREMENT_BLOCK and contents defined by SPDM v1.03, Sect 10.11.1, Table 53 and 54:
 * <p>
 * Measurement block format {
 *      Index                           1 byte;
 *      MeasurementSpec                 1 byte;
 *      MeasurementSize                 2 bytes;
 *      Measurement                     <MeasurementSize> bytes;
 * }
 * <p>
 * DMTF measurement spec format {
 *      DMTFSpecMeasurementValueType    1 byte;
 *      DMTFSpecMeasurementValueSize    2 bytes;
 *      DMTFSpecMeasurementValue        <DMTFSpecMeasurementValueSize> bytes;
 * }
 * <p>
 * DMTFSpecMeasurementValueType[7]
 *      Indicates how bits [0:6] are represented
 *      Bit = 0: Digest
 *      Bit = 1: Raw bit stream
 * DMTFSpecMeasurementValueType[6:0]
 *      Immutable ROM                   0x0
 *      Mutable firmware                0x1
 *      Hardware configuration          0x2
 *      Firmware configuration          0x3
 *      etc.
 * <p>
 */
public abstract class DeviceSecurityEventDataHeaderBase {

//    /**
//     * Contains the human-readable info inside the Device Security Event.
//     */
//    @Getter
//    private String dSEDheaderInfo = "";

    /**
     * Contains the size (in bytes) of the Header.
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
    private int deviceTypeId = -1;
//    /**
//     * Device type.
//     */
//    @Getter
//    private String deviceType = "";
    /**
     * Device path length.
     */
    @Getter
    private String devicePathLength = "";
    /**
     * Device path.
     */
    @Getter
    private String devicePath = "";

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


    public DeviceSecurityEventDataHeaderBase() {

    }


    /**
     * DeviceSecurityEventDataHeader Constructor.
     *
     * @param dSEDbytes byte array holding the DeviceSecurityEventData.
     */
    public DeviceSecurityEventDataHeaderBase(final byte[] dSEDbytes) {

//        spdmMeasurementBlockList = new ArrayList<>();

        byte[] signatureBytes = new byte[UefiConstants.SIZE_16];
        System.arraycopy(dSEDbytes, 0, signatureBytes, 0, UefiConstants.SIZE_16);
        signature = new String(signatureBytes, StandardCharsets.UTF_8)
                .substring(0, UefiConstants.SIZE_15);

        byte[] versionBytes = new byte[UefiConstants.SIZE_2];
        System.arraycopy(dSEDbytes, UefiConstants.OFFSET_16, versionBytes, 0,
                UefiConstants.SIZE_2);
        version = HexUtils.byteArrayToHexString(versionBytes);

    }

    public void extractDeviceTypeId(final byte[] dSEDbytes, int startByte) {

        // get the device type ID
        byte[] deviceTypeBytes = new byte[UefiConstants.SIZE_4];
        System.arraycopy(dSEDbytes, startByte, deviceTypeBytes, 0,
                UefiConstants.SIZE_4);
        deviceTypeId = HexUtils.leReverseInt(deviceTypeBytes);
    }

    public void extractDevicePathString(final byte[] dSEDbytes, int startByte) {

        // get the device path length
        byte[] devicePathLengthBytes = new byte[UefiConstants.SIZE_8];
        System.arraycopy(dSEDbytes, startByte, devicePathLengthBytes, 0,
                UefiConstants.SIZE_8);
        int deviceTypeLength = HexUtils.leReverseInt(devicePathLengthBytes);

        // TODO: how to interpret this??  i'ts not ascii

        // get the device path
        startByte = startByte + UefiConstants.SIZE_8;
        byte[] devicePathBytes = new byte[UefiConstants.SIZE_16];
        System.arraycopy(dSEDbytes, startByte, devicePathBytes, 0,
                deviceTypeLength);

        // TODO: store device path length
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
     * Returns a human readable description of the data within this event.
     *
     * @return a description of this event..
     */
    public String toString() {
        String dsedHeaderInfo = "";

        dsedHeaderInfo += "\n   SPDM Device";
        dsedHeaderInfo += "\n      Device Type: " + deviceType;
        dsedHeaderInfo += "\n      Device Path: " + devicePath;

//        if (version.equals("0100")) {
//            dsedHeaderInfo += "\n   SPDM hash algorithm = " + h1SpdmHashAlgo;
//            dsedHeaderInfo += "\n   SPDM Device";
//            dsedHeaderInfo += "\n      Device Type: " + deviceType;
//            dsedHeaderInfo += "\n      Device Path: " + devicePath;
//            dsedHeaderInfo += "\n   SPDM Measurement Block " + h1SpdmMeasurementBlock.toString();
//        } else if(version.equals("0200")) {
//            dsedHeaderInfo = "tbd";
//        }
        return dsedHeaderInfo;
    }

}
