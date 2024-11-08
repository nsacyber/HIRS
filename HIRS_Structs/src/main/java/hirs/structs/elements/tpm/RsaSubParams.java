package hirs.structs.elements.tpm;

import hirs.structs.elements.Struct;
import hirs.structs.elements.StructElementLength;
import hirs.structs.elements.StructElements;

import java.util.Arrays;

/**
 * Parameters that are used to describe a particular {@link AsymmetricKeyParams} as specified by the
 * TCPA 4.20.
 */
@StructElements(elements = {"keyLength", "totalPrimes", "exponentSize", "exponent"})
public class RsaSubParams implements Struct {

    private int keyLength;

    private int totalPrimes;

    @StructElementLength(fieldName = "exponent")
    private int exponentSize;

    private byte[] exponent;

    /**
     * @return the length of the key
     */
    public int getKeyLength() {
        return keyLength;
    }

    /**
     * @return the total number of prime numbers in the key. Typically this is associated with the
     * block size.
     */
    public int getTotalPrimes() {
        return totalPrimes;
    }

    /**
     * @return the public exponent of the key
     */
    public byte[] getExponent() {
        return Arrays.copyOf(exponent, exponent.length);
    }

    /**
     * @return the size of the exponent block.
     */
    public int getExponentSize() {
        return exponentSize;
    }
}
