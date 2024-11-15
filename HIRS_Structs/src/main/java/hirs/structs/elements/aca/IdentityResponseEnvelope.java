package hirs.structs.elements.aca;

import hirs.structs.elements.Struct;
import hirs.structs.elements.StructElementLength;
import hirs.structs.elements.StructElements;

import java.util.Arrays;

/**
 * Data structure used by the ACA to respond back to a client's {@link IdentityRequestEnvelope}.
 */
@StructElements(elements = {"asymmetricContentsSize", "asymmetricContents",
        "symmetricAttestation"})
public class IdentityResponseEnvelope implements Struct {

    @StructElementLength(fieldName = "asymmetricContents")
    private int asymmetricContentsSize;

    private byte[] asymmetricContents;

    private SymmetricAttestation symmetricAttestation;

    /**
     * Gets the asymmetric contents block.
     *
     * @return the asymmetric contents block.
     */
    public byte[] getAsymmetricContents() {
        return Arrays.copyOf(asymmetricContents, asymmetricContents.length);
    }

    /**
     * Gets the asymmetric contents block size.
     *
     * @return the asymmetric contents block size
     */
    public int getAsymmetricContentsSize() {
        return asymmetricContentsSize;
    }

    /**
     * Gets the symmetric attestation.
     *
     * @return the symmetric attestation.
     */
    public SymmetricAttestation getSymmetricAttestation() {
        return symmetricAttestation;
    }
}
