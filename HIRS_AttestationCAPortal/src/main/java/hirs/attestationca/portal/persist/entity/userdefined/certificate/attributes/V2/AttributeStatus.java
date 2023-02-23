package hirs.attestationca.portal.persist.entity.userdefined.certificate.attributes.V2;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

/**
 * A type to handle the security Level used in the FIPS Level.
 * Ordering of enum types is intentional and their ordinal values correspond to enum
 * values in the TCG spec.
 *
 * <pre>
 * AttributeStatus ::= ENUMERATED {
 *      added (0),
 *      modified (1),
 *      removed (2) }
 * </pre>
 */
@AllArgsConstructor
public enum AttributeStatus {
    /**
     * Attribute Status for ADDED.
     */
    ADDED("added"),
    /**
     * Attribute Status for MODIFIED.
     */
    MODIFIED("modified"),
    /**
     * Attribute Status for REMOVED.
     */
    REMOVED("removed"),
    /**
     * Attribute Status for EMPTY.
     */
    EMPTY_STATUS(StringUtils.EMPTY);

    @Getter
    private final String value;
}
