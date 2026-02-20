package hirs.attestationca.persist;

import hirs.attestationca.persist.provision.CertificateRequestProcessor;
import hirs.attestationca.persist.provision.IdentityClaimProcessor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.security.GeneralSecurityException;

/**
 * Service layer class responsible for handling both certificate and identity claim requests made by the provisioner.
 */
@Service
@Log4j2
public class AttestationCertificateAuthorityServiceImpl implements AttestationCertificateAuthorityService {
    private final CertificateRequestProcessor certificateRequestProcessor;
    private final IdentityClaimProcessor identityClaimProcessor;

    /**
     * Constructor.
     *
     * @param certificateRequestProcessor certificate request processor service
     * @param identityClaimProcessor      identity claim processor service
     */
    public AttestationCertificateAuthorityServiceImpl(
            final CertificateRequestProcessor certificateRequestProcessor,
            final IdentityClaimProcessor identityClaimProcessor) {
        this.certificateRequestProcessor = certificateRequestProcessor;
        this.identityClaimProcessor = identityClaimProcessor;
    }

    /**
     * Processes the provided identity claim.
     *
     * @param identityClaim a byte array representation of the identity claim
     * @return processed identity claim response
     */
    public byte[] processIdentityClaimTpm2(final byte[] identityClaim) throws GeneralSecurityException {
        return this.identityClaimProcessor.processIdentityClaimTpm2(identityClaim);
    }

    /**
     * Processes the provided certificate request.
     *
     * @param certificateRequest a byte array representation of the certificate request
     * @return processed certificate request response
     */
    public byte[] processCertificateRequest(final byte[] certificateRequest) throws GeneralSecurityException {
        return this.certificateRequestProcessor.processCertificateRequest(certificateRequest);
    }

    /**
     * Retrieves the encoded public key.
     *
     * @return encoded public key
     */
    public byte[] getLeafACACertPublicKey() {
        return this.certificateRequestProcessor.getLeafACACertificatePublicKey();
    }
}
