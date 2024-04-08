package hirs.utils.tpm.eventlog.events;

import hirs.utils.HexUtils;
import hirs.utils.tpm.eventlog.TcgTpmtHa;
import hirs.utils.tpm.eventlog.uefi.UefiConstants;
import lombok.Getter;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Class to process the DeviceSecurityEventDataHeader.
 * The first 16 bytes of the event data header MUST be a String based identifier (Signature),
 * NUL-terminated. The only currently defined Signature is "SPDM Device Sec" which implies
 * the event data is a DEVICE_SECURITY_EVENT_DATA. DEVICE_SECURITY_EVENT_DATA_HEADER contains
 * the measurement(s) and hash algorithm (SpdmHashAlg) identifier returned by the SPDM
 * "GET_MEASUREMENTS" function.
 * <p>
 * typedef struct tdDEVICE_SECURITY_EVENT_DATA_HEADER {
 * UINT8                             Signature[16];
 * UINT16                            Version;
 * UINT16                           Length;
 * UINT32                           SpdmHashAlg;
 * UINT32                           DeviceType;
 * SPDM_MEASUREMENT_BLOCK           SpdmMeasurementBlock;
 * UINT64                           DevicePathLength;
 * UNIT8                            DevicePath[DevicePathLength]
 * } DEVICE_SECURITY_EVENT_DATA_HEADER;
 * <p>
 * typedef struct tdSPDM_MEASUREMENT_BLOCK {
 * tbd      tbdalgorithmId;
 * tbd      tbddigestSize;
 * } SPDM_MEASUREMENT_BLOCK;
 * <p>
 * typedef struct tdDEVICEPATHLENGTH {
 * tbd      tbdalgorithmId;
 * tbd      tbddigestSize;
 * } DEVICEPATHLENGTH;
 * <p>
 * define TPM_ALG_SHA1           (TPM_ALG_ID)(0x0004)
 * define TPM_ALG_SHA256         (TPM_ALG_ID)(0x000B)
 * define TPM_ALG_SHA384         (TPM_ALG_ID)(0x000C)
 * define TPM_ALG_SHA512         (TPM_ALG_ID)(0x000D)
 * <p>
// * Notes: Parses event data for an EfiSpecID per Table 5 TCG_EfiSpecIdEvent Example.
// * 1. Should be the first Structure in the log
// * 2. Has an EventType of EV_NO_ACTION (0x00000003)
// * 3. Digest of 20 bytes of all 0's
// * 4. Event content defined as TCG_EfiSpecIDEvent Struct.
// * 5. First 16 bytes of the structure is an ASCII "Spec ID Event03"
// * 6. The version of the log is used to determine which format the Log
// * is to use (sha1 or Crypto Agile)
 */
public class DeviceSecurityEventDataHeader {
//    /**
//     * Minor Version.
//     */
//    @Getter
//    private String versionMinor = "";
//    /**
//     * Major Version.
//     */
//    @Getter
//    private String versionMajor = "";
//    /**
//     * Specification errata version.
//     */
//    @Getter
//    private String errata = "";
//    /**
//     * Signature (text) data.
//     */
//    @Getter
//    private String signature = "";
//    /**
//     * Platform class.
//     */
//    @Getter
//    private String platformClass = "";
//    /**
//     * Algorithm count.
//     */
//    @Getter
//    private int numberOfAlg = 0;
//    /**
//     * True if event log uses Crypto Agile format.
//     */
//    @Getter
//    private boolean cryptoAgile = false;
//    /**
//     * Algorithm list.
//     */
//    private List<String> algList;
//
//    /**
//     * EvEfiSpecIdEvent Constructor.
//     *
//     * @param efiSpecId byte array holding the spec ID Event.
//     */
//    public EvEfiSpecIdEvent(final byte[] efiSpecId) {
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
//    }
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
