package hirs.attestationca.persist.entity.userdefined;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Preconditions;
import hirs.attestationca.persist.entity.ArchivableEntity;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.codec.binary.Hex;
import org.hibernate.annotations.JdbcTypeCode;

import javax.xml.XMLConstants;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.UUID;

/**
 * This class represents the Reference Integrity Manifest object that will be
 * loaded into the DB and displayed in the ACA.
 */
@Getter @Setter @ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Log4j2
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@Table(name = "ReferenceManifest")
@Access(AccessType.FIELD)
public class ReferenceManifest  extends ArchivableEntity {

    /**
     * Holds the name of the 'hexDecHash' field.
     */
    public static final String HEX_DEC_HASH_FIELD = "hexDecHash";
    /**
     * String for display of a Base RIM.
     */
    public static final String BASE_RIM = "Base";
    /**
     * String for display of a Support RIM.
     */
    public static final String SUPPORT_RIM = "Support";
    /**
     * String for display of a Support RIM.
     */
    public static final String MEASUREMENT_RIM = "Measurement";

    /**
     * String for the xml schema ios standard.
     */
    public static final String SCHEMA_STATEMENT = "ISO/IEC 19770-2:2015 Schema (XSD 1.0) "
            + "- September 2015, see http://standards.iso.org/iso/19770/-2/2015/schema.xsd";
    /**
     * String for the xml schema URL file name.
     */
    public static final String SCHEMA_URL = "swid_schema.xsd";
    /**
     * String for the language type for the xml schema.
     */
    public static final String SCHEMA_LANGUAGE = XMLConstants.W3C_XML_SCHEMA_NS_URI;
    /**
     * String for the package location of the xml generated java files.
     */
    public static final String SCHEMA_PACKAGE = "hirs.utils.xjc";

    @EqualsAndHashCode.Include
    @Column(columnDefinition = "mediumblob", nullable = false)
    private byte[] rimBytes;
    @EqualsAndHashCode.Include
    @Column(nullable = false)
    private String rimType = "Base";
    @Column
    private String tagId = null;
    @Column
    private boolean swidPatch = false;
    @Column
    private boolean swidSupplemental = false;
    @Column
    private String platformManufacturer = null;
    @Column
    private String platformManufacturerId = null;
    @Column
    private String swidTagVersion = null;
    @Column
    private String swidVersion = null;
    @Column
    private String platformModel = null;
    @Column(nullable = false)
    private String fileName = null;
    @JdbcTypeCode(java.sql.Types.VARCHAR)
    @Column
    private UUID associatedRim;
    @Column
    private String deviceName;
    @Column
    private String hexDecHash = "";
    @Column
    private String eventLogHash = "";
    @Column
    @JsonIgnore
    private String base64Hash = "";

    /**
     * Default constructor necessary for Hibernate.
     */
    protected ReferenceManifest() {
        super();
        this.rimBytes = null;
        this.rimType = null;
        this.platformManufacturer = null;
        this.platformManufacturerId = null;
        this.platformModel = null;
        this.fileName = BASE_RIM;
        this.tagId = null;
        this.associatedRim = null;
    }

    /**
     * Default constructor for ingesting the bytes of the file content.
     * @param rimBytes - file contents.
     */
    public ReferenceManifest(final byte[] rimBytes) {
        Preconditions.checkArgument(rimBytes != null,
                "Cannot construct a RIM from a null byte array");

        Preconditions.checkArgument(rimBytes.length > 0,
                "Cannot construct a RIM from an empty byte array");

        this.rimBytes = rimBytes.clone();
        MessageDigest digest = null;
        this.hexDecHash = "";
        try {
            digest = MessageDigest.getInstance("SHA-256");
            this.hexDecHash = Hex.encodeHexString(
                    digest.digest(rimBytes));
        } catch (NoSuchAlgorithmException noSaEx) {
            log.error(noSaEx);
        }
        this.base64Hash = "";
        try {
            digest = MessageDigest.getInstance("SHA-256");
            this.base64Hash = Base64.getEncoder().encodeToString(
                    digest.digest(rimBytes));
        } catch (NoSuchAlgorithmException noSaEx) {
            log.error(noSaEx);
        }
    }

    /**
     * Getter for the Reference Integrity Manifest as a byte array.
     *
     * @return array of bytes
     */
    @JsonIgnore
    public byte[] getRimBytes() {
        if (this.rimBytes != null) {
            return this.rimBytes.clone();
        }
        return null;
    }

    public boolean isBase() {
        return rimType.equals(BASE_RIM);
    }

    public boolean isSupport() {
        return rimType.equals(SUPPORT_RIM);
    }
}
