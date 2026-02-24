package hirs.attestationca.persist;

import java.security.GeneralSecurityException;

/**
 * Interface that defines the responsibilities of the Attestation Certificate Authority service.
 */
public interface AttestationCertificateAuthorityService {

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
    byte[] processCertificateRequest(byte[] certificateRequest) throws GeneralSecurityException;


    /**
     * Retrieves the encoded public key of the leaf certificate.
     *
     * @return encoded public key of the leaf certificate
     */
    byte[] getLeafACACertPublicKey();
}
