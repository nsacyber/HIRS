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
    private int eventNumber;
    @Column(nullable = false)
    private String digestValue;
    @Column(nullable = false)
    private String eventType;
    @Column(nullable = false)
    private boolean matchFail;

    /**
     * Default Constructor.
     */
    public ReferenceDigestValue() {
        super();
        this.digestRecordId = UUID.randomUUID();
        this.eventNumber = -1;
        this.digestValue = "";
        this.eventType = "";
        this.matchFail = false;
    }

    /**
     * Default Constructor with parameters for all associated data.
     * @param digestRecordId the UUID of the associated record
     * @param eventNumber the event number
     * @param digestValue the key digest value
     * @param eventType the event type to store
     * @param matchFail the status of the baseline check
     */
    public ReferenceDigestValue(final UUID digestRecordId, final int eventNumber,
                                final String digestValue, final String eventType,
                                final boolean matchFail) {
        this.digestRecordId = digestRecordId;
        this.eventNumber = eventNumber;
        this.digestValue = digestValue;
        this.eventType = eventType;
        this.matchFail = matchFail;
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
    public int getEventNumber() {
        return eventNumber;
    }

    /**
     * Setter for the event number.
     * @param eventNumber the value to store
     */
    public void setEventNumber(final int eventNumber) {
        this.eventNumber = eventNumber;
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
                && Objects.equals(digestRecordId, that.digestRecordId)
                && Objects.equals(eventType, that.eventType);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(eventNumber, digestValue, digestRecordId, eventType, matchFail);
        return result;
    }

    /**
     * Returns a string of the classes fields.
     * @return a string
     */
    public String toString() {
        return String.format("ReferenceDigestValue: {%d, %b}", eventNumber, matchFail);
    }
}
