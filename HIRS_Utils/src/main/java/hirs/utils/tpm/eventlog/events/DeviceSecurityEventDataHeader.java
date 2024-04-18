package hirs.utils.tpm.eventlog.events;

import hirs.utils.HexUtils;
import hirs.utils.tpm.eventlog.TcgTpmtHa;
import hirs.utils.tpm.eventlog.spdm.SpdmHa;
import hirs.utils.tpm.eventlog.spdm.SpdmMeasurementBlock;
import hirs.utils.tpm.eventlog.uefi.UefiConstants;
import lombok.Getter;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

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
public class DeviceSecurityEventDataHeader {

//    /**
//     * Contains the human-readable info inside the Device Security Event.
//     */
//    @Getter
//    private String dSEDheaderInfo = "";

    /** ----------- Variables common to all Header Types -----------
     */
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
    private String deviceType = "";
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

    /** ----------- Variables specific to Header Type 1 -----------
//    /**
//     * Type Header 1 event data length.
//     */
//    @Getter
//    private String h1Length = "";
    /**
     * Type Header 1 SPDM hash algorithm.
     */
    @Getter
    private String h1SpdmHashAlgo = "";
//    /**
//     * Type Header 1 SPDM Measurement Block list.
//     */
//    private List<SpdmMeasurementBlock> h1SpdmMeasurementBlockList;
    /**
     * Type Header 1 SPDM Measurement Block.
     */
    private SpdmMeasurementBlock h1SpdmMeasurementBlock;

    /** ----------- Variables specific to Header Type 2 -----------
     */
    // TBD

    /**
     * DeviceSecurityEventDataHeader Constructor.
     *
     * @param dSEDbytes byte array holding the DeviceSecurityEventData.
     */
    public DeviceSecurityEventDataHeader(final byte[] dSEDbytes) {

//        spdmMeasurementBlockList = new ArrayList<>();

        byte[] signatureBytes = new byte[UefiConstants.SIZE_16];
        System.arraycopy(dSEDbytes, 0, signatureBytes, 0, UefiConstants.SIZE_16);
        signature = new String(signatureBytes, StandardCharsets.UTF_8)
                .substring(0, UefiConstants.SIZE_15);

        byte[] versionBytes = new byte[UefiConstants.SIZE_2];
        System.arraycopy(dSEDbytes, UefiConstants.OFFSET_16, versionBytes, 0,
                UefiConstants.SIZE_2);
        version = HexUtils.byteArrayToHexString(versionBytes);

//        if(version == "0100") {
        if (version.equals("0100")) {

            byte[] lengthBytes = new byte[UefiConstants.SIZE_2];
            System.arraycopy(dSEDbytes, 18, lengthBytes, 0,
                    UefiConstants.SIZE_2);
            int h1Length = HexUtils.leReverseInt(lengthBytes);

            byte[] spdmHashAlgoBytes = new byte[UefiConstants.SIZE_4];
            System.arraycopy(dSEDbytes, UefiConstants.OFFSET_20, spdmHashAlgoBytes, 0,
                    UefiConstants.SIZE_4);
            int h1SpdmHashAlgoInt = HexUtils.leReverseInt(spdmHashAlgoBytes);
            h1SpdmHashAlgo = SpdmHa.tcgAlgIdToString(h1SpdmHashAlgoInt);

            byte[] deviceTypeBytes = new byte[UefiConstants.SIZE_4];
            System.arraycopy(dSEDbytes, UefiConstants.OFFSET_24, deviceTypeBytes, 0,
                    UefiConstants.SIZE_4);
            int deviceTypeInt = HexUtils.leReverseInt(deviceTypeBytes);
            deviceType = deviceTypeToString(deviceTypeInt);

            // For each measurement block, create a SpdmMeasurementBlock object (can there be many blocks ?)

            // get the size of the SPDM Measurement Block
            byte[] sizeOfSpdmMeasBlockBytes = new byte[UefiConstants.SIZE_2];
            System.arraycopy(dSEDbytes, 30, sizeOfSpdmMeasBlockBytes, 0,
                    UefiConstants.SIZE_2);
            int sizeOfSpdmMeas = HexUtils.leReverseInt(sizeOfSpdmMeasBlockBytes);
            int sizeOfSpdmMeasBlock = sizeOfSpdmMeas + 4;

            // extract the bytes from the SPDM Measurement Block
            byte[] spdmMeasBlockBytes = new byte[sizeOfSpdmMeasBlock];
            System.arraycopy(dSEDbytes, 28, spdmMeasBlockBytes, 0,
                    sizeOfSpdmMeasBlock);
            h1SpdmMeasurementBlock = new SpdmMeasurementBlock(spdmMeasBlockBytes);

//        byte[] algorithmIDBytes = new byte[UefiConstants.SIZE_2];
//        int algLocation = UefiConstants.SIZE_28;
//        for (int i = 0; i < numberOfAlg; i++) {
//            System.arraycopy(efiSpecId, algLocation + UefiConstants.OFFSET_4 * i, algorithmIDBytes,
//                    0, UefiConstants.SIZE_2);
//            String alg = TcgTpmtHa.tcgAlgIdToString(HexUtils.leReverseInt(algorithmIDBytes));
//            algList.add(alg);
//        }
//        if ((algList.size() == 1) && (algList.get(0).compareTo("SHA1") == 0)) {
//            cryptoAgile = false;
//        } else {
//            cryptoAgile = true;
//        }

        }
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
        if (version.equals("0100")) {
            dsedHeaderInfo += "\n   SPDM hash algorithm = " + h1SpdmHashAlgo;
            dsedHeaderInfo += "\n   SPDM Device";
            dsedHeaderInfo += "\n      Device Type: " + deviceType;
            dsedHeaderInfo += "\n      Device Path: " + devicePath;
            dsedHeaderInfo += "\n   SPDM Measurement Block " + h1SpdmMeasurementBlock.toString();
        } else if(version.equals("0200")) {
            dsedHeaderInfo = "tbd";
        }
        return dsedHeaderInfo;
    }

}
