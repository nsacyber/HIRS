package hirs.structs.elements.tpm;

import hirs.structs.elements.Struct;
import hirs.structs.elements.StructElements;

/**
 * As specified in TCPA Main Specification section 4.27.3. This structure contains the public
 * portion of an asymmetric key pair. It contains all the information necessary for it's unambiguous
 * usage.
 */
@StructElements(elements = { "asymmetricKeyParams", "storePubKey" })
public class AsymmetricPublicKey implements Struct {

    /**
     * The default RSA algorithm ID.
     */
    public static final int DEFAULT_RSA_ALG_ID = 1;

    /**
     * The default key length.
     */
    public static final int DEFAULT_KEY_LENGTH = 2048;

    /**
     * The default total number of primes.
     */
    public static final int DEFAULT_PRIME_TOTAL = 2;

    /**
     * The default RSA encryption scheme.
     */
    public static final short DEFAULT_RSA_ENCRYPTION_SCHEME = 0x3;

    /**
     * The default RSA signature scheme.
     */
    public static final short DEFAULT_RSA_SIGNATURE_SCHEME = 0x1;

    private AsymmetricKeyParams asymmetricKeyParams;

    private StorePubKey storePubKey;

    /**
     * Default constructor. This is required for the {@link
     * hirs.structs.converters.StructConverter}.
     */
    public AsymmetricPublicKey() {
    }

    /**
     * @return information regarding this key
     */
    public AsymmetricKeyParams getAsymmetricKeyParams() {
        return asymmetricKeyParams;
    }

    /**
     * @return the public as described by the key parameters.
     */
    public StorePubKey getStorePubKey() {
        return storePubKey;
    }
}
