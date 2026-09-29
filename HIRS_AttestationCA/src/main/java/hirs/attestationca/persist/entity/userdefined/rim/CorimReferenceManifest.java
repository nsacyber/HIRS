package hirs.attestationca.persist.entity.userdefined.rim;

import hirs.attestationca.persist.entity.userdefined.ReferenceManifest;
import hirs.utils.rim.unsignedRim.cbor.ietfCorim.CoRim;
import hirs.utils.rim.unsignedRim.cbor.ietfCorim.CoRimParser;
import hirs.utils.signature.cose.Cbor.CborTagProcessor;
import hirs.utils.signature.cose.CoseParser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

/**
 * A Concise Reference Integrity Manifest (CoRIM) as defined by the IETF RATS working group
 * (draft-ietf-rats-corim). May be uploaded either as an unsigned {@code tagged-unsigned-corim-map}
 * (CBOR tag 501) or as a signed CoRIM: an unsigned CoRIM wrapped in a COSE_Sign1 envelope
 * (CBOR tag 18, RCF 9052) with a {@code corim-meta-map} in the protected header.
 *
 * <p>Follows the same minimal-persist pattern as {@link ComponentReferenceManifest}: only a
 * handful of summary columns are stored; the full structure (nested CoMID/CoSWID tags,
 * measurements, dependent RIMs) is a re-parsed lazily from {@code rimBytes} at display time via
 * {@link #parseCorim()}.
 */
@Log4j2
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Entity
public class CorimReferenceManifest extends ReferenceManifest {

    /** {@code corim-map / 0 (id)} - string or UUID identifying this CoRIM. */
    @Column
    private String corimId;

    /** {@code corim-map / 3 (profile)} - first profile OID/URI, stringified. */
    @Column
    private String profile;

    /** {@code corim-map / 4 (validity) / not-before}, formatted by {@link CoRimParser}. */
    @Column
    private String corimNotBefore;

    /** {@code corim-map / 4 (validity) / not-after}, formatted by {@link CoRimParser}. */
    @Column
    private String corimNotAfter;

    /** True if the uploaded bytes were a COSE_Sign1-wrapped (signed) CoRIM. */
    @Column
    private boolean corimSigned;

    /**
     * Default constructor necessary for Hibernate.
     */
    protected CorimReferenceManifest() {
        super();
    }

    /**
     * Main constructor for the CoRIM RIM object. Accepts either a signed CoRIM (COSE_Sign1, CBOR tag 18)
     * or an unsigned {@code tagged-unsigned-corim-map} (CBOR tag 501), parses the inner
     * {@code corim-map}, and populates the summary fields shown on the RIM list page. Signature
     * verification is intentionally not performed here; the details page performs it lazily.
     *
     * @param fileName string representation of the uploaded file
     * @param rimBytes raw uploaded bytes (COSE_Sign1-wrapped or bare tag-501 CoRIM)
     */
    public CorimReferenceManifest(final String fileName,
                                      final byte[] rimBytes) {
        super(rimBytes);
        this.setFileName(fileName);
        this.setRimType(CORIM_RIM);

        try {
            final CoRimParser corim = new CoRimParser(unwrapToCorimMap(rimBytes));

            // CoRIM-specific persisted columns
            this.corimId = nullIfBlank(corim.getId());
            this.profile = nullIfBlank(corim.getProfile());
            this.corimNotBefore = nullIfBlank(corim.getNotBeforeStr());
            this.corimNotAfter = nullIfBlank(corim.getNotAfterStr());

            // inherited ReferenceManifest fields (drive the list-page columns)
            this.setTagId(this.corimId);
            this.setPlatformManufacturer(nullIfBlank(corim.getEntityName()));
            this.setPlatformManufacturerId(nullIfBlank(corim.getEntityRegId()));
            this.setPlatformModel(firstNonBlank(this.profile, this.corimId));

        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to parse file as an IETF CoRIM (signed or unsigned)", e);
        }
    }

