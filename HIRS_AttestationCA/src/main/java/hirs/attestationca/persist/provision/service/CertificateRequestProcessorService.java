package hirs.attestationca.persist.provision.service;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import hirs.attestationca.configuration.provisionerTpm2.ProvisionerTpm2;
import hirs.attestationca.persist.entity.manager.DeviceRepository;
import hirs.attestationca.persist.entity.manager.PolicyRepository;
import hirs.attestationca.persist.entity.tpm.TPM2ProvisionerState;
import hirs.attestationca.persist.entity.userdefined.Device;
import hirs.attestationca.persist.entity.userdefined.PolicySettings;
import hirs.attestationca.persist.entity.userdefined.SupplyChainValidationSummary;
import hirs.attestationca.persist.entity.userdefined.certificate.EndorsementCredential;
import hirs.attestationca.persist.entity.userdefined.certificate.PlatformCredential;
import hirs.attestationca.persist.entity.userdefined.info.TPMInfo;
import hirs.attestationca.persist.entity.userdefined.report.DeviceInfoReport;
import hirs.attestationca.persist.enums.AppraisalStatus;
import hirs.attestationca.persist.exceptions.CertificateProcessingException;
import hirs.attestationca.persist.provision.helper.IssuedCertificateAttributeHelper;
import hirs.attestationca.persist.provision.helper.ParsedTpmPublic;
import hirs.attestationca.persist.provision.helper.ProvisionUtils;
import hirs.attestationca.persist.provision.helper.TpmPublicHelper;
import hirs.attestationca.persist.validation.SupplyChainValidationService;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.ArrayUtils;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * Service class responsible for processing the Provisioner's Certificate Request.
 */
@Service
@Log4j2
public class CertificateRequestProcessorService {
    private final SupplyChainValidationService supplyChainValidationService;
    private final CredentialManagementService credentialManagementService;
    private final Tpm2ProvisionerStateService tpm2ProvisionerStateService;
    private final DeviceRepository deviceRepository;
    private final PolicyRepository policyRepository;
    private final X509Certificate acaCertificate;
    private final int certificateValidityInDays;
    private final PrivateKey privateKey;

    /**
     * Constructor.
     *
     * @param supplyChainValidationService object that is used to run provisioning
     * @param credentialManagementService  credential management service
     * @param tpm2ProvisionerStateService  tpm2 provisioner state service
     * @param deviceRepository             database connector for Devices.
     * @param policyRepository             db connector for policies.
     * @param privateKey                   private key used for communication authentication
     * @param acaCertificate               object used to create credential
     * @param certificateValidityInDays    int for the time in which a certificate is valid.
     */
    @Autowired
    public CertificateRequestProcessorService(final SupplyChainValidationService supplyChainValidationService,
                                              final CredentialManagementService credentialManagementService,
                                              final Tpm2ProvisionerStateService tpm2ProvisionerStateService,
                                              final DeviceRepository deviceRepository,
                                              final PolicyRepository policyRepository,
                                              final PrivateKey privateKey,
                                              @Qualifier("leafACACert") final X509Certificate acaCertificate,
                                              @Value("${aca.certificates.validity}") final int certificateValidityInDays
    ) {
        this.credentialManagementService = credentialManagementService;
        this.tpm2ProvisionerStateService = tpm2ProvisionerStateService;
        this.certificateValidityInDays = certificateValidityInDays;
        this.supplyChainValidationService = supplyChainValidationService;
        this.deviceRepository = deviceRepository;
        this.acaCertificate = acaCertificate;
        this.policyRepository = policyRepository;
        this.privateKey = privateKey;
    }

    /**
     * Retrieves the byte array representation of the ACA certificate public key.
     *
     * @return byte array representation of the ACA certificate public key
     */
    public byte[] getLeafACACertificatePublicKey() {
        return acaCertificate.getPublicKey().getEncoded();
    }

