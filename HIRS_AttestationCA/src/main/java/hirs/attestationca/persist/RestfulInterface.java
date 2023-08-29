package hirs.attestationca.persist;

/**
 * Defines the responsibilities of the Attestation Certificate Authority.
 */
public interface RestfulInterface {

    byte[] processIdentityRequest(byte[] identityRequest);

    byte[] processIdentityClaimTpm2(byte[] identityClaim);

    byte[] processCertificateRequest(byte[] certificateRequest);

}
