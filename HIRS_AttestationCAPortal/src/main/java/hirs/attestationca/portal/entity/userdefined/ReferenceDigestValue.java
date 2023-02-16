package hirs.attestationca.portal.entity.userdefined;

import hirs.attestationca.portal.entity.ArchivableEntity;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;

import java.util.UUID;

/**
 * This class represents that actual entry in the Support RIM.
 * Digest Value, Event Type, index, RIM Tagid
 */
@ToString @EqualsAndHashCode(callSuper = false)
@Setter @Getter
@Entity
@Table(name = "ReferenceDigestValue")
@Access(AccessType.FIELD)
public class ReferenceDigestValue  extends ArchivableEntity {

//    @Type(type = "uuid-char")
    @JdbcTypeCode(java.sql.Types.VARCHAR)
    @Column
    private UUID baseRimId;
//    @Type(type = "uuid-char")
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
}
