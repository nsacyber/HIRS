package hirs.attestationca.persist.provision;

/**
 * Defines the responsibilities of the Attestation Certificate Authority.
 */
public interface RestfulInterface {

    /**
     * Processes the provided identity claim.
     *
     * @param identityClaim a byte array representation of the identity claim
     * @return a byte array representation of the identity claim response
     */
    byte[] processIdentityClaimTpm2(byte[] identityClaim);

    /**
     * Processes the provided certificate request.
     *
     * @param certificateRequest a byte array representation of the certificate request
     * @return a byte array representation of the certificate request response
     */
    byte[] processCertificateRequest(byte[] certificateRequest);

}
