package hirs.attestationca.persist.provision.helper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Checks the TPMA_OBJECT attributes for TPM Keys.
 */
public final class TpmObjectAttributes {
    public static final int FIXED_TPM = 0x00000002;
    public static final int FIXED_PARENT = 0x00000010;
    public static final int SENSITIVE_DATA_ORIGIN = 0x00000020;
    public static final int USER_WITH_AUTH = 0x00000040;
    public static final int ADMIM_WITH_POLICY = 0x00000080;
    public static final int FIRMWARE_LIMITED = 0x00000100;
    public static final int RESTRICTED = 0x00010000;
    public static final int DECRYPT = 0x00020000;
    public static final int SIGN = 0x00040000;

    private static final int FIXED_KEY = FIXED_TPM | FIXED_PARENT | SENSITIVE_DATA_ORIGIN;

    private TpmObjectAttributes() { }

    /**
     * An attestation key must be a restricted signing key fixed to the TPM.
     * It will be refused if it's a decrypt key.
     * @param pub the AK public area
     * @return why the key is refused, or empty if it is acceptable
     */
    public static Optional<String> checkAttestationKey(final ParsedTpmPublic pub) {
        return check("AK", pub.objectAttributes(), FIXED_KEY | RESTRICTED | SIGN, DECRYPT);
    }

    /**
     * LDevID must be an unrestricted signing key fixed to the TPM.
     * It will be refused if it's a decrypt key.
     * @param pub the LDevID public area
     * @return why the key is refused, or empty if it is acceptable
     */
    public static Optional<String> checkDeviceIdentityKey(final ParsedTpmPublic pub) {
        return check("LDevID", pub.objectAttributes(), FIXED_KEY | SIGN, RESTRICTED | DECRYPT);
    }

    private static Optional<String> check(final String key, final int attributes, final int required, final int forbidden) {
        List<String> missing = names(required & ~attributes);
        List<String> unexpected = names(attributes & forbidden);
        if (missing.isEmpty() && unexpected.isEmpty()) {
            return Optional.empty();
        }
        StringBuilder reason = new StringBuilder("The ").append(key).append(" is not a key fixed to this TPM:");
        if (!missing.isEmpty()) {
            reason.append(" missing ").append(String.join(", ", missing)).append('.');
        }
        if (!unexpected.isEmpty()) {
            reason.append(" must not have ").append(String.join(", ", unexpected)).append('.');
        }
        return Optional.of(reason.toString());
    }

    private static List<String> names(final int attributes) {
        List<String> names = new ArrayList<>();
        addIfSet(names, attributes, FIXED_TPM, "fixedTPM");
        addIfSet(names, attributes, FIXED_PARENT, "fixedParent");
        addIfSet(names, attributes, SENSITIVE_DATA_ORIGIN, "sensitiveDataOrigin");
        addIfSet(names, attributes, RESTRICTED, "restricted");
        addIfSet(names, attributes, DECRYPT, "decrypt");
        addIfSet(names, attributes, SIGN, "sign");
        return names;
    }

    private static void addIfSet(final List<String> names, final int attributes, final int bit, final String name) {
        if ((attributes & bit) != 0) {
            names.add(name);
        }
    }
}
