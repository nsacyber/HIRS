package hirs.structs.elements.tpm;

import hirs.structs.elements.Struct;
import hirs.structs.elements.StructElementLength;
import hirs.structs.elements.StructElements;
import lombok.Getter;

import java.util.Arrays;

/**
 * Part of the TPM Identity Request. This Structure is encrypted inside the request and is typically
 * unencrypted by an Attestation Certificate Authority.
 */
@StructElements(elements = {"algorithmId", "encryptionScheme", "keySize", "key"})
public class SymmetricKey implements Struct {

    /**
     * Algorithm ID for AES encryption.
     */
    public static final int ALGORITHM_AES = 6;

    /**
     * Scheme ID for CBC.
     */
    public static final short SCHEME_CBC = 255;

    /**
     * of the symmetric key
     */
    @Getter
    private int algorithmId;

    /**
     * the encryption scheme of the symmetric key.
     */
    @Getter
    private short encryptionScheme;

    /**
     * the size the underlying symmetric key block.
     */
    @Getter
    @StructElementLength(fieldName = "key")
    private short keySize;

    private byte[] key;

    /**
     * @return the underlying key block.
     */
    public byte[] getKey() {
        return Arrays.copyOf(key, key.length);
    }
}
