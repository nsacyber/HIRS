package hirs.attestationca.persist.entity.userdefined.rim;

import hirs.attestationca.persist.entity.userdefined.ReferenceManifest;
import hirs.utils.rim.unsignedRim.cbor.ietfCoswid.Coswid;
import hirs.utils.rim.unsignedRim.cbor.ietfCoswid.CoswidParser;
import hirs.utils.rim.unsignedRim.cbor.tcgCompRimCoswid.TcgCompRimCoswid;
import hirs.utils.rim.unsignedRim.cbor.tcgCompRimCoswid.TcgCompRimCoswidParser;
import hirs.utils.signature.cose.CoseParser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

/**
 * Subclass that will just focus on Component RIMs.
 * A Component Reference Integrity Manifest: a TCG Component RIM CoSWID (RFC 9393 CoSWID plus
 * TCG-defined extension keys for component manufacturer, binding spec, payload type, and SPDM
 * measurements) wrapped in a COSE_Sign1 envelope (RFC 9052)
 */
@Log4j2
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Entity
public class ComponentReferenceManifest extends ReferenceManifest {

    @Column
    private String bindingSpec;

    @Column
    private String bindingSpecVersion;

    @Column
    private String softwareName;

    @Column
    private String softwareVersion;

    /**
     * Default constructor necessary for Hibernate.
     */
    protected ComponentReferenceManifest() {
        super();
    }

    /**
     * Main constructor for the Component RIM object.
     * Takes the raw bytes of a COSE-signed TCG Component RIM CoSWID, unwraps
     * the COSE_Sign1 envelope, parses the inner CoSWID (including TCG extension keys), and
     * populates the summary fields shown on the RIM list page. Signature verification is
     * intentionally not performed here.
     *
     * @param fileName - string representation of the uploaded file
     * @param rimBytes byte array representation of the COSE_Sign1-wrapped TCG Component RIM CoSWID
     */
    public ComponentReferenceManifest(final String fileName,
                                      final byte[] rimBytes) {
        super(rimBytes);
        this.setFileName(fileName);
        this.setRimType(COMPONENT_RIM);

        try {
            final CoseParser cose = new CoseParser(rimBytes);
            final TcgCompRimCoswid crim = new TcgCompRimCoswidParser(cose.getPayload()).getTcRim();

            // inherited ReferenceManifest fields (drive the list-page columns)
            this.setTagId(crim.getTagId());
            this.setSwidTagVersion(crim.getTagVersion());
            this.setSwidVersion(crim.getSoftwareVersion());
            this.setSwidPatch(crim.isPatch());
            this.setSwidSupplemental(crim.isSupplemental());
            this.setPlatformManufacturer(crim.getCrimComponentManufacturer());
            this.setPlatformManufacturerId(crim.getCrimComponentManufacturerID());
            this.setPlatformModel(firstNonBlank(crim.getProduct(), crim.getSoftwareName()));
            this.setPayloadType(crim.getCrimPayloadType());

            // Component-specific persisted columns
            this.bindingSpec = crim.getCrimBindingSpec();
            this.bindingSpecVersion = crim.getCrimBindingSpecVersion();
            this.softwareName = crim.getSoftwareName();
            this.softwareVersion = crim.getSoftwareVersion();

//        } catch (Exception ie) {
//            throw new IllegalArgumentException("Not a component RIM.", ie);
//        }
//
//        // test cbor parser of file
//        try {
//            TCGEventLog ev = new TCGEventLog(rimBytes);
//            java.util.Objects.requireNonNull(ev);       // workaround for SpotBugs to see the variable as "used"

        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to parse file as a TCG component RIM CoSWID", e);
        }
    }

    /**
     * Re-parses the stored bytes as a COSE_Sign1 object. used by the details page to display
     * COSE header information (algorithm, key id, content type) and, in future, to verify the signature.
     *
     * @return a {@link CoseParser} over this RIM's raw bytes
     */
    public CoseParser parseCose() {
        return new CoseParser(getRimBytes());
    }

    /**
     * Re-parses the stored bytes, unwraps the COSE envelope, and returns the decoded TCG
     * Component RIM CoSWID. Used by the details page to display the full contents.
     *
     * @return the parsed {@link TcgCompRimCoswid}
     */
    public TcgCompRimCoswid parseComponentRim() {
        try {
            return new TcgCompRimCoswidParser(parseCose().getPayload()).getTcRim();
        } catch (Exception e) {
            log.error("Failed to re-parse TCG Component RIM CoSWID from {}", getFileName(), e);
            throw new IllegalStateException("Failed to re-parse TCG Component RIM CoSWID payload", e);
        }
    }

    /**
     *
     * @param preferred
     * @param fallback
     * @return
     */
    private static String firstNonBlank(final String preferred, final String fallback) {
        return (preferred != null && !preferred.isBlank()) ? preferred : fallback;
    }
}
