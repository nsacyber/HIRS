package hirs.utils.tpm.eventlog.events;

import hirs.utils.tpm.eventlog.uefi.UefiConstants;
import lombok.Getter;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

/**
 * Class to process the EV_NO_ACTION event.
 * The first 16 bytes of the event data MUST be a String based identifier (Signature).
 * The only currently defined Signatures are
 * 1) "Spec ID Event03"
 *      - implies the data is a TCG_EfiSpecIDEvent
 *      - TCG_EfiSpecIDEvent is the first event in a TPM Event Log and is used to determine
 *        if the format of the Log (SHA1 vs Crypto Agile).
 * 2) "NvIndexInstance"
 *      - implies the data is a NV_INDEX_INSTANCE_EVENT_LOG_DATA
 * <p>
 * Notes:
 * 1. First 16 bytes of the structure is an ASCII with a fixed Length of 16
 * 2. Add processing of other NoEvent types when new ones get defined
 */
public class EvNoAction {

    /**
     * Signature (text) data.
     */
    private String signature = "";
    /**
     * True of the event is a SpecIDEvent.
     */
    private boolean bSpecIDEvent = false;
    /**
     * True of the event is a NvIndexInstance.
     */
    private boolean bNvIndexInstance = false;
    /**
     * EvEfiSpecIdEvent Object.
     */
    @Getter
    private EvEfiSpecIdEvent specIDEvent = null;
    /**
     * NvIndexInstanceEvent Object.
     */
    @Getter
    private NvIndexInstanceEventLogData nvIndexInstanceEvent = null;

    /**
     * EvNoAction constructor.
     *
     * @param eventData byte array holding the event to process.
     * @throws java.io.UnsupportedEncodingException if input fails to parse.
     */
    public EvNoAction(final byte[] eventData) throws UnsupportedEncodingException {
        byte[] signatureBytes = new byte[UefiConstants.SIZE_15];
        System.arraycopy(eventData, 0, signatureBytes, 0, UefiConstants.SIZE_15);
        signature = new String(signatureBytes, StandardCharsets.UTF_8);
        signature = signature.replaceAll("[^\\P{C}\t\r\n]", ""); // remove null characters
        if (signature.contains("Spec ID Event03")) {      // implies CryptAgileFormat
            specIDEvent = new EvEfiSpecIdEvent(eventData);
            bSpecIDEvent = true;
        } else if (signature.contains("NvIndexInstance")) {
            nvIndexInstanceEvent = new NvIndexInstanceEventLogData(eventData);
            bNvIndexInstance = true;
        }
    }

    /**
     * Determines if this event is a SpecIDEvent.
     *
     * @return true of the event is a SpecIDEvent.
     */
    public boolean isSpecIDEvent() {
        return bSpecIDEvent;
    }

    /**
     * Returns a description of this event.
     *
     * @return Human readable description of this event.
     */
    public String toString() {
        String noActionInfo = "";
        if (bSpecIDEvent) {
            noActionInfo += "   Signature = Spec ID Event03 : ";
            if (specIDEvent.isCryptoAgile()) {
                noActionInfo += "Log format is Crypto Agile\n";
            } else {
                noActionInfo += "Log format is SHA 1 (NOT Crypto Agile)\n";
            }
            noActionInfo += "   Platform Profile Specification version = "
                    + specIDEvent.getVersionMajor() + "." + specIDEvent.getVersionMinor()
                    + " using errata version " + specIDEvent.getErrata();
        } else if (bNvIndexInstance) {
            noActionInfo = nvIndexInstanceEvent.toString();
        } else {
            noActionInfo = "EV_NO_ACTION event named " + signature
                    + " encountered but support for processing it has not been added to this application.\n";
        }
        return noActionInfo;
    }
}
