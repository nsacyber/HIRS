package hirs.data.persist;

import java.util.Arrays;
import java.util.Objects;

/**
 * This class represents that actual entry in the Support RIM.
 * Digest Value, Event Type, index, RIM Tagid
 */
public class ReferenceDigestValue {

    private int eventNumber;
    private String digestValue;
    private String eventType;
    private String tagId;
    private boolean matchFail;
    private byte[] chunk;

    /**
     * Default Constructor.
     */
    public ReferenceDigestValue() {

    }

    /**
     * Maybe add the match fail status to the device object: eventNumber, digest value
     */

    /**
     * Default Constructor with a parameter for the data.
     * @param data event data
     */
    public ReferenceDigestValue(final byte[] data) {
        this.chunk = data.clone();
        int i = 0;
        this.eventNumber = data[i];
        // look to using the Digest class
        this.digestValue = String.valueOf(data[++i]);
        this.eventType = String.valueOf(data[++i]);
        this.tagId = String.valueOf(data[++i]);
        this.matchFail = false;
    }

    /**
     * Default Constructor with parameters for all associated data.
     * @param eventNumber the event number
     * @param digestValue the key digest value
     * @param eventType the event type
     * @param tagId the tag id
     * @param matchFail the status of the baseline check
     */
    public ReferenceDigestValue(final int eventNumber, final String digestValue,
                          final String eventType, final String tagId, final boolean matchFail) {
        this.eventNumber = eventNumber;
        this.digestValue = digestValue;
        this.eventType = eventType;
        this.tagId = tagId;
        this.matchFail = matchFail;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ReferenceDigestValue that = (ReferenceDigestValue) obj;
        return eventNumber == that.eventNumber && matchFail == that.matchFail
                && Objects.equals(digestValue, that.digestValue)
                && Objects.equals(eventType, that.eventType)
                && Objects.equals(tagId, that.tagId) && Arrays.equals(chunk, that.chunk);
    }

    @Override
    @SuppressWarnings("MagicNumber")
    public int hashCode() {
        int result = Objects.hash(eventNumber, digestValue, eventType, tagId, matchFail);
        result = 31 * result + Arrays.hashCode(chunk);
        return result;
    }
}
