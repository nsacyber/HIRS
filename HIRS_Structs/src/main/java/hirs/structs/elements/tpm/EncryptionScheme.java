package hirs.structs.elements.tpm;

/**
 * Maps embedded TPM encryption schemes (integers) to constants that can be used for encryption
 * purposes.
 */
public enum EncryptionScheme {

    /**
     * An encryption scheme for a {@link javax.crypto.Cipher} that is RSA, using ECB OAEP with a
     * SHA1 hash and MGF1 padding.
     */
    OAEP("RSA/ECB/OAEPWithSha1AndMGF1Padding"),

    /**
     * An encryption scheme for a {@link javax.crypto.Cipher}, also the default, that is RSA using
     * ECB with PKCS1 padding.
     */
    PKCS1("RSA/ECB/PKCS1Padding");

    /**
     * OAEP ID.
     */
    public static final int OAEP_VALUE = 3;

    private String encryptionScheme;

    /**
     * Constructs the string cipher transformation embedded that is returned by {@link
     * this#toString()}.
     *
     * @param encryptionScheme cipher transformation
     */
    EncryptionScheme(final String encryptionScheme) {
        this.encryptionScheme = encryptionScheme;
    }

    @Override
    public String toString() {
        return this.encryptionScheme;
    }

    /**
     * Maps an {@link EncryptionScheme} based upon an integer. If the scheme is unmapped, the
     * default, {@link #PKCS1} is returned.
     *
     * @param scheme to map
     * @return the encryption scheme, or if unknown, the default.
     */
    public static EncryptionScheme fromInt(final int scheme) {
        switch (scheme) {
            case OAEP_VALUE:
                return OAEP;
            default:
                return PKCS1;
        }
    }
}
