package hirs.attestationca.persist;

import hirs.attestationca.persist.entity.manager.CertificateRepository;
import hirs.attestationca.persist.entity.manager.ComponentResultRepository;
import hirs.attestationca.persist.entity.manager.DeviceRepository;
import hirs.attestationca.persist.entity.manager.IssuedCertificateRepository;
import hirs.attestationca.persist.entity.manager.PolicyRepository;
import hirs.attestationca.persist.entity.manager.ReferenceDigestValueRepository;
import hirs.attestationca.persist.entity.manager.ReferenceManifestRepository;
import hirs.attestationca.persist.entity.manager.TPM2ProvisionerStateRepository;
import hirs.attestationca.persist.entity.userdefined.certificate.IssuedAttestationCertificate;
import hirs.attestationca.persist.service.SupplyChainValidationService;
import hirs.structs.converters.StructConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;

/**
 * Restful implementation of the {@link AttestationCertificateAuthority}.
 * Exposes the ACA methods as REST endpoints.
 */
@PropertySource(value = "file:/etc/hirs/aca/application.properties",
        ignoreResourceNotFound = true)
@RestController
@RequestMapping("/")
public class RestfulAttestationCertificateAuthority extends AttestationCertificateAuthority implements RestfulInterface {

    /**
     * Constructor.
     *
     * @param supplyChainValidationService   scp service
     * @param privateKey                     the ACA private key
     * @param acaCertificate                 the ACA certificate
     * @param componentResultRepository      the component result repository
     * @param certificateRepository          the certificate manager
     * @param referenceManifestRepository    the referenceManifestManager
     * @param validDays                      the number of days issued certs are valid
     * @param deviceRepository               the device manager
     * @param referenceDigestValueRepository the reference event repository
     * @param policyRepository               the provisioning policy entity
     * @param tpm2ProvisionerStateRepository the provisioner state
     */
    @SuppressWarnings({"checkstyle:parameternumber"})
    @Autowired
    public RestfulAttestationCertificateAuthority(
            final SupplyChainValidationService supplyChainValidationService,
            final PrivateKey privateKey, final X509Certificate acaCertificate,
            final StructConverter structConverter,
            final ComponentResultRepository componentResultRepository,
            final CertificateRepository certificateRepository,
            final IssuedCertificateRepository issuedCertificateRepository,
            final ReferenceManifestRepository referenceManifestRepository,
            final DeviceRepository deviceRepository,
            final ReferenceDigestValueRepository referenceDigestValueRepository,
            @Value("${aca.certificates.validity}") final int validDays,
            final PolicyRepository policyRepository,
            final TPM2ProvisionerStateRepository tpm2ProvisionerStateRepository) {
        super(supplyChainValidationService, privateKey, acaCertificate, structConverter,
                componentResultRepository, certificateRepository, issuedCertificateRepository,
                referenceManifestRepository,
                validDays, deviceRepository,
                referenceDigestValueRepository, policyRepository, tpm2ProvisionerStateRepository);
    }

    /**
     * Processes a given IdentityRequestEnvelope and
     * generates a IdentityResponseEnvelope. In most cases,
     * a client will generate the request using the TPM "Collate Identity" process.
     *
     * Wrap the {@link AttestationCertificateAuthority#processIdentityRequest(byte[])}
     * with a Spring {@link org.springframework.web.bind.annotation.RequestMapping}. Effectively, this method then will allow spring to
     * serialize and deserialize the request and responses on method invocation and
     * return, respectively.
     *
     * @param identityRequest generated during the collate identity process with a Tpm
     * @return response for the request
     */
    @Override
    @ResponseBody
    @RequestMapping(value = "/identity-request/process",
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public byte[] processIdentityRequest(@RequestBody final byte[] identityRequest) {
        return super.processIdentityRequest(identityRequest);
    }

    /**
     * Listener for identity requests from TPM 2.0 provisioning.
     *
     * Processes a given IdentityClaim and generates a response
     * containing an encrypted nonce to be returned by the client in
     * a future handshake request.
     *
     * @param identityClaim The request object from the provisioner.
     * @return The response to the provisioner.
     */
    @Override
    @ResponseBody
    @RequestMapping(value = "/identity-claim-tpm2/process",
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public byte[] processIdentityClaimTpm2(@RequestBody final byte[] identityClaim) {
        return super.processIdentityClaimTpm2(identityClaim);
    }

    /**
     * Processes a given CertificateRequest
     * and generates a response containing the signed, public certificate for
     * the client's desired attestation key, if the correct nonce is supplied.
     *
     * @param certificateRequest request containing nonce from earlier identity
     *      *                           claim handshake
     * @return The response to the client provisioner.
     */
    @Override
    @ResponseBody
    @RequestMapping(value = "/request-certificate-tpm2",
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public byte[] processCertificateRequest(@RequestBody final byte[] certificateRequest) {
        return super.processCertificateRequest(certificateRequest);
    }

    /**
     * (non-javadoc)
     * <p>
     * Wrap the {@link AttestationCertificateAuthority#getPublicKey()} with a Spring
     * {@link org.springframework.web.bind.annotation.RequestMapping} such that Spring can serialize the certificate to be returned to an
     * HTTP Request.
     */
    @Override
    @ResponseBody
    @RequestMapping(value = "/public-key", method = RequestMethod.GET)
    public byte[] getPublicKey() {
        return super.getPublicKey();
    }
}
