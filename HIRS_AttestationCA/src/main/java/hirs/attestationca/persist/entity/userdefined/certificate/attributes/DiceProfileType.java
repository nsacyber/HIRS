package hirs.attestationca.persist.entity.userdefined.certificate.attributes;

/**
 * Enumeration of DICE certificate profiles as defined in the TCG specification "DICE Certificate Profiles".
 */
public enum DiceProfileType {
    /** IDevID (Initial Device Identifier) profile. */
    IDevID,
    /** LDevID (Locally Significant Device Identifier) profile. */
    LDevID,
    /** ECA (Embedded Certificate Authority) profile. */
    ECA,
    /** Attestation certificate profile. */
    ATTESTATION,
    /** Unknown or unclassified profile. */
    UNKNOWN
}
