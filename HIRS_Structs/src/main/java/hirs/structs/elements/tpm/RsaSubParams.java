package hirs.structs.elements.tpm;

import hirs.structs.elements.Struct;
import hirs.structs.elements.StructElementLength;
import hirs.structs.elements.StructElements;
import lombok.Getter;

import java.util.Arrays;

/**
 * Parameters that are used to describe a particular {@link AsymmetricKeyParams} as specified by the
 * TCPA 4.20.
 */
@StructElements(elements = {"keyLength", "totalPrimes", "exponentSize", "exponent"})
public class RsaSubParams implements Struct {

    /**
     * the length of the key.
     */
    @Getter
    private int keyLength;

    /**
     * the total number of prime numbers in the key. Typically this is associated with the
     * block size.
     */
    @Getter
    private int totalPrimes;

    /**
     * the size of the exponent block.
     */
    @Getter
    @StructElementLength(fieldName = "exponent")
    private int exponentSize;

    private byte[] exponent;

    /**
     * @return the public exponent of the key
     */
    public byte[] getExponent() {
        return Arrays.copyOf(exponent, exponent.length);
    }

}
