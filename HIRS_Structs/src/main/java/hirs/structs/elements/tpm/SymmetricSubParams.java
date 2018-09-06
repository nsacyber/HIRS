package hirs.structs.elements.tpm;

import hirs.structs.elements.Struct;
import hirs.structs.elements.StructElementLength;
import hirs.structs.elements.StructElements;

import java.util.Arrays;

/**
 * Represents a dynamic key parameters data structure that is enclosed inside a {@link
 * SymmetricKeyParams}.
 */
@StructElements(elements = { "keyLength", "blockSize", "ivSize", "iv" })
public class SymmetricSubParams implements Struct {

    private int keyLength;

    private int blockSize;

    @StructElementLength(fieldName = "iv")
    private int ivSize;

    private byte[] iv;

    /**
     * @return the key length.
     */
    public int getKeyLength() {
        return keyLength;
    }

    /**
     * @return the block size.
     */
    public int getBlockSize() {
        return blockSize;
    }

    /**
     * @return the IV size.
     */
    public int getIvSize() {
        return ivSize;
    }

    /**
     * @return the IV.
     */
    public byte[] getIv() {
        return Arrays.copyOf(iv, iv.length);
    }
}
