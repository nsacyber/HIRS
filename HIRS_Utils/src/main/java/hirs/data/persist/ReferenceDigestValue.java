package hirs.data.persist;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.annotations.Type;

import javax.persistence.Column;
import javax.persistence.Entity;
import java.util.Objects;
import java.util.UUID;

/**
 * This class represents that actual entry in the Support RIM.
 * Digest Value, Event Type, index, RIM Tagid
 */
@Entity
public class ReferenceDigestValue extends AbstractEntity {

    private static final Logger LOGGER = LogManager.getLogger(ReferenceDigestValue.class);
    @Type(type = "uuid-char")
    @Column
    private UUID digestRecordId;
    @Column(nullable = false)
    private int pcrIndex;
    @Column(nullable = false)
    private String digestValue;
    @Column(nullable = false)
    private String eventType;
    @Column(nullable = false)
    private boolean matchFail;
    @Column(nullable = false)
    private boolean patched = false;

    /**
     * Default Constructor.
     */
    public ReferenceDigestValue() {
        super();
        this.digestRecordId = UUID.randomUUID();
        this.pcrIndex = -1;
        this.digestValue = "";
        this.eventType = "";
        this.matchFail = false;
        this.patched = false;
    }

    /**
     * Default Constructor with parameters for all associated data.
     * @param digestRecordId the UUID of the associated record
     * @param pcrIndex the event number
     * @param digestValue the key digest value
     * @param eventType the event type to store
     * @param matchFail the status of the baseline check
     * @param patched the status of the value being updated to to patch
     */
    public ReferenceDigestValue(final UUID digestRecordId, final int pcrIndex,
                                final String digestValue, final String eventType,
                                final boolean matchFail, final boolean patched) {
        this.digestRecordId = digestRecordId;
        this.pcrIndex = pcrIndex;
        this.digestValue = digestValue;
        this.eventType = eventType;
        this.matchFail = matchFail;
        this.patched = patched;
    }

    /**
     * Getter for the digest record UUID.
     * @return the string of the UUID
     */
    public UUID getDigestRecordId() {
        return digestRecordId;
    }

    /**
     * Setter for the digest record UUID.
     * @param digestRecordId the value to store
     */
    public void setDigestRecordId(final UUID digestRecordId) {
        this.digestRecordId = digestRecordId;
    }

    /**
     * Getter for the event number.
     * @return the stored value
     */
    public int getPcrIndex() {
        return pcrIndex;
    }

    /**
     * Setter for the event number.
     * @param pcrIndex the value to store
     */
    public void setPcrIndex(final int pcrIndex) {
        this.pcrIndex = pcrIndex;
    }

    /**
     * Getter for the digest value.
     * @return the stored value
     */
    public String getDigestValue() {
        return digestValue;
    }

    /**
     * Setter for the digest value.
     * @param digestValue the value to store
     */
    public void setDigestValue(final String digestValue) {
        this.digestValue = digestValue;
    }

    /**
     * Getter for the event type value.
     * @return the stored value
     */
    public String getEventType() {
        return eventType;
    }

    /**
     * Setter for the event type.
     * @param eventType the value to store
     */
    public void setEventType(final String eventType) {
        this.eventType = eventType;
    }

    /**
     * Getter for the status of the match fail.
     * @return the value of the status
     */
    public boolean isMatchFail() {
        return matchFail;
    }

    /**
     * Setter for the status of a match fail.
     * @param matchFail the value to store
     */
    public void setMatchFail(final boolean matchFail) {
        this.matchFail = matchFail;
    }

    /**
     * Getter for the status of the patched state.
     * @return patched flag
     */
    public boolean isPatched() {
        return patched;
    }

    /**
     * Setter for the status of the patched state.
     * @param patched the flag to set
     */
    public void setPatched(final boolean patched) {
        this.patched = patched;
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
        return pcrIndex == that.pcrIndex && matchFail == that.matchFail
                && Objects.equals(digestValue, that.digestValue)
                && Objects.equals(digestRecordId, that.digestRecordId)
                && Objects.equals(eventType, that.eventType);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(pcrIndex, digestValue, digestRecordId,
                eventType, matchFail, patched);
        return result;
    }

    /**
     * Returns a string of the classes fields.
     * @return a string
     */
    public String toString() {
        return String.format("ReferenceDigestValue: {%d, %s, %s, %b}",
                pcrIndex, digestValue, eventType, matchFail);
    }
}
