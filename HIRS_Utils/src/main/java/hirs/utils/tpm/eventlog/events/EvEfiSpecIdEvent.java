package hirs.utils.tpm.eventlog.events;

import hirs.utils.HexUtils;
import hirs.utils.tpm.eventlog.TcgTpmtHa;
import hirs.utils.tpm.eventlog.uefi.UefiConstants;
import lombok.Getter;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Class to process the TCG_EfiSpecIDEvent.
 * The first 16 bytes of a Event Data MUST be String based identifier (Signature).
 * The only currently defined Signature is "Spec ID Event03"  which implies the data is
 * a TCG_EfiSpecIDEvent. TCG_EfiSpecIDEvent is the first event in a TPM Event Log
 * and is used to determine the format of the Log (SHA1 vs Crypt Agile).
 * <p>
 * typedef struct tdTCG_EfiSpecIdEvent {
 * BYTE                               Signature[16];
 * UINT32                             platformClass;
 * UINT8                              specVersionMinor;
 * UINT8                              specVersionMajor;
 * UINT8                              specErrata;
 * UINT8                              uintnSize;
 * UINT32                             numberOfAlgorithms;
 * TCG_EfiSpecIdEventAlgorithmSize    digestSizes[numberOfAlgorithms];
 * UINT8                              vendorInfoSize;
 * BYTE                               vendorInfo[VendorInfoSize];
 * } TCG_EfiSpecIDEvent;
 * <p>
 * typedef struct tdTCG_EfiSpecIdEventAlgorithmSize {
 * UINT16      algorithmId;
 * UINT16      digestSize;
 * } TCG_EfiSpecIdEventAlgorithmSize;
 * <p>
 * define TPM_ALG_SHA1           (TPM_ALG_ID)(0x0004)
 * define TPM_ALG_SHA256         (TPM_ALG_ID)(0x000B)
 * define TPM_ALG_SHA384         (TPM_ALG_ID)(0x000C)
 * define TPM_ALG_SHA512         (TPM_ALG_ID)(0x000D)
 * <p>
 * Notes: Parses event data for an EfiSpecID per Table 5 TCG_EfiSpecIdEvent Example.
 * 1. Should be the first Structure in the log
 * 2. Has an EventType of EV_NO_ACTION (0x00000003)
 * 3. Digest of 20 bytes of all 0's
 * 4. Event content defined as TCG_EfiSpecIDEvent Struct.
 * 5. First 16 bytes of the structure is an ASCII "Spec ID Event03"
 * 6. The version of the log is used to determine which format the Log
 * is to use (sha1 or Crypto Agile)
 */
public class EvEfiSpecIdEvent {
    /**
     * Minor Version.
     */
    @Getter
    private String versionMinor = "";
    /**
     * Major Version.
     */
    @Getter
    private String versionMajor = "";
    /**
     * Specification errata version.
     */
    @Getter
    private String errata = "";
    /**
     * Signature (text) data.
     */
    @Getter
    private String signature = "";
    /**
     * Platform class.
     */
    @Getter
    private String platformClass = "";
    /**
     * Algorithm count.
     */
    @Getter
    private int numberOfAlg = 0;
    /**
     * True if event log uses Crypto Agile format.
     */
    @Getter
    private boolean cryptoAgile = false;
    /**
     * Algorithm list.
     */
    private List<String> algList;
    public  List<String> getAlgList() {
        return new ArrayList<>(algList);
    }

    /**
     * EvEfiSpecIdEvent Constructor.
     *
     * @param efiSpecId byte array holding the spec ID Event.
     */
    public EvEfiSpecIdEvent(final byte[] efiSpecId) {
        algList = new ArrayList<>();
        byte[] signatureBytes = new byte[UefiConstants.SIZE_16];
        System.arraycopy(efiSpecId, 0, signatureBytes, 0, UefiConstants.SIZE_16);
        signature = new String(signatureBytes, StandardCharsets.UTF_8)
                .substring(0, UefiConstants.SIZE_15);

        byte[] platformClassBytes = new byte[UefiConstants.SIZE_4];
        System.arraycopy(efiSpecId, UefiConstants.OFFSET_16, platformClassBytes, 0,
                UefiConstants.SIZE_4);
        platformClass = HexUtils.byteArrayToHexString(platformClassBytes);

        byte[] specVersionMinorBytes = new byte[1];
        System.arraycopy(efiSpecId, UefiConstants.OFFSET_20, specVersionMinorBytes, 0, 1);
        versionMinor = HexUtils.byteArrayToHexString(specVersionMinorBytes);

        byte[] specVersionMajorBytes = new byte[1];
        System.arraycopy(efiSpecId, UefiConstants.OFFSET_21, specVersionMajorBytes, 0, 1);
        versionMajor = HexUtils.byteArrayToHexString(specVersionMajorBytes);

        byte[] specErrataBytes = new byte[1];
        System.arraycopy(efiSpecId, UefiConstants.OFFSET_22, specErrataBytes, 0, 1);
        errata = HexUtils.byteArrayToHexString(specErrataBytes);

        byte[] numberOfAlgBytes = new byte[UefiConstants.SIZE_4];
        System.arraycopy(efiSpecId, UefiConstants.OFFSET_24, numberOfAlgBytes, 0,
                UefiConstants.SIZE_4);
        numberOfAlg = HexUtils.leReverseInt(numberOfAlgBytes);

        byte[] algorithmIDBytes = new byte[UefiConstants.SIZE_2];
        int algLocation = UefiConstants.SIZE_28;
        for (int i = 0; i < numberOfAlg; i++) {
            System.arraycopy(efiSpecId, algLocation + UefiConstants.OFFSET_4 * i, algorithmIDBytes,
                    0, UefiConstants.SIZE_2);
            String alg = TcgTpmtHa.tcgAlgIdToString(HexUtils.leReverseInt(algorithmIDBytes));
            algList.add(alg);
        }
        if ((algList.size() == 1) && (algList.get(0).compareTo("SHA1") == 0)) {
            cryptoAgile = false;
        } else {
            cryptoAgile = true;
        }
    }

    /**
     * Returns a human-readable description of the data within this event.
     *
     * @return a description of this event.
     */
    public String toString() {
        String specInfo = "";

        specInfo += "   Signature = Spec ID Event03 : ";
        if (this.isCryptoAgile()) {
            specInfo += "Log format is Crypto Agile\n";
        } else {
            specInfo += "Log format is SHA 1 (NOT Crypto Agile)\n";
        }
        specInfo += "   Platform Profile Specification version = "
                + this.getVersionMajor() + "." + this.getVersionMinor()
                + " using errata version " + this.getErrata() + "\n";

//        if (signature.equals("Spec ID Event#")) {
//            specInfo += "Platform Profile Specification version = " + versionMajor + "." + versionMinor
//                    + " using errata version" + errata + "\n";
//        } else {
//            specInfo = "EV_NO_ACTION event named " + signature + " encountered but support for processing"
//                    + " it has not been added to this application" + "\n";
//        }
        specInfo += "   Algorithm list:";
        for (int i = 0; i < numberOfAlg; i++) {
            specInfo += "\n      " + i + ": " + algList.get(i);
        }
        return specInfo;
    }
}
