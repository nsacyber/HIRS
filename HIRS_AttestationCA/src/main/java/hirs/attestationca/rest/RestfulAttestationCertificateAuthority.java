package hirs.attestationca.rest;

import hirs.attestationca.IdentityProcessingException;
import hirs.persist.DBManager;
import hirs.persist.TPM2ProvisionerState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import hirs.attestationca.AbstractAttestationCertificateAuthority;
import hirs.attestationca.service.SupplyChainValidationService;
import hirs.data.service.DeviceRegister;
import hirs.persist.CertificateManager;
import hirs.persist.DeviceManager;
import hirs.structs.converters.StructConverter;

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
     * @param certificateManager the certificate manager
     * @param deviceRegister the device register
     * @param validDays the number of days issued certs are valid
     * @param deviceManager the device manager
     * @param tpm2ProvisionerStateDBManager the DBManager for persisting provisioner state
     */
    @SuppressWarnings({ "checkstyle:parameternumber" })
    @Autowired
    public RestfulAttestationCertificateAuthority(
            final SupplyChainValidationService supplyChainValidationService,
            final PrivateKey privateKey, final X509Certificate acaCertificate,
            final StructConverter structConverter,
            final CertificateManager certificateManager,
            final DeviceRegister deviceRegister,
            final DeviceManager deviceManager,
            final DBManager<TPM2ProvisionerState> tpm2ProvisionerStateDBManager,
            @Value("${aca.certificates.validity}") final int validDays) {
        super(supplyChainValidationService, privateKey, acaCertificate, structConverter,
                certificateManager, deviceRegister, validDays, deviceManager,
                tpm2ProvisionerStateDBManager);
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
    @RequestMapping(value = "/identity-request/process", method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE,
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
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
            consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE,
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
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
            consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE,
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
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
    @RequestMapping(value = "/public-key", method = RequestMethod.GET,
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public byte[] getPublicKey() {
        return super.getPublicKey();
    }

    /**
     * Handle processing of exceptions for ACA REST API.
     * @param e exception thrown during invocation of ACA REST API
     * @return exception thrown during invocation of ACA REST API
     */
    @ExceptionHandler
    @ResponseBody
    @ResponseStatus(code = HttpStatus.INTERNAL_SERVER_ERROR)
    public Exception handleException(final Exception e) {
        if (e instanceof IdentityProcessingException) {
            LOG.error("Processing exception while provisioning", e.getMessage(), e);
        } else {
            LOG.error(String.format("Encountered unexpected error while processing identity "
                    + "claim: %s", e.getMessage()), e);
        }
        return e;
    }

}
