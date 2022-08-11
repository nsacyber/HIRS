package hirs.attestationca.rest;

import hirs.attestationca.AbstractAttestationCertificateAuthority;
import hirs.attestationca.validation.SupplyChainValidationService;
import hirs.data.service.DeviceRegister;
import hirs.persist.DeviceManager;
import hirs.persist.ReferenceEventManager;
import hirs.persist.service.CertificateService;
import hirs.persist.service.ReferenceManifestService;
import hirs.structs.converters.StructConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;

/**
 * Restful implementation of the {@link hirs.attestationca.AttestationCertificateAuthority}.
 * Exposes the ACA methods as REST endpoints.
 */
@RestController
@RequestMapping("/")
public class RestfulAttestationCertificateAuthority
                                            extends AbstractAttestationCertificateAuthority {

    /**
     * Constructor.
     * @param supplyChainValidationService the supply chain service
     * @param privateKey the ACA private key
     * @param acaCertificate the ACA certificate
     * @param structConverter the struct converter
     * @param certificateService the certificate service
     * @param referenceManifestService the referenceManifestManager
     * @param deviceRegister the device register
     * @param validDays the number of days issued certs are valid
     * @param deviceManager the device manager
     * @param referenceEventManager the reference event manager
     */
    @SuppressWarnings({ "checkstyle:parameternumber" })
    @Autowired
    public RestfulAttestationCertificateAuthority(
            final SupplyChainValidationService supplyChainValidationService,
            final PrivateKey privateKey, final X509Certificate acaCertificate,
            final StructConverter structConverter,
            final CertificateService certificateService,
            final ReferenceManifestService referenceManifestService,
            final DeviceRegister deviceRegister,
            final DeviceManager deviceManager,
            final ReferenceEventManager referenceEventManager,
            @Value("${aca.certificates.validity}") final int validDays) {
        super(supplyChainValidationService, privateKey, acaCertificate, structConverter,
                certificateService, referenceManifestService,
                deviceRegister, validDays, deviceManager,
                referenceEventManager);
    }

    /*
     * (non-javadoc)
     *
     * Wrap the {@link AbstractAttestationCertificateAuthority#processIdentityRequest(byte[])}
     * with a Spring {@link RequestMapping}. Effectively, this method then will allow spring to
     * serialize and deserialize the request and responses on method invocation and
     * return, respectively.
     */
    @Override
    @ResponseBody
    @RequestMapping(value = "/identity-request/process",
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public byte[] processIdentityRequest(@RequestBody final byte[] request) {
        return super.processIdentityRequest(request);
    }

    /**
     * Listener for identity requests from TPM 2.0 provisioning.
     * @param request The request object from the provisioner.
     * @return The response to the provisioner.
     */
    @Override
    @ResponseBody
    @RequestMapping(value = "/identity-claim-tpm2/process",
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public byte[] processIdentityClaimTpm2(@RequestBody final byte[] request) {
        return super.processIdentityClaimTpm2(request);
    }

    /**
     * Endpoint for processing certificate requests for TPM 2.0 provisioning.
     *
     * @param request The credential request from the client provisioner.
     * @return The response to the client provisioner.
     */
    @Override
    @ResponseBody
    @RequestMapping(value = "/request-certificate-tpm2",
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public byte[] processCertificateRequest(@RequestBody final byte[] request) {
        return super.processCertificateRequest(request);
    }

    /*
     * (non-javadoc)
     *
     * Wrap the {@link AbstractAttestationCertificateAuthority#getPublicKey()} with a Spring
     * {@link RequestMapping} such that Spring can serialize the certificate to be returned to an
     * HTTP Request.
     */
    @Override
    @ResponseBody
    @RequestMapping(value = "/public-key", method = RequestMethod.GET)
    public byte[] getPublicKey() {
        return super.getPublicKey();
    }

}
