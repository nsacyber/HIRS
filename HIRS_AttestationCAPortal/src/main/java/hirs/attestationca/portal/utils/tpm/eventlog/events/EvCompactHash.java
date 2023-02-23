package hirs.attestationca.portal.utils.tpm.eventlog.events;

import hirs.attestationca.utils.HexUtils;
import hirs.attestationca.utils.tpm.eventlog.uefi.UefiConstants;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

/**
 * Class to process the EV_COMPACT_HASH event.
 * The Old 2005 PFP description of EV_COMPACT_HASH which provides 4 byte ESI field (a pointer).
 * The 2019 PFP description allow the vendor to create event data that is "specified by the caller"
 * however the for PCR 6 there is a constraint that it contain
 * "The Event Data field SHALL be a unique string".
 */
public class EvCompactHash {

    /**
     * Holds the Compact Hash description.
     */
    private String eventInfo = "";

    /**
     * Constructor that takes in the event data (hex string) and passes to function below.
     *
     * @param event byte array of the Event Compact Hash.
     * @throws java.io.UnsupportedEncodingException if compact hash has non utf-8 characters.
     */
    public EvCompactHash(final byte[] event) throws UnsupportedEncodingException {
        hashEvent(event);
    }

    /**
     * Takes the event data (hex string) converts to readable output.
     * This may be somewhat limited due to the unpublished nature of vendor specific data.
     *
     * @param event data to process.
     * @return a human readable description.
     * @throws java.io.UnsupportedEncodingException if compact hash has non utf-8 characters.
     */
    public String hashEvent(final byte[] event) throws UnsupportedEncodingException {
        // determine if old format is used
        if (event.length == UefiConstants.SIZE_4) {   // older PFP defines as 4 byte ESI pointer.
            eventInfo = "   ESI = " + HexUtils.byteArrayToHexString(event);
        } else {  // otherwise assume the event content is a string
            eventInfo = "   " + new String(event, StandardCharsets.UTF_8);
        }
        return eventInfo;
    }

    /**
     * Readable description of the Event Content, however limiting that may be.
     *
     * @return Event description.
     */
    public String toString() {
        return eventInfo;
    }
}
