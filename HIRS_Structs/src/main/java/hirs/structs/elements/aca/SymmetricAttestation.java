package hirs.structs.elements.aca;

import hirs.structs.elements.Struct;
import hirs.structs.elements.StructElementLength;
import hirs.structs.elements.StructElements;
import hirs.structs.elements.tpm.SymmetricKeyParams;

import java.util.Arrays;

/**
 * Part of the Attestation Certificate Authority {@link IdentityResponseEnvelope}. This portion of
 * the envelope contains the Identity Credential that is signed by the ACA. This along with the key
 * parameters are typically sent to the TPM to activate an Identity.
 */
@StructElements(elements = {"credentialSize", "algorithm", "credential"})
public class SymmetricAttestation implements Struct {

    @StructElementLength(fieldName = "credential")
    private int credentialSize;

    private SymmetricKeyParams algorithm;

    private byte[] credential;

    /**
     * Gets the credential block size.
     *
     * @return the size of the credential block
     */
    public int getCredentialSize() {
        return credentialSize;
    }

    /**
     * Gets the key parameters for the credential.
     *
     * @return the algorithm and other meta data regarding the key
     */
    public SymmetricKeyParams getAlgorithm() {
        return algorithm;
    }

    /**
     * Gets the credential block.
     *
     * @return the credential block
     */
    public byte[] getCredential() {
        return Arrays.copyOf(credential, credential.length);
    }
}
