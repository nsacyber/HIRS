package hirs.attestationca.persist.entity.userdefined.certificate;

/**
 * User-defined variables used for the validation, processing, and management of certificates.
 */
public final class CertificateVariables {

    /**
     * PEM format header for certificate data.
     */
    public static final String PEM_HEADER = "-----BEGIN CERTIFICATE-----";

    /**
     * PEM format footer for certificate data.
     */
    public static final String PEM_FOOTER = "-----END CERTIFICATE-----";

    /**
     * PEM format header for attribute certificate data.
     */
    public static final String PEM_ATTRIBUTE_HEADER = "-----BEGIN ATTRIBUTE CERTIFICATE-----";

    /**
     * PEM format footer for attribute certificate data.
     */
    public static final String PEM_ATTRIBUTE_FOOTER = "-----END ATTRIBUTE CERTIFICATE-----";

    /**
     * Message indicating a malformed certificate.
     */
    public static final String MALFORMED_CERT_MESSAGE = "Malformed certificate detected.";

    /**
     * Maximum certificate length in bytes.
     */
    public static final int MAX_CERT_LENGTH_BYTES = 2048;

    /**
     * Maximum numeric precision allowed.
     */
    public static final int MAX_NUMERIC_PRECISION = 49;

    /**
     * Can store up to 160 bit values.
     */
    public static final int MAX_PUB_KEY_MODULUS_HEX_LENGTH = 1024;

    /**
     * Key usage bit for the first key operation.
     */
    public static final int KEY_USAGE_BIT0 = 0;

    /**
     * Key usage bit for the second key operation.
     */
    public static final int KEY_USAGE_BIT1 = 1;

    /**
     * Key usage bit for the third key operation.
     */
    public static final int KEY_USAGE_BIT2 = 2;

    /**
     * Key usage bit for the fourth key operation.
     */
    public static final int KEY_USAGE_BIT3 = 3;

    /**
     * Key usage bit for the fifth key operation.
     */
    public static final int KEY_USAGE_BIT4 = 4;

    /**
     * Key usage bit for the sixth key operation.
     */
    public static final int KEY_USAGE_BIT5 = 5;

    /**
     * Key usage bit for the seventh key operation.
     */
    public static final int KEY_USAGE_BIT6 = 6;

    /**
     * Key usage bit for the eighth key operation.
     */
    public static final int KEY_USAGE_BIT7 = 7;

    /**
     * Key usage bit for the ninth key operation.
     */
    public static final int KEY_USAGE_BIT8 = 8;

    /**
     * Digital signature key usage.
     */
    public static final String KEY_USAGE_DS = "DIGITAL SIGNATURE";

    /**
     * Non-repudiation key usage.
     */
    public static final String KEY_USAGE_NR = "NON-REPUDIATION";

    /**
     * Key encipherment key usage.
     */
    public static final String KEY_USAGE_KE = "KEY ENCIPHERMENT";

    /**
     * Data encipherment key usage.
     */
    public static final String KEY_USAGE_DE = "DATA ENCIPHERMENT";

    /**
     * Key agreement key usage.
     */
    public static final String KEY_USAGE_KA = "KEY AGREEMENT";

    /**
     * Key certification signature key usage.
     */
    public static final String KEY_USAGE_KC = "KEY CERT SIGN";

    /**
     * Certificate revocation list signature key usage.
     */
    public static final String KEY_USAGE_CS = "CRL SIGN";

    /**
     * Key usage for enciphering only.
     */
    public static final String KEY_USAGE_EO = "ENCIPHER ONLY";

    /**
     * Key usage for deciphering only.
     */
    public static final String KEY_USAGE_DO = "DECIPHER ONLY";

    /**
     * OID for ECDSA (Elliptic Curve Digital Signature Algorithm).
     */
    public static final String ECDSA_OID = "1.2.840.10045.4.3.2";

    /**
     * OID for ECDSA with SHA224 hash function.
     */
    public static final String ECDSA_SHA224_OID = "1.2.840.10045.4.1";

    /**
     * OID for RSA with SHA-256 hash function.
     */
    public static final String RSA256_OID = "1.2.840.113549.1.1.11";

    /**
     * OID for RSA with SHA-384 hash function.
     */
    public static final String RSA384_OID = "1.2.840.113549.1.1.12";

    /**
     * OID for RSA with SHA-512 hash function.
     */
    public static final String RSA512_OID = "1.2.840.113549.1.1.13";

    /**
     * OID for RSA with SHA-224 hash function.
     */
    public static final String RSA224_OID = "1.2.840.113549.1.1.14";

    /**
     * OID for RSA with SHA-512/224 hash function.
     */
    public static final String RSA512_224_OID = "1.2.840.113549.1.1.15";

    /**
     * OID for RSA with SHA-512/256 hash function.
     */
    public static final String RSA512_256_OID = "1.2.840.113549.1.1.16";

    /**
     * Algorithm string for RSA with SHA-256.
     */
    public static final String RSA256_STRING = "SHA256WithRSA";

    /**
     * Algorithm string for RSA with SHA-384.
     */
    public static final String RSA384_STRING = "SHA384WithRSA";

    /**
     * Algorithm string for RSA with SHA-224.
     */
    public static final String RSA224_STRING = "SHA224WithRSA";

    /**
     * Algorithm string for RSA with SHA-512.
     */
    public static final String RSA512_STRING = "SHA512WithRSA";

    /**
     * Algorithm string for RSA with SHA-512/224.
     */
    public static final String RSA512_224_STRING = "SHA512-224WithRSA";

    /**
     * Algorithm string for RSA with SHA-512/256.
     */
    public static final String RSA512_256_STRING = "SHA512-256WithRSA";

    /**
     * Algorithm string for ECDSA with SHA-256.
     */
    public static final String ECDSA_STRING = "SHA256WithECDSA";

    /**
     * Algorithm string for ECDSA with SHA-224.
     */
    public static final String ECDSA_SHA224_STRING = "SHA224WithECDSA";

    /**
     * Private constructor was created to silence checkstyle error.
     */
    private CertificateVariables() {
    }
}
