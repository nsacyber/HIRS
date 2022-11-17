package hirs.attestationca.entity;

import hirs.data.persist.ArchivableEntity;
import org.bouncycastle.util.Arrays;
import org.hibernate.annotations.Type;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Objects;
import java.util.UUID;

/**
 * This class represents that actual entry in the Support RIM.
 * Digest Value, Event Type, index, RIM Tagid
 */
@Entity
@Table(name = "ReferenceDigestValue")
@XmlRootElement(name = "ReferenceDigestValue")
@XmlAccessorType(XmlAccessType.FIELD)
@Access(AccessType.FIELD)
public class ReferenceDigestValue extends ArchivableEntity {

    @Type(type = "uuid-char")
    @Column
    private UUID baseRimId;
    @Type(type = "uuid-char")
    @Column
    private UUID supportRimId;
    @Column(nullable = false)
    private String manufacturer;
    @Column(nullable = false)
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
    @Column(nullable = false)
    private boolean updated = false;

    /**
     * Default constructor necessary for Hibernate.
     */
    protected ReferenceDigestValue() {
        super();
        this.baseRimId = null;
        this.supportRimId = null;
        this.manufacturer = "";
        this.model = "";
        this.pcrIndex = -1;
        this.digestValue = "";
        this.eventType = "";
        this.matchFail = false;
        this.patched = false;
        this.updated = false;
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
     * @param patched the status of the value being updated to patch
     * @param updated the status of the value being updated with info
     * @param contentBlob the data value of the content
     */
    public ReferenceDigestValue(final UUID baseRimId, final UUID supportRimId,
                                final String manufacturer, final String model,
                                final int pcrIndex, final String digestValue,
                                final String eventType, final boolean matchFail,
                                final boolean patched, final boolean updated,
                                final byte[] contentBlob) {
        this.baseRimId = baseRimId;
        this.supportRimId = supportRimId;
        this.manufacturer = manufacturer;
        this.model = model;
        this.pcrIndex = pcrIndex;
        this.digestValue = digestValue;
        this.eventType = eventType;
        this.matchFail = matchFail;
        this.patched = patched;
        this.updated = updated;
        this.contentBlob = Arrays.clone(contentBlob);
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
     * Getter for the manufacturer value.
     * @return the stored value
     */
    public String getManufacturer() {
        return manufacturer;
    }

    /**
     * Setter for the manufacturer value.
     * @param manufacturer the value to store
     */
    public void setManufacturer(final String manufacturer) {
        this.manufacturer = manufacturer;
    }

    /**
     * Getter for the model value.
     * @return the stored value
     */
    public String getModel() {
        return model;
    }

    /**
     * Setter for the model value.
     * @param model the value to store
     */
    public void setModel(final String model) {
        this.model = model;
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

    /**
     * Getter for the status of the updated state.
     * @return updated flag
     */
    public boolean isUpdated() {
        return updated;
    }

    /**
     * Setter for the status of the updated state.
     * @param updated the flag to set
     */
    public void setUpdated(final boolean updated) {
        this.updated = updated;
    }

    /**
     * Getter for the byte array of event values.
     * @return a clone of the byte array
     */
    public byte[] getContentBlob() {
        return contentBlob.clone();
    }

    /**
     * Setter for the byte array of values.
     * @param contentBlob non-null array.
     */
    public void setContentBlob(final byte[] contentBlob) {
        if (contentBlob != null) {
            this.contentBlob = contentBlob.clone();
        }
    }

    /**
     * Helper method to update the attributes of this object.
     * @param support the associated RIM.
     * @param baseRimId the main id to update
     */
    public void updateInfo(final SupportReferenceManifest support, final UUID baseRimId) {
        if (support != null) {
            setBaseRimId(baseRimId);
            setManufacturer(support.getPlatformManufacturer());
            setModel(support.getPlatformModel());
            setUpdated(true);
            if (support.isSwidPatch()) {
                // come back to this later, how does this get
                // identified to be patched
                setPatched(true);
            }
        }
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
        int result = Objects.hash(pcrIndex, digestValue, manufacturer, model,
                eventType, matchFail, patched, updated);
        return result;
    }

    /**
     * Returns a string of the classes fields.
     * @return a string
     */
    public String toString() {
        return String.format("ReferenceDigestValue: {%s, %d, %s, %s, "
                        + "matchFail - %b, updated - %b, patched - %b}",
                model, pcrIndex, digestValue, eventType, matchFail, updated, patched);
    }
}
