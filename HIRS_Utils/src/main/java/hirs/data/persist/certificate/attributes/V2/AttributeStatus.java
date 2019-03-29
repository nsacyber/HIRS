package hirs.data.persist.certificate.attributes.V2;

import hirs.data.persist.certificate.attributes.ComponentIdentifier;

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
     * Attribute Status for NOT_SPECIFIED.
     */
    NOT_SPECIFIED(ComponentIdentifier.EMPTY_COMPONENT);

    private final String value;

    /**
     * Basic constructor.
     * @param value string containing the value.
     */
    AttributeStatus(final String value) {
        this.value = value;
    }

    /**
     * Getter for the string of attribute status value.
     * @return the string containing the value.
     */
    public String getValue() {
        return this.value;
    }
}
