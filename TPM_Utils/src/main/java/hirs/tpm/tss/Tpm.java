package hirs.tpm.tss;

import java.io.IOException;

/**
 * Defines the functionality of a Tpm as specified in the TCG TSS Specification.
 */
public interface Tpm {

    /**
     * Takes ownership of an active but unowned Tpm.
     */
    void takeOwnership();

    /**
     * Obtain the public Endorsement Credential modulus.
     *
     * @return the endorsement credential modulus
     * @see #collateIdentityRequest(byte[], String)
     */
    byte[] getEndorsementCredentialModulus();

    /**
     * Obtains the full Endorsement Credential stored in the TPM.
     * @return the EC on the TPM, if present, otherwise null
     * @throws IOException if there's an error writing the EC to a temporary file for processing
     */
    byte[] getEndorsementCredential() throws IOException;

    /**
     * Instructs the Tpm to collate an identity request as stated in the TSS specification.
     *
     * @param acaPublicKey non null, ACA PK information.
     * @param uuid         non null, non empty, UUID for the identity.
     * @return the identity request. to be later attested.
     */
    byte[] collateIdentityRequest(byte[] acaPublicKey, String uuid);

    /**
     * Activates the identity within the TPM given the label and ACA symmetric and asymmetric
     * identity response blobs. The return will be the identity credential that is activated within
     * the TPM.
     *
     * @param asymmetricBlob from ACA identity response
     * @param symmetricBlob  from ACA identity response
     * @param uuid           of the identity request
     * @return the activated identity credential
     */
    byte[] activateIdentity(byte[] asymmetricBlob, byte[] symmetricBlob, String uuid);

    /**
     * Queries the TPM for quote data given the specified pcrValues using the specified UUID.
     *
     * @param pcrValues cannot be null or empty
     * @param nonce cannot be null or empty
     * @param uuid cannot be null or empty
     * @return quote data
     */
    byte[] getQuote(String pcrValues, String nonce, String uuid);
}
