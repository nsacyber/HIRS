package hirs.structs.elements.tpm;

import hirs.structs.elements.Struct;
import hirs.structs.elements.StructElementLength;
import hirs.structs.elements.StructElements;
import lombok.Getter;

import java.util.Arrays;

/**
 * As specified in TCPA Main Specification section 4.27.2. This structure represents a public key of
 * an asymmetric key pair.
 */
@StructElements(elements = {"keyLength", "key"})
public class StorePubKey implements Struct {
    
    @Getter
    @StructElementLength(fieldName = "key")
    private int keyLength;

    private byte[] key;

    /**
     * @return contains the public key information which varies depending on the key algorithm. In
     * example, if an RSA key, this field will represent the RSA public modulus.
     */
    public byte[] getKey() {
        return Arrays.copyOf(key, key.length);
    }
}
