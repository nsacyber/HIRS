package hirs.attestationca.persist.provision.service;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service class responsible for handling both certificate and identity claim requests made by the provisioner.
 */
@Service
@Log4j2
public class AttestationCertificateAuthorityServiceImpl implements AttestationCertificateAuthorityService {
    private final CertificateRequestProcessorService certificateRequestProcessorService;
    private final IdentityClaimProcessorService identityClaimProcessorService;

    /**
     * Constructor.
     *
     * @param certificateRequestProcessorService certificate request processor service
     * @param identityClaimProcessorService      identity claim processor service
     */
    @Autowired
    public AttestationCertificateAuthorityServiceImpl(
            final CertificateRequestProcessorService certificateRequestProcessorService,
            final IdentityClaimProcessorService identityClaimProcessorService) {
        this.certificateRequestProcessorService = certificateRequestProcessorService;
        this.identityClaimProcessorService = identityClaimProcessorService;
    }

    /**
     * Processes the provided identity claim.
     *
     * @param identityClaim a byte array representation of the identity claim
     * @return processed identity claim response
     */
    public byte[] processIdentityClaimTpm2(final byte[] identityClaim) {
        return identityClaimProcessorService.processIdentityClaimTpm2(identityClaim);
    }

    /**
     * Processes the provided certificate request.
     *
     * @param certificateRequest a byte array representation of the certificate request
     * @return processed certificate request response
     */
    public byte[] processCertificateRequest(final byte[] certificateRequest) {
        return certificateRequestProcessorService.processCertificateRequest(certificateRequest);
    }

    /**
     * Retrieves the encoded public key of the leaf certificate.
     *
     * @return encoded public key of the leaf certificate
     */
    public byte[] getLeafACACertPublicKey() {
        return certificateRequestProcessorService.getLeafACACertificatePublicKey();
    }
}
