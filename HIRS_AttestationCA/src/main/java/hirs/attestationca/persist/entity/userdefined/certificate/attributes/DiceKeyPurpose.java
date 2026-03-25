package hirs.attestationca.persist.entity.userdefined.certificate.attributes;

import lombok.Getter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Enumeration of DICE key purposes as defined in the TCG specification "DICE Certificate Profiles".
 * Contains DICE EKU OID mappings for each purpose.
 */
public enum DiceKeyPurpose {
    /** Initial Identity key purpose. */
    IDENTITY_INIT("2.23.133.5.4.100.6", "DICE Initial Identity"),
    /** Local Identity key purpose. */
    IDENTITY_LOC("2.23.133.5.4.100.7", "DICE Local Identity"),
    /** Initial Attestation key purpose. */
    ATTEST_INIT("2.23.133.5.4.100.8", "DICE Initial Attestation"),
    /** Local Attestation key purpose. */
    ATTEST_LOC("2.23.133.5.4.100.9", "DICE Local Attestation"),
    /** Initial Assertion key purpose. */
    ASSERT_INIT("2.23.133.5.4.100.10", "DICE Initial Assertion"),
    /** Local Assertion key purpose. */
    ASSERT_LOC("2.23.133.5.4.100.11", "DICE Local Assertion"),
    /** ECA (Embedded Certificate Authority) key purpose. */
    ECA("2.23.133.5.4.100.12", "DICE Embedded Certificate Authority"),
    /** Other key purposes not specifically defined. */
    OTHER(null, "Other");

    private static final Map<String, DiceKeyPurpose> BY_OID;

    static {
        Map<String, DiceKeyPurpose> byOid = new HashMap<>();
        for (DiceKeyPurpose value : values()) {
            if (value.oid != null) {
                byOid.put(value.oid, value);
            }
        }
        BY_OID = Collections.unmodifiableMap(byOid);
    }

    /** Contains the TCG DICE OID for this key purpose. */
    @Getter
    private final String oid;
    /** Contains the display name for this key purpose. */
    @Getter
    private final String displayName;

    DiceKeyPurpose(final String oid, final String displayName) {
        this.oid = oid;
        this.displayName = displayName;
    }

    /**
     * Helper method to return a DICE key purpose from a given OID.
     * @param oid the input OID
     * @return An enum value corresponding to the key purpose.
     */
    public static DiceKeyPurpose fromOid(final String oid) {
        return BY_OID.getOrDefault(oid, OTHER);
    }

    /**
     * Create a mapping of DICE EKU OIDs to their corresponding key purposes.
     * @return An unmodifiable {@link Map} of DICE EKU OIDs to human-readable key purpose descriptions.
     */
    public static Map<String, String> getExtendedKeyUsageMap() {
        Map<String, String> ekuMap = new HashMap<>();
        for (DiceKeyPurpose value : values()) {
            ekuMap.put(value.oid, value.displayName);
        }
        return Collections.unmodifiableMap(ekuMap);
    }
}
