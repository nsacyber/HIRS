package hirs.attestationca.persist.entity.userdefined.certificate;

public final class CertificateVariables {

    /**
     *
     */
    public static final String PEM_HEADER = "-----BEGIN CERTIFICATE-----";
    /**
     *
     */
    public static final String PEM_FOOTER = "-----END CERTIFICATE-----";
    /**
     *
     */
    public static final String PEM_ATTRIBUTE_HEADER = "-----BEGIN ATTRIBUTE CERTIFICATE-----";
    /**
     *
     */
    public static final String PEM_ATTRIBUTE_FOOTER = "-----END ATTRIBUTE CERTIFICATE-----";
    /**
     *
     */
    public static final String MALFORMED_CERT_MESSAGE = "Malformed certificate detected.";
    /**
     * Maximum certificate length in bytes.
     */
    public static final int MAX_CERT_LENGTH_BYTES = 2048;
    /**
     *
     */
    public static final int MAX_NUMERIC_PRECISION = 49;
    /**
     * Can store up to 160 bit values.
     */
    public static final int MAX_PUB_KEY_MODULUS_HEX_LENGTH = 1024;
    /**
     *
     */
    public static final int KEY_USAGE_BIT0 = 0;
    /**
     *
     */
    public static final int KEY_USAGE_BIT1 = 1;
    /**
     *
     */
    public static final int KEY_USAGE_BIT2 = 2;
    /**
     *
     */
    public static final int KEY_USAGE_BIT3 = 3;
    /**
     *
     */
    public static final int KEY_USAGE_BIT4 = 4;
    /**
     *
     */
    public static final int KEY_USAGE_BIT5 = 5;
    /**
     *
     */
    public static final int KEY_USAGE_BIT6 = 6;
    /**
     *
     */
    public static final int KEY_USAGE_BIT7 = 7;
    /**
     *
     */
    public static final int KEY_USAGE_BIT8 = 8;
    /**
     *
     */
    public static final String KEY_USAGE_DS = "DIGITAL SIGNATURE";
    /**
     *
     */
    public static final String KEY_USAGE_NR = "NON-REPUDIATION";
    /**
     *
     */
    public static final String KEY_USAGE_KE = "KEY ENCIPHERMENT";
    /**
     *
     */
    public static final String KEY_USAGE_DE = "DATA ENCIPHERMENT";
    /**
     *
     */
    public static final String KEY_USAGE_KA = "KEY AGREEMENT";
    /**
     *
     */
    public static final String KEY_USAGE_KC = "KEY CERT SIGN";
    /**
     *
     */
    public static final String KEY_USAGE_CS = "CRL SIGN";
    /**
     *
     */
    public static final String KEY_USAGE_EO = "ENCIPHER ONLY";
    /**
     *
     */
    public static final String KEY_USAGE_DO = "DECIPHER ONLY";
    /**
     *
     */
    public static final String ECDSA_OID = "1.2.840.10045.4.3.2";
    /**
     *
     */
    public static final String ECDSA_SHA224_OID = "1.2.840.10045.4.1";
    /**
     *
     */
    public static final String RSA256_OID = "1.2.840.113549.1.1.11";
    /**
     *
     */
    public static final String RSA384_OID = "1.2.840.113549.1.1.12";
    /**
     *
     */
    public static final String RSA512_OID = "1.2.840.113549.1.1.13";
    /**
     *
     */
    public static final String RSA224_OID = "1.2.840.113549.1.1.14";
    /**
     *
     */
    public static final String RSA512_224_OID = "1.2.840.113549.1.1.15";
    /**
     *
     */
    public static final String RSA512_256_OID = "1.2.840.113549.1.1.16";
    /**
     *
     */
    public static final String RSA256_STRING = "SHA256WithRSA";
    /**
     *
     */
    public static final String RSA384_STRING = "SHA384WithRSA";
    /**
     *
     */
    public static final String RSA224_STRING = "SHA224WithRSA";
    /**
     *
     */
    public static final String RSA512_STRING = "SHA512WithRSA";
    /**
     *
     */
    public static final String RSA512_224_STRING = "SHA512-224WithRSA";
    /**
     *
     */
    public static final String RSA512_256_STRING = "SHA512-256WithRSA";
    /**
     *
     */
    public static final String ECDSA_STRING = "SHA256WithECDSA";
    /**
     *
     */
    public static final String ECDSA_SHA224_STRING = "SHA224WithECDSA";

    /**
     * Private constructor was created to silence checkstyle error.
     */
    private CertificateVariables() {
    }
}
