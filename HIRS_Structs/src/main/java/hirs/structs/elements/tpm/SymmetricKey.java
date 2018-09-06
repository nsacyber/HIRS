package hirs.structs.elements.tpm;

import hirs.structs.elements.Struct;
import hirs.structs.elements.StructElementLength;
import hirs.structs.elements.StructElements;

import java.util.Arrays;

/**
 * Part of the TPM Identity Request. This Structure is encrypted inside the request and is typically
 * unencrypted by an Attestation Certificate Authority.
 */
@StructElements(elements = { "algorithmId", "encryptionScheme", "keySize", "key" })
public class SymmetricKey implements Struct {

    /**
     * Algorithm ID for AES encryption.
     */
    public static final int ALGORITHM_AES = 6;

    /**
     * Scheme ID for CBC.
     */
    public static final short SCHEME_CBC = 255;

    private int algorithmId;

    private short encryptionScheme;

    @StructElementLength(fieldName = "key")
    private short keySize;

    private byte[] key;

    /**
     * @return of the symmetric key
     */
    public int getAlgorithmId() {
        return algorithmId;
    }

    /**
     * @return the encryption scheme of the symmetric key
     */
    public short getEncryptionScheme() {
        return encryptionScheme;
    }

    /**
     * @return the size the underlying symmetric key block.
     */
    public short getKeySize() {
        return keySize;
    }

    /**
     * @return the underlying key block.
     */
    public byte[] getKey() {
        return Arrays.copyOf(key, key.length);
    }
}
