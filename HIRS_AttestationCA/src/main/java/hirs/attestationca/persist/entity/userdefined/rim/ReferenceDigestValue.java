package hirs.attestationca.persist.entity.userdefined.rim;

import hirs.attestationca.persist.entity.ArchivableEntity;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.bouncycastle.util.Arrays;
import org.hibernate.annotations.JdbcTypeCode;

import java.util.UUID;

/**
 * This class represents that actual entry in the Support RIM.
 * Digest Value, Event Type, index, RIM Tagid
 */
@Data
@Builder
@AllArgsConstructor
@Entity
@EqualsAndHashCode(callSuper=false)
@Table(name = "ReferenceDigestValue")
@Access(AccessType.FIELD)
public class ReferenceDigestValue extends ArchivableEntity {

    @JdbcTypeCode(java.sql.Types.VARCHAR)
    @Column
    private UUID baseRimId;
    @JdbcTypeCode(java.sql.Types.VARCHAR)
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
    private boolean patched;
    @Column(nullable = false)
    private boolean updated;

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