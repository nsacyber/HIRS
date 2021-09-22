package hirs.attestationca;

/**
 * Defines the responsibilities of the Attestation Certificate Authority.
 */
public interface AttestationCertificateAuthority {

    /**
     * The default size for IV blocks.
     */
    public int DEFAULT_IV_SIZE = 16;

    /**
     * Processes a given {@link hirs.structs.elements.aca.IdentityRequestEnvelope} and
     * generates a {@link hirs.structs.elements.aca.IdentityResponseEnvelope}. In most cases,
     * a client will generate the request using the TPM "Collate Identity" process.
     *
     * @param identityRequest generated during the collate identity process with a Tpm
     * @return response for the request
     */
    byte[] processIdentityRequest(byte[] identityRequest);

    /**
     * Processes a given
     * {@link hirs.attestationca.configuration.provisionerTpm2.ProvisionerTpm2.IdentityClaim} and
     * generates a response containing an encrypted nonce to be returned by the client in
     * a future handshake request.
     *
     * @param identityClaim generated during the create identity claim process on a TPM2 Provisioner
     * @return response for the request
     */
    byte[] processIdentityClaimTpm2(byte[] identityClaim);

    /**
     * Processes a given
     * {@link hirs.attestationca.configuration.provisionerTpm2.ProvisionerTpm2.CertificateRequest}
     * and generates a response containing the signed, public certificate for
     * the client's desired attestation key, if the correct nonce is supplied.
     *
     * @param certificateRequest request containing nonce from earlier identity
     *                           claim handshake
     * @return response for the request
     */
    byte[] processCertificateRequest(byte[] certificateRequest);

    /**
     * Issues the PK of the ACA public/private key pair.
     *
     * @return public key of the attestation certificate authority
     */
    byte[] getPublicKey();
}
