package hirs.tpm.eventlog;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;

import hirs.utils.HexUtils;

/**
 * Class to process a TCG_PCR_EVENT.
 * TCG_PCR_EVENT is used when the Event log uses the SHA1 Format as described in the
 * TCG Platform Firmware Profile (PFP) specification.
 * typedef struct {
 * TCG_PCRINDEX  PCRIndex;  //PCR Index value that either
 * //matches the PCRIndex of a
 * //previous extend operation or
 * //indicates that this Event Log
 * //entry is not associated with
 * //an extend operation
 * TCG_EVENTTYPE EventType; //See Log event types defined in toStrng()
 * TCG_DIGEST    digest;    //The hash of the event data
 * UINT32        EventSize; //Size of the event data
 * UINT8         Event[EventSize];  //The event data
 * } TCG_PCR_EVENT;
 */
public class TpmPcrEvent {
    /**
     * Type length = 4 bytes.
     */
    public static final int EV_TYPE_SIZE = 4;
    /**
     * Event Log spec version.
     */
    public static final int MIN_SIZE = 32;
    /**
     * Event Type (byte array).
     */
    public static final int INT_LENGTH = 4;
    /**
     * Event Type (byte array).
     */
    public static final int SHA1_LENGTH = 20;
    /**
     * Event Type (byte array).
     */
    public static final int SHA256_LENGTH = 32;
    /**
     * Each PCR bank holds 24 registers.
     */
    public static final int PCR_COUNT = 24;
    /**
     * PCR index.
     */
    private int pcrIndex = -1;
    /**
     * Event Type (long).
     */
    private long eventType = 0;
    /**
     * Event digest.
     */
    private byte[] digest = null;
    /**
     * Even data.
     */
    private byte[] eventContent;
    /**
     * TCG Event Log spec version.
     */
    private static String version = "Unknown";
    /**
     * TCG Event Log errata version.
     */
    private static String errata = "Unknown";
    /**
     * Length (in bytes) of a pcr.
     */
    private int digestLength = 0;

    /**
     * Constructor.
     *
     * @param is ByteArrayInputStream holding the event
     * @throws IOException when event can't be parsed
     */
    public TpmPcrEvent(final ByteArrayInputStream is) throws IOException {

    }

    /**
     * Sets the digest from a  TCG_PCR_EVENT digest field.
     * This can be SHA1 for older event structures or any algorithm for newer structure.
     *
     * @param digestData cryptographic hash
     */
    protected void setEventDigest(final byte[] digestData) {
        digest = new byte[digestLength];
        System.arraycopy(digestData, 0, digest, 0, this.digestLength);
    }

    /**
     * Retrieves the digest from a TCG Event.
     * This can be SHA1 for older event structures or any algorithm for newer structure.
     *
     * @return the digest data for the event
     */
    public byte[] getEventDigest() {
        byte[] digestCopy = new byte[digestLength];
        System.arraycopy(digest, 0, digestCopy, 0, this.digestLength);
        return digestCopy;
    }

    /**
     * Sets the event PCR index value from a TCG Event.
     *
     * @param eventIndex TCG Event PCR Index as defined in the PFP
     */
    protected void setPcrIndex(final byte[] eventIndex) {
        pcrIndex = HexUtils.leReverseInt(eventIndex);
    }

    /**
     * Gets the event index value from a TCG Event.
     *
     * @return eventIndex TCG Event Index as defined in the PFP
     */
    public int getPcrIndex() {
        return pcrIndex;
    }

    /**
     * Sets the EventType.
     *
     * @param type byte array holding the PFP defined log event type
     */
    protected void setEventType(final byte[] type) {
        byte[] evType = HexUtils.leReverseByte(type);
        eventType = new BigInteger(evType).longValue();
    }

    /**
     * Returns the EventType for the Event.
     *
     * @return event type
     */
    public long getEventType() {
        return eventType;
    }

    /**
     * Returns the version of the TCG Log Event specification pertaining to the log.
     * only updated if the event is a TCG_EfiSpecIdEvent.
     *
     * @return specification version
     */
    public String getSpecVersion() {
        return version;
    }

    /**
     * Returns the Errata version of the TCG Log Event specification pertaining to the log.
     * only updated if the event is a TCG_EfiSpecIdEvent).
     *
     * @return Errata version
     */
    public String getSpecErrataVersion() {
        return errata;
    }

    /**
     * Sets the event content after processing.
     *
     * @param eventData The PFP defined event content
     */
    protected void setEventContent(final byte[] eventData) {
        eventContent = new byte[eventData.length];
        System.arraycopy(eventContent, 0, eventData, 0, eventData.length);
    }

    /**
     * Gets the length of number of bytes in a PCR for the event.
     * event log format.
     *
     * @return byte array holding the events content field
     */
    protected byte[] getEventContent() {
        return eventContent;
    }

    /**
     * Sets the Digest Length.
     * Also the number of bytes expected within each PCR.
     *
     * @param length number of bytes in a PCR for the event.
     */
    public void setDigestLength(final int length) {
        digestLength = length;
    }

    /**
     * Gets the length of number of bytes in a PCR for the event.
     *
     * @return Byte Array containing the PFP defined event content
     */
    public int getDigestLength() {
        return digestLength;
    }
}