    /**
     * Peels outer wrappers off the raw uploaded bytes and returns the bare {@code corim-map}
     * body that {@link CoRimParser} expects. Also records {@link #corimSigned}.
     *
     * <p>Accepted outer shapes (per draft-ietf-rats-corim and observed vendor output):
     * <ul>
     *     <li>bare untagged {@code corim-map}</li>
     *     <li>{@code #6.501(corim-map)} - tagged-unsigned-corim-map</li>
     *     <li>{@code #6.18(COSE_Sign1)} - signed CoRIM; payload is the (possibly 501-tagged) map</li>
     *     <li>{@code #6.500(#6.502(#6.18(...)))} / {@code #6.500(#6.501(...))} -
     *         {@code tagged-corim / $concise-rim-type-choice} envelope used by some vendors</li>
     * </ul>
     *
     * @param raw the raw uploaded bytes
     * @return the untagged {@code corim-map} CBOR body
     */
    private byte[] unwrapToCorimMap(final byte[] raw) {
        this.corimSigned = false;
        byte[] cur = raw;
        // Peel at most a handful of outer CoRIM-family / COSE tags. Anything deeper is malformed,.
        for (int i = 0; i < 4; i++) {
            final CborTagProcessor ctp = new CborTagProcessor(cur);
            if (!ctp.isTagged()) {
                // Bare corim-map (or the map body left after stripping 501). CoRimParser take it as-is.
                return cur;
            }
            if (ctp.isCose()) {
                // COSE_Sign1 (tag 18). CoseParser.getPayload() strips the bstr wrapper and peels one
                // inner tag (501) if present, so what comes back is the bare corim-map.
                this.corimSigned = true;
                return new CoseParser(cur).getPayload();
            }
            if (CoRim.isCoRimTag(ctp.getTagId())) {
                // 500 (tagged-corim), 501 (unsigned-corim-map), 502 (signed-corim), ... - strip and
                // keep peeling; the next layer is either another tag, COSE, or the map itself.
                cur = ctp.getContent();
                continue;
            }
            throw new IllegalArgumentException(
                    "Not a CoRIM: unexpected outer CBOR tag " + ctp.getTagId());
        }
        throw new IllegalArgumentException(
                "Not a CoRIM: outer tag nesting too deep");
    }

    /**
     * Re-parses the stored bytes as a COSE_Sign1 object. Only meaningful when {@link #isCorimSigned()}
     * is true. Used by the details page to display COSE header information (algorithm, key id,
     * content type, corim-meta) and to verify the signature.
     *
     * @return a {@link CoseParser} over this RIM's raw bytes, or {@code null} if this CoRIM is unsigned
     */
    public CoseParser parseCose() {
        final byte[] cose = getCoseBytes();
        return cose == null ? null : new CoseParser(cose);
    }

    /**
     * Returns the raw COSE_Sign1 (CBOR tag 18) bytes for a signed CoRIM, with any outer
     * CoRIM-family wrapper tags (e.g. {@code #6.500(#6.502(...))}) peeled off. Used by the
     * details page to reconstruct the RFC 9052 {@code Sig_structure1} for signature verification.
     *
     * @return the tag-18 COSE_Sign1 bytes, or {@code null} if this CoRIM is unsigned
     */
    public byte[] getCoseBytes() {
        if (!corimSigned) {
            return null;
        }
        byte[] cur = getRimBytes();
        for (int i = 0; i < 4; i++) {
            final CborTagProcessor ctp = new CborTagProcessor(cur);
            if (ctp.isCose()) {
                return cur;
            }
            if (CoRim.isCoRimTag(ctp.getTagId()))
            {
                cur = ctp.getContent();
                continue;
            }
            break;
        }
        throw new IllegalStateException("corimSigned is set but no COSE_Sign1 envelope was found");
    }

    /**
     * Re-parses the stored bytes, unwraps any COSE envelope, and returns the decoded TCG
     * CoRIM. Used by the details page to display the full contents (identity, validity, entities,
     * dependent RIMs, nested CoMID/CoSWID tags, and the flattened measurement list).
     *
     * @return the parsed {@link CoRimParser} (which extends {@link CoRim})
     */
    public CoRimParser parseCorim() {
        try {
            return new CoRimParser(unwrapToCorimMap(getRimBytes()));
        } catch (Exception e) {
            log.error("Failed to re-parse CoRIM from {}", getFileName(), e);
            throw new IllegalStateException("Failed to re-parse CoRIM payload", e);
        }
    }

    private static String nullIfBlank(final String s) {
        return (s == null || s.isBlank()) ? null : s;
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
