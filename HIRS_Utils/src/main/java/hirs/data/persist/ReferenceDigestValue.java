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
    private UUID baseRimId;
    @Column
    private UUID supportRimId;
    @Column
    private String manufacturer;
    @Column
    private String model;
    @Column(nullable = false)
    private int pcrIndex;
    @Column(nullable = false)
    private String digestValue;
    @Column(nullable = false)
    private String eventType;
    @Column(columnDefinition = "blob", nullable = true)
    private byte[] contentBlob;
    @Column(nullable = false)
    private boolean matchFail;
    @Column(nullable = false)
    private boolean patched = false;

    /**
     * Default Constructor.
     */
    public ReferenceDigestValue() {
        super();
        this.baseRimId = UUID.randomUUID();
        this.supportRimId = UUID.randomUUID();
        this.manufacturer = "";
        this.model = "";
        this.pcrIndex = -1;
        this.digestValue = "";
        this.eventType = "";
        this.matchFail = false;
        this.patched = false;
        this.contentBlob = null;
    }

    /**
     * Default Constructor with parameters for all associated data.
     * @param baseRimId the UUID of the associated record
     * @param supportRimId the UUID of the associated record
     * @param manufacturer associated creator for this information
     * @param model the specific device type
     * @param pcrIndex the event number
     * @param digestValue the key digest value
     * @param eventType the event type to store
     * @param matchFail the status of the baseline check
     * @param patched the status of the value being updated to to patch
     * @param contentBlob the data value of the content
     */
    public ReferenceDigestValue(final UUID baseRimId, final UUID supportRimId,
                                final String manufacturer, final String model,
                                final int pcrIndex, final String digestValue,
                                final String eventType, final boolean matchFail,
                                final boolean patched, final byte[] contentBlob) {
        this.baseRimId = baseRimId;
        this.supportRimId = supportRimId;
        this.manufacturer = manufacturer;
        this.model = model;
        this.pcrIndex = pcrIndex;
        this.digestValue = digestValue;
        this.eventType = eventType;
        this.matchFail = matchFail;
        this.patched = patched;
        this.contentBlob = contentBlob;
    }

    /**
     * Getter for the digest record UUID.
     * @return the string of the UUID
     */
    public UUID getBaseRimId() {
        return baseRimId;
    }

    /**
     * Setter for the digest record UUID.
     * @param baseRimId the value to store
     */
    public void setBaseRimId(final UUID baseRimId) {
        this.baseRimId = baseRimId;
    }

    /**
     * Getter for the digest record UUID.
     * @return the string of the UUID
     */
    public UUID getSupportRimId() {
        return supportRimId;
    }

    /**
     * Setter for the digest record UUID.
     * @param supportRimId the value to store
     */
    public void setSupportRimId(final UUID supportRimId) {
        this.supportRimId = supportRimId;
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
                && Objects.equals(baseRimId, that.baseRimId)
                && Objects.equals(supportRimId, that.supportRimId)
                && Objects.equals(eventType, that.eventType);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(pcrIndex, digestValue, baseRimId, supportRimId,
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
