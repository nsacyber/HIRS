package hirs.attestationca.persist;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for the ACA that communicates with the Provisioner during the provisioning process.
 */
@Log4j2
@RestController
@RequestMapping("/HIRS_AttestationCA")
public class AttestationCertificateAuthorityRestController {
    private final AttestationCertificateAuthorityService attestationCertificateAuthorityService;

    /**
     * Constructor.
     *
     * @param attestationCertificateAuthorityService Attestation Certificate Authority service
     */
    @Autowired
    public AttestationCertificateAuthorityRestController(
            final AttestationCertificateAuthorityService attestationCertificateAuthorityService) {
        this.attestationCertificateAuthorityService = attestationCertificateAuthorityService;
    }

    /**
     * Processes a given IdentityClaim and generates a response containing an encrypted nonce to be returned by the
     * client in a future handshake request.
     *
     * @param identityClaim The request object from the provisioner.
     * @return The response to the provisioner.
     */
    @ResponseBody
    @PostMapping(value = "/identity-claim-tpm2/process", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public byte[] processIdentityClaimTpm2(@RequestBody final byte[] identityClaim) {
        return this.attestationCertificateAuthorityService.processIdentityClaimTpm2(identityClaim);
    }

    /**
     * Processes a given Certificate Request  and generates a response containing the signed, public certificate for
     * the client's desired attestation key, if the correct nonce is supplied.
     *
     * @param certificateRequest request containing nonce from earlier identity
     *                           claim handshake
     * @return The response to the client provisioner.
     */
    @ResponseBody
    @PostMapping(value = "/request-certificate-tpm2", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public byte[] processCertificateRequest(@RequestBody final byte[] certificateRequest) {
        return this.attestationCertificateAuthorityService.processCertificateRequest(certificateRequest);
    }

    /**
     * Processes a GET request to retrieve the byte array representation of the public key.
     *
     * @return byte array representation of the public key
     */
    @ResponseBody
    @GetMapping("/public-key")
    public byte[] getLeafACACertPublicKey() {
        return this.attestationCertificateAuthorityService.getLeafACACertPublicKey();
    }
}
