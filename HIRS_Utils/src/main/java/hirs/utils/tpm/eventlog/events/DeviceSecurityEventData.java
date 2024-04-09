package hirs.utils.tpm.eventlog.events;

import hirs.utils.HexUtils;
import hirs.utils.tpm.eventlog.TcgTpmtHa;
import hirs.utils.tpm.eventlog.uefi.UefiConstants;
import lombok.Getter;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;


/**
 * Class to process the DEVICE_SECURITY_EVENT_DATA event per PFP.
 * DEVICE_SECURITY_EVENT_DATA has 2 structures:
 *    1) DEVICE_SECURITY_EVENT_DATA_HEADER
 *    2) DEVICE_SECURITY_EVENT_DATA_DEVICE_CONTEXT
 * DEVICE_SECURITY_EVENT_DATA_HEADER
 *    The first 16 bytes of the event data header MUST be a String based identifier (Signature),
 *    NUL-terminated. The only currently defined Signature is "SPDM Device Sec" which implies
 *    the event data is a DEVICE_SECURITY_EVENT_DATA. DEVICE_SECURITY_EVENT_DATA_HEADER contains
 *    the measurement(s) and hash algorithm (SpdmHashAlg) identifier returned by the SPDM
 *    "GET_MEASUREMENTS" function.
 * DEVICE_SECURITY_EVENT_DATA_DEVICE_CONTEXT
 *    DEVICE_SECURITY_EVENT_DATA_DEVICE_CONTEXT is a common SPDM structure which includes the
 *       identification of the device, device vendor, subsystem, etc.
 * <p>
 * typedef struct tdDEVICE_SECURITY_EVENT_DATA {
 * DEVICE_SECURITY_EVENT_DATA_HEADER            EventDataHeader;
 * DEVICE_SECURITY_EVENT_DATA_DEVICE_CONTEXT    DeviceContext;
 * } DEVICE_SECURITY_EVENT_DATA;
 * <p>
 * Notes: Parses event data for an DEVICE_SECURITY_EVENT_DATA per PFP v1.06 Rev52 Table 20.
 * 1. Has an EventType of EV_EFI_SPDM_FIRMWARE_BLOB (0x800000E1)
 * 2. Digest of 48 bytes
 * 3. Event content defined as DEVICE_SECURITY_EVENT_DATA Struct.
 * 4. First 16 bytes of the structure header is an ASCII "SPDM Device Sec"
 */
public class DeviceSecurityEventData {
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
    /**
     * Signature (text) data.
     */
    @Getter
    private String signature = "";
    /**
     * Platform class.
     */
    @Getter
    private String version = "";
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

    /**
     * DeviceSecurityEventData Constructor.
     *
     * @param deviceSecurityEventDataBytes byte array holding the spec ID Event.
     */
    public DeviceSecurityEventData(final byte[] deviceSecurityEventDataBytes) {
//        algList = new ArrayList<>();
        byte[] signatureBytes = new byte[UefiConstants.SIZE_16];
        System.arraycopy(deviceSecurityEventDataBytes, 0, signatureBytes, 0, UefiConstants.SIZE_16);
        //signature = HexUtils.byteArrayToHexString(signatureBytes);
        signature = new String(signatureBytes, StandardCharsets.UTF_8)
                .substring(0, UefiConstants.SIZE_15);

        byte[] versionBytes = new byte[UefiConstants.SIZE_4];
        System.arraycopy(deviceSecurityEventDataBytes, UefiConstants.OFFSET_16, versionBytes, 0,
                UefiConstants.SIZE_4);
        version = HexUtils.byteArrayToHexString(versionBytes);

        if (version == "1") {

        } else if (version == "2") {

        }

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
}
