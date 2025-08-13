package hirs.utils.tpm.eventlog.events;

import hirs.utils.HexUtils;
import hirs.utils.tpm.eventlog.uefi.UefiConstants;
import lombok.Getter;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

/**
 * Class to process the EV_NO_ACTION event.
 * The first 16 bytes of the event data MUST be a String based identifier (Signature).
 * Currently defined Signatures are
 * .  "Spec ID Event03"
 * .    - implies the data is a TCG_EfiSpecIDEvent
 * .    - TCG_EfiSpecIDEvent is the first event in a TPM Event Log and is used to determine
 * .      if the format of the Log (SHA1 vs Crypto Agile).
 * .  "StartupLocality"
 * .    - implies the data represents locality info (use lookup to interpret)
 * .  "NvIndexInstance"
 * .    - implies the data is a NV_INDEX_INSTANCE_EVENT_LOG_DATA
 * .  "NvIndexDynamic"
 * .    - implies the data is a NV_INDEX_DYNAMIC_EVENT_LOG_DATA
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
    @Getter
    private boolean isSpecIdEvent = false;
    /**
     * True if the event is a StartupLocality event.
     */
    @Getter
    private boolean isStartupLocality = false;
    /**
     * StartupLocality as defined in the PC Client Platform Firmware Profile.
     */
    @Getter
    private int startupLocality = 0;
    /**
     * TCG Event Log spec version.
     */
    @Getter
    private String specVersion = "Unknown";
    /**
     * TCG Event Log errata version.
     */
    @Getter
    private String specErrataVersion = "Unknown";

    /**
     * Human-readable description of the data within this DEVICE_SECURITY_EVENT_DATA/..DATA2 event.
     */
    @Getter
    private String noActionInfo = "";

    /**
     * Track status of pci.ids
     * This is only used for events that access the pci.ids file.
     * Default is normal status (normal status is from-filesystem).
     * Status will only change IF this is an event that uses this file,
     * and if that event causes a different status.
     */
    @Getter
    private String pciidsFileStatus = UefiConstants.FILESTATUS_FROM_FILESYSTEM;

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
            EvEfiSpecIdEvent specIDEvent = new EvEfiSpecIdEvent(eventData);
            noActionInfo += specIDEventToString(specIDEvent);
            isSpecIdEvent = true;
            specVersion = String.format("%s.%s",
                    specIDEvent.getVersionMajor(),
                    specIDEvent.getVersionMinor());
            specErrataVersion = specIDEvent.getErrata();
        } else if (signature.contains("StartupLocality")) {
            noActionInfo += "   Signature = StartupLocality";
            noActionInfo += "\n   StartupLocality = " + getLocality(eventData);
            isStartupLocality = true;
            getLocality(eventData); // set startupLocality variable
        } else if (signature.contains("NvIndexInstance")) {
            NvIndexInstanceEventLogData nvIndexInstanceEvent = new NvIndexInstanceEventLogData(eventData);
            noActionInfo += nvIndexInstanceEvent.toString();
            pciidsFileStatus = nvIndexInstanceEvent.getPciidsFileStatus();
        } else if (signature.contains("NvIndexDynamic")) {
            NvIndexDynamicEventLogData nvIndexDynamicEvent = new NvIndexDynamicEventLogData(eventData);
            noActionInfo += nvIndexDynamicEvent.toString();
        } else {
            noActionInfo = "   EV_NO_ACTION event named \"" + signature
                    + "\" encountered but support for processing it has not been"
                    + " added to this application.\n";
        }
    }

    /**
     * Returns a human-readable description of a SpecId event.
     *
     * @param specIDEvent byte array holding the event.
     * @return a description of the event.
     */
    public String specIDEventToString(final EvEfiSpecIdEvent specIDEvent) {

//        String specIdInfo = "";
//        specIdInfo += "   Signature = Spec ID Event03 : ";
//        if (specIDEvent.isCryptoAgile()) {
//            specIdInfo += "Log format is Crypto Agile\n";
//        } else {
//            specIdInfo += "Log format is SHA 1 (NOT Crypto Agile)\n";
//        }
//        specIdInfo += "   Platform Profile Specification version = "
//                + specIDEvent.getVersionMajor() + "." + specIDEvent.getVersionMinor()
//                + " using errata version " + specIDEvent.getErrata();
//
//        return specIdInfo;
        return specIDEvent.toString();
    }

    /**
     * Returns a human-readable description of locality based on numeric representation lookup.
     *
     * @param eventData byte array holding the event from which to grab locality
     * @return a description of the locality.
     */
    public String getLocality(final byte[] eventData) {
        final int eventDataSrcIndex = 16;
        final int locality0 = 0;
        final int locality3 = 3;
        final int locality4 = 4;

        byte[] localityBytes = new byte[1];
        System.arraycopy(eventData, eventDataSrcIndex, localityBytes, 0, 1);
        startupLocality = HexUtils.leReverseInt(localityBytes);

        return switch (startupLocality) {
            case locality0 -> "Locality 0 without an H-CRTM sequence";
            case locality3 -> "Locality 3 without an H-CRTM sequence";
            case locality4 -> "Locality 4 with an H-CRTM sequence initialized";
            default -> "Unknown";
        };
    }

    /**
     * Returns a description of this event.
     *
     * @return Human-readable description of this event.
     */
    public String toString() {
        return noActionInfo;
    }
}
