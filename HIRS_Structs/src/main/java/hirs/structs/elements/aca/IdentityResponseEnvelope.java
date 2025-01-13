package hirs.structs.elements.aca;

import hirs.structs.elements.Struct;
import hirs.structs.elements.StructElementLength;
import hirs.structs.elements.StructElements;
import lombok.Getter;

import java.util.Arrays;

/**
 * Data structure used by the ACA to respond back to a client's {@link IdentityRequestEnvelope}.
 */
@StructElements(elements = {"asymmetricContentsSize", "asymmetricContents",
        "symmetricAttestation"})
public class IdentityResponseEnvelope implements Struct {

    /**
     * the asymmetric contents block size
     */
    @Getter
    @StructElementLength(fieldName = "asymmetricContents")
    private int asymmetricContentsSize;

    private byte[] asymmetricContents;

    /**
     * the symmetric attestation.
     */
    @Getter
    private SymmetricAttestation symmetricAttestation;

    /**
     * Gets the asymmetric contents block.
     *
     * @return the asymmetric contents block.
     */
    public byte[] getAsymmetricContents() {
        return Arrays.copyOf(asymmetricContents, asymmetricContents.length);
    }

}
