package hirs.utils.tpm.eventlog.events;

import hirs.utils.HexUtils;
import hirs.utils.tpm.eventlog.TcgTpmtHa;
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
 * typedef struct tdDEVICE_SECURITY_EVENT_DATA_HEADER2 {
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

    /**
     * Contains the human-readable info inside the Device Security Event.
     */
    @Getter
    private String dSEDheaderInfo = "";
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
     * Event Data Length.
     */
    @Getter
    private String length = "";
    /**
     * Signature (text) data.
     */
    @Getter
    private String spdmHashAlgo = "";

    /**
     * DeviceSecurityEventDataHeader Constructor.
     *
     * @param dSEDbytes byte array holding the DeviceSecurityEventData.
     */
    public DeviceSecurityEventDataHeader(final byte[] dSEDbytes) {
//        algList = new ArrayList<>();
//        byte[] signatureBytes = new byte[UefiConstants.SIZE_16];
//        System.arraycopy(efiSpecId, 0, signatureBytes, 0, UefiConstants.SIZE_16);
//        signature = HexUtils.byteArrayToHexString(signatureBytes);
//        signature = new String(signatureBytes, StandardCharsets.UTF_8)
//                .substring(0, UefiConstants.SIZE_15);
//
//        byte[] platformClassBytes = new byte[UefiConstants.SIZE_4];
//        System.arraycopy(efiSpecId, UefiConstants.OFFSET_16, platformClassBytes, 0,
//                UefiConstants.SIZE_4);
//        platformClass = HexUtils.byteArrayToHexString(platformClassBytes);
//
//        byte[] specVersionMinorBytes = new byte[1];
//        System.arraycopy(efiSpecId, UefiConstants.OFFSET_20, specVersionMinorBytes, 0, 1);
//        versionMinor = HexUtils.byteArrayToHexString(specVersionMinorBytes);
//
//        byte[] specVersionMajorBytes = new byte[1];
//        System.arraycopy(efiSpecId, UefiConstants.OFFSET_21, specVersionMajorBytes, 0, 1);
//        versionMajor = HexUtils.byteArrayToHexString(specVersionMajorBytes);
//
//        byte[] specErrataBytes = new byte[1];
//        System.arraycopy(efiSpecId, UefiConstants.OFFSET_22, specErrataBytes, 0, 1);
//        errata = HexUtils.byteArrayToHexString(specErrataBytes);
//
//        byte[] numberOfAlgBytes = new byte[UefiConstants.SIZE_4];
//        System.arraycopy(efiSpecId, UefiConstants.OFFSET_24, numberOfAlgBytes, 0,
//                UefiConstants.SIZE_4);
//        numberOfAlg = HexUtils.leReverseInt(numberOfAlgBytes);
//
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
//
//    /**
//     * Returns a human readable description of the data within this event.
//     *
//     * @return a description of this event..
//     */
//    public String toString() {
//        String specInfo = "";
//        if (signature.equals("Spec ID Event#")) {
//            specInfo += "Platform Profile Specification version = " + versionMajor + "." + versionMinor
//                    + " using errata version" + errata;
//        } else {
//            specInfo = "EV_NO_ACTION event named " + signature
//                    + " encountered but support for processing it has not been added to this application";
//        }
//        return specInfo;
//    }


}