    /**
     * Basic implementation of the ACA processCertificateRequest method. Parses the nonce, validates its correctness,
     * generates the signed, public attestation certificate, stores it, and returns it to the client.
     *
     * @param certificateRequestByteArray request containing nonce from earlier identity claim handshake
     * @return a certificateResponse containing the signed certificate
     */
    public byte[] processCertificateRequest(final byte[] certificateRequestByteArray) {
        log.info("Certificate Request has been received and is ready to be processed");

        if (ArrayUtils.isEmpty(certificateRequestByteArray)) {
            final String errorMsg = "The CertificateRequest sent by the client cannot be null or empty.";
            log.error(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        final PolicySettings policySettings = policyRepository.findByName("Default");

        // attempt to deserialize Protobuf CertificateRequest
        ProvisionerTpm2.CertificateRequest certificateRequest;
        try {
            certificateRequest = ProvisionerTpm2.CertificateRequest.parseFrom(certificateRequestByteArray);
        } catch (InvalidProtocolBufferException ipbe) {
            final String errorMsg = "Could not deserialize Protobuf Certificate Request object.";
            log.error(errorMsg);
            throw new CertificateProcessingException(errorMsg, ipbe);
        }

        String certificateRequestJsonString = "";
        try {
            certificateRequestJsonString = JsonFormat.printer().print(certificateRequest);
        } catch (InvalidProtocolBufferException exception) {
            log.error("Certificate request could not be parsed properly into a JSON string");
        }

        // attempt to retrieve provisioner state based on nonce in request
        TPM2ProvisionerState tpm2ProvisionerState = getTpm2ProvisionerState(certificateRequest);

        if (tpm2ProvisionerState != null) {
            // Parse Identity Claim to gather necessary components
            ProvisionerTpm2.IdentityClaim identityClaim =
                    ProvisionUtils.parseIdentityClaim(tpm2ProvisionerState.getIdentityClaim());

            // Get endorsement public key
            ParsedTpmPublic ekPub;
            try {
                ekPub = TpmPublicHelper.parseTpmPublicArea(identityClaim.getEkPublicArea().toByteArray());
            } catch (Exception e) {
                log.error("Could not parse EK pub: {}", e.getMessage());
                throw new IllegalStateException(e.getMessage());
            }

            // Get attestation public key
            ParsedTpmPublic akPub;
            try {
                akPub = TpmPublicHelper.parseTpmPublicArea(identityClaim.getEkPublicArea().toByteArray());
            } catch (Exception e) {
                log.error("Could not parse AK pub: {}", e.getMessage());
                throw new IllegalStateException(e.getMessage());
            }

            // Get LDevID public key if it exists
            ParsedTpmPublic ldevidPub = null;
            if (identityClaim.hasLdevidPublicArea()) {
                try {
                    ldevidPub = TpmPublicHelper.parseTpmPublicArea(identityClaim.getLdevidPublicArea().toByteArray());
                } catch (Exception e) {
                    log.error("Could not parse LDevID pub: {}", e.getMessage());
                    throw new IllegalStateException(e.getMessage());
                }
            }

            // Get Endorsement Credential if it exists or was uploaded
            EndorsementCredential endorsementCredential =
                    credentialManagementService.parseEcFromIdentityClaim(identityClaim, ekPub.publicKey());

            // Get Platform Credentials if they exist or were uploaded
            List<PlatformCredential> platformCredentials =
                    credentialManagementService.parsePcsFromIdentityClaim(identityClaim,
                            endorsementCredential);

            // Get the device associated with the identity claim. Update the device if the cert request quote exists.
            Device device = retrieveDeviceWithUpdatedTPMInfo(certificateRequest, identityClaim);

            AppraisalStatus.Status validationResult = doQuoteValidation(device);

            if (validationResult == AppraisalStatus.Status.PASS) {
                // Create signed, attestation certificate
                X509Certificate attestationCertificate = generateCredential(akPub.publicKey(),
                        endorsementCredential, platformCredentials, device.getName(), acaCertificate);

                if (ldevidPub != null) {
                    // Create signed LDevID certificate
                    X509Certificate ldevidCertificate = generateCredential(ldevidPub.publicKey(),
                            endorsementCredential, platformCredentials, device.getName(), acaCertificate);
                    byte[] derEncodedAttestationCertificate = ProvisionUtils.getDerEncodedCertificate(
                            attestationCertificate);
                    byte[] derEncodedLdevidCertificate = ProvisionUtils.getDerEncodedCertificate(
                            ldevidCertificate);
                    String pemEncodedAttestationCertificate = ProvisionUtils.getPemEncodedCertificate(
                            attestationCertificate);
                    String pemEncodedLdevidCertificate = ProvisionUtils.getPemEncodedCertificate(
                            ldevidCertificate);

                    // We validated the nonce and made use of the identity claim so state can be deleted
                    tpm2ProvisionerStateService.deleteTPM2ProvisionerState(tpm2ProvisionerState);

                    boolean generateAtt =
                            credentialManagementService.saveAttestationCertificate(derEncodedAttestationCertificate,
                                    endorsementCredential, platformCredentials, device, false);

                    boolean generateLDevID =
                            credentialManagementService.saveAttestationCertificate(derEncodedLdevidCertificate,
                                    endorsementCredential, platformCredentials, device, true);

                    ProvisionerTpm2.CertificateResponse.Builder certificateResponseBuilder =
                            ProvisionerTpm2.CertificateResponse.newBuilder()
                                    .setStatus(ProvisionerTpm2.ResponseStatus.PASS);

                    if (generateAtt) {
                        certificateResponseBuilder =
                                certificateResponseBuilder.setCertificate(pemEncodedAttestationCertificate);
                    }

                    if (generateLDevID) {
                        certificateResponseBuilder =
                                certificateResponseBuilder.setLdevidCertificate(pemEncodedLdevidCertificate);
                    }
                    ProvisionerTpm2.CertificateResponse certificateResponse = certificateResponseBuilder.build();

                    String certResponseJsonStringAfterSuccess = "";
                    try {
                        certResponseJsonStringAfterSuccess = JsonFormat.printer().print(certificateRequest);
                    } catch (InvalidProtocolBufferException exception) {
                        log.error("Certificate response object after a successful validation, "
                                + "assuming the LDevId public key exists, could not be parsed "
                                + "properly into a JSON string");
                    }

                    if (!policySettings.isSaveProtobufToLogNeverEnabled()
                            && policySettings.isSaveProtobufToLogAlwaysEnabled()) {
                        log.info("------------- Start Of Protobuf Logging Of Certificate Request/Response "
                                + "After Successful Validation (LDevId public key does exist) "
                                + "-------------");

                        log.info("Certificate request object received after a successful validation "
                                        + " and if the LDevID public key exists {}",
                                certificateRequestJsonString.isEmpty()
                                        ? certificateRequest : certificateRequestJsonString);

                        log.info("Certificate Response "
                                + "object after a successful validation and if the LDevID "
                                + "public key exists : {}", certResponseJsonStringAfterSuccess.isEmpty()
                                ? certificateResponse : certResponseJsonStringAfterSuccess);

                        log.info("------------- End Of Protobuf Logging Of Certificate Request/Response "
                                + "After Successful Validation (LDevId public key does exist) "
                                + "-------------");
                    }

                    return certificateResponse.toByteArray();
                } else {
                    byte[] derEncodedAttestationCertificate = ProvisionUtils.getDerEncodedCertificate(
                            attestationCertificate);
                    String pemEncodedAttestationCertificate = ProvisionUtils.getPemEncodedCertificate(
                            attestationCertificate);

                    // We validated the nonce and made use of the identity claim so state can be deleted
                    tpm2ProvisionerStateService.deleteTPM2ProvisionerState(tpm2ProvisionerState);

                    ProvisionerTpm2.CertificateResponse.Builder certificateResponseBuilder =
                            ProvisionerTpm2.CertificateResponse.
                                    newBuilder().setStatus(ProvisionerTpm2.ResponseStatus.PASS);

                    boolean generateAtt = credentialManagementService.saveAttestationCertificate(
                            derEncodedAttestationCertificate,
                            endorsementCredential, platformCredentials, device, false);

                    if (generateAtt) {
                        certificateResponseBuilder =
                                certificateResponseBuilder.setCertificate(pemEncodedAttestationCertificate);
                    }
                    ProvisionerTpm2.CertificateResponse certificateResponse =
                            certificateResponseBuilder.build();

                    String certResponseJsonStringAfterSuccess = "";
                    try {
                        certResponseJsonStringAfterSuccess = JsonFormat.printer().print(certificateRequest);
                    } catch (InvalidProtocolBufferException exception) {
                        log.error("Certificate response object after a successful validation, "
                                + "assuming the LDevId public key does not exist, could not be parsed "
                                + "properly into a JSON string");
                    }

                    if (!policySettings.isSaveProtobufToLogNeverEnabled()
                            && policySettings.isSaveProtobufToLogAlwaysEnabled()) {

                        log.info("------------- Start Of Protobuf Logging Of Certificate Request/Response "
                                + "After Successful Validation (LDevId public key does not exist) "
                                + "-------------");

                        log.info("Certificate request object received after a successful validation "
                                        + " and if the LDevID public key does not exist {}",
                                certificateRequestJsonString.isEmpty()
                                        ? certificateRequest : certificateRequestJsonString);

                        log.info("Certificate Request Response "
                                        + "object after a successful validation and if the LDevID "
                                        + "public key does not exist : {}",
                                certResponseJsonStringAfterSuccess.isEmpty()
                                        ? certificateResponse : certResponseJsonStringAfterSuccess);

                        log.info("------------- End Of Protobuf Logging Of Certificate Request/Response "
                                + "After Successful Validation (LDevId public key does not exist) "
                                + "-------------");
                    }
                    return certificateResponse.toByteArray();
                }
            } else {
                log.error("Supply chain validation did not succeed. Firmware Quote Validation failed."
                        + " Result is: {}", validationResult);

                ProvisionerTpm2.CertificateResponse certificateResponse = ProvisionerTpm2.CertificateResponse
                        .newBuilder()
                        .setStatus(ProvisionerTpm2.ResponseStatus.FAIL)
                        .build();

                String certResponseJsonStringAfterFailure = "";
                try {
                    certResponseJsonStringAfterFailure = JsonFormat.printer().print(certificateRequest);
                } catch (InvalidProtocolBufferException exception) {
                    log.error("Certificate response object after a failed validation could not be parsed"
                            + " properly into a JSON string");
                }

                if (!policySettings.isSaveProtobufToLogNeverEnabled()
                        && (policySettings.isSaveProtobufToLogAlwaysEnabled()
                        || policySettings.isSaveProtobufToLogOnFailedValEnabled())) {
                    log.info("------------- Start Of Protobuf Log Of Certificate Request/Response"
                            + " After Failed Validation -------------");

                    log.info("Certificate request object received after a failed validation:"
                            + " {}", certificateRequest);
                    log.info("Certificate Request Response object after a failed validation: {}",
                            certResponseJsonStringAfterFailure.isEmpty()
                                    ? certificateResponse : certResponseJsonStringAfterFailure);

                    log.info("------------- End Of Protobuf Log Of Certificate Request/Response"
                            + " After Failed Validation -------------");
                }

                return certificateResponse.toByteArray();
            }
        } else {
            if (!policySettings.isSaveProtobufToLogNeverEnabled()
                    && (policySettings.isSaveProtobufToLogAlwaysEnabled()
                    || policySettings.isSaveProtobufToLogOnFailedValEnabled())) {
                log.info("------------- Start Of Protobuf Log Of Certificate Request After Failed "
                        + "Validation (Invalid Nonce) -------------");

                log.error("Could not process credential request. Invalid nonce provided: {}",
                        certificateRequestJsonString.isEmpty() ? certificateRequest : certificateRequestJsonString);

                log.info("------------- End Of Protobuf Log Of Certificate Request After Failed "
                        + "Validation (Invalid Nonce) -------------");
            }
            throw new CertificateProcessingException("Invalid nonce given in request by client.");
        }
    }

    /**
     * Helper method that retrieves a {@link Device} object from the Identity Claim and updates the {@link Device}
     * object TPM info if the certificate request's nonce is present.
     *
     * @param certificateRequest certificate request
     * @param identityClaim      identity claim
     * @return a {@link Device} object
     */
    private Device retrieveDeviceWithUpdatedTPMInfo(final ProvisionerTpm2.CertificateRequest certificateRequest,
                                                    final ProvisionerTpm2.IdentityClaim identityClaim) {
        final String deviceName = identityClaim.getDv().getNw().getHostname();
        Device device = deviceRepository.findByName(deviceName);

        // Parse through the Provisioner supplied TPM Quote and pcr values. These fields are optional.
        if (!certificateRequest.getQuote().isEmpty()) {
            DeviceInfoReport deviceInfoReport = Objects.requireNonNull(device.getDeviceInfo());

            TPMInfo savedInfo = deviceInfoReport.getTpmInfo();
            TPMInfo tpmInfo = new TPMInfo(savedInfo.getTpmMake(),
                    savedInfo.getTpmVersionMajor(),
                    savedInfo.getTpmVersionMinor(),
                    savedInfo.getTpmVersionRevMajor(),
                    savedInfo.getTpmVersionRevMinor(),
                    savedInfo.getPcrValues(),
                    ProvisionUtils.parseTPMQuoteHash(certificateRequest.getQuote().toStringUtf8())
                            .getBytes(StandardCharsets.UTF_8),
                    ProvisionUtils.parseTPMQuoteSignature(certificateRequest.getQuote().toStringUtf8())
                            .getBytes(StandardCharsets.UTF_8));

            DeviceInfoReport dvReport = new DeviceInfoReport(
                    deviceInfoReport.getNetworkInfo(),
                    deviceInfoReport.getOSInfo(),
                    deviceInfoReport.getFirmwareInfo(),
                    deviceInfoReport.getHardwareInfo(), tpmInfo,
                    identityClaim.getClientVersion());

            device.setDeviceInfo(dvReport);
            return deviceRepository.save(device);
        }

        return device;
    }

    /**
     * Helper method to unwrap the certificate request sent by the client and verify the
     * provided nonce.
     *
     * @param certificateRequest Client Certificate Request containing nonce to complete identity claim
     * @return the {@link TPM2ProvisionerState} if valid nonce provided / null, otherwise
     */
    private TPM2ProvisionerState getTpm2ProvisionerState(final ProvisionerTpm2.CertificateRequest certificateRequest) {
        if (certificateRequest.hasNonce()) {
            byte[] nonce = certificateRequest.getNonce().toByteArray();
            return tpm2ProvisionerStateService.getTPM2ProvisionerState(nonce);
        }
        return null;
    }

    /**
     * Performs supply chain validation for just the quote under Firmware validation.
     * Performed after main supply chain validation and a certificate request.
     *
     * @param device associated device to validate.
     * @return the {@link AppraisalStatus} of the supply chain validation
     */
    private AppraisalStatus.Status doQuoteValidation(final Device device) {
        log.info("Beginning Quote Validation...");
        // perform supply chain validation
        SupplyChainValidationSummary scvs = supplyChainValidationService.validateQuote(device);
        AppraisalStatus.Status validationResult;

        // either validation wasn't enabled or device already failed
        if (scvs == null) {
            // this will just allow for the certificate to be saved.
            validationResult = AppraisalStatus.Status.PASS;
        } else {
            device.setSummaryId(scvs.getId().toString());
            // update the validation result in the device
            validationResult = scvs.getOverallValidationResult();
            device.setSupplyChainValidationStatus(validationResult);
            deviceRepository.save(device);
        }

        return validationResult;
    }

    /**
     * Generates a credential using the specified public key.
     *
     * @param publicKey             non-null public key
     * @param endorsementCredential the endorsement credential
     * @param platformCredentials   the list of platform credentials
     * @param deviceName            The host name used in the subject alternative name
     * @param acaCertificate        object used to create credential
     * @return identity credential
     */
    private X509Certificate generateCredential(final PublicKey publicKey,
                                               final EndorsementCredential endorsementCredential,
                                               final List<PlatformCredential> platformCredentials,
                                               final String deviceName,
                                               final X509Certificate acaCertificate) {
        try {
            // have the certificate expire in the configured number of days
            Calendar expiry = Calendar.getInstance();
            expiry.add(Calendar.DAY_OF_YEAR, certificateValidityInDays);

            X500Name issuer = new X509CertificateHolder(acaCertificate.getEncoded()).getSubject();
            Date notBefore = new Date();
            Date notAfter = expiry.getTime();
            BigInteger serialNumber = BigInteger.valueOf(System.currentTimeMillis());

            SubjectPublicKeyInfo subjectPublicKeyInfo = SubjectPublicKeyInfo.getInstance(publicKey.getEncoded());

            // The subject should be left blank, per spec
            X509v3CertificateBuilder x509v3CertificateBuilder = new X509v3CertificateBuilder(issuer, serialNumber,
                    notBefore, notAfter, null /* subjectName */, subjectPublicKeyInfo);

            Extension subjectAlternativeName = IssuedCertificateAttributeHelper.buildSubjectAlternativeNameFromCerts(
                    endorsementCredential, platformCredentials, deviceName);

            Extension authKeyIdentifier = IssuedCertificateAttributeHelper.buildAuthorityKeyIdentifier(acaCertificate);

            x509v3CertificateBuilder.addExtension(subjectAlternativeName);

            if (authKeyIdentifier != null) {
                x509v3CertificateBuilder.addExtension(authKeyIdentifier);
            }

            // identify cert as an AIK with this extension
            if (IssuedCertificateAttributeHelper.EXTENDED_KEY_USAGE_EXTENSION != null) {
                x509v3CertificateBuilder.addExtension(IssuedCertificateAttributeHelper.EXTENDED_KEY_USAGE_EXTENSION);
            } else {
                log.warn("Failed to build extended key usage extension and add to AIK");
                throw new IllegalArgumentException(
                        "Extended Key Usage attribute unavailable. Unable to issue certificates");
            }

            // Add signing extension
            x509v3CertificateBuilder.addExtension(Extension.keyUsage, true,
                    new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment));

            // Basic constraints
            x509v3CertificateBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));

            ContentSigner signer =
                    new JcaContentSignerBuilder(getSignatureAlgorithm()).setProvider("BC").build(privateKey);

            X509CertificateHolder holder = x509v3CertificateBuilder.build(signer);

            return new JcaX509CertificateConverter().setProvider("BC").getCertificate(holder);
        } catch (IOException | IllegalArgumentException | OperatorCreationException | CertificateException exception) {
            throw new CertificateProcessingException("Encountered error while generating "
                    + "identity credential: " + exception.getMessage(), exception);
        }
    }

    /**
     * Returns a string representation of the signature algorithm that will be used with the provided private key.
     *
     * @return signature algorithm associated with the private key
     */
    private String getSignatureAlgorithm() {
        if (privateKey instanceof RSAPrivateKey) {
            return "SHA256WithRSA";
        } else if (privateKey instanceof ECPrivateKey) {
            return "SHA256withECDSA";
        }
        throw new IllegalArgumentException("Unsupported private key type: " + privateKey.getClass().getName());
    }
}
