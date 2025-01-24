package hirs.structs.elements.tpm;

import hirs.structs.elements.Struct;
import hirs.structs.elements.StructElementLength;
import hirs.structs.elements.StructElements;
import lombok.Getter;

import java.util.Arrays;

/**
 * Represents a dynamic key parameters data structure that is enclosed inside a {@link
 * SymmetricKeyParams}.
 */
@StructElements(elements = {"keyLength", "blockSize", "ivSize", "iv"})
public class SymmetricSubParams implements Struct {

    /**
     * the key length.
     */
    @Getter
    private int keyLength;

    /**
     * the block size.
     */
    @Getter
    private int blockSize;

    /**
     * the IV size.
     */
    @Getter
    @StructElementLength(fieldName = "iv")
    private int ivSize;

    private byte[] iv;

    /**
     * @return the IV.
     */
    public byte[] getIv() {
        return Arrays.copyOf(iv, iv.length);
    }
}
