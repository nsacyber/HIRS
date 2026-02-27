package hirs.attestationca.persist.provision;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import hirs.attestationca.configuration.provisionerTpm2.ProvisionerTpm2;
import hirs.attestationca.persist.entity.manager.CertificateRepository;
import hirs.attestationca.persist.entity.manager.DeviceRepository;
import hirs.attestationca.persist.entity.manager.PolicyRepository;
import hirs.attestationca.persist.entity.manager.TPM2ProvisionerStateRepository;
import hirs.attestationca.persist.entity.tpm.TPM2ProvisionerState;
import hirs.attestationca.persist.entity.userdefined.Device;
import hirs.attestationca.persist.entity.userdefined.PolicySettings;
import hirs.attestationca.persist.entity.userdefined.SupplyChainValidationSummary;
import hirs.attestationca.persist.entity.userdefined.certificate.EndorsementCredential;
import hirs.attestationca.persist.entity.userdefined.certificate.IssuedAttestationCertificate;
import hirs.attestationca.persist.entity.userdefined.certificate.PlatformCredential;
import hirs.attestationca.persist.entity.userdefined.info.TPMInfo;
import hirs.attestationca.persist.entity.userdefined.report.DeviceInfoReport;
import hirs.attestationca.persist.enums.AppraisalStatus;
import hirs.attestationca.persist.enums.PublicKeyAlgorithm;
import hirs.attestationca.persist.exceptions.CertificateProcessingException;
import hirs.attestationca.persist.provision.helper.CredentialManagementHelper;
import hirs.attestationca.persist.provision.helper.IssuedCertificateAttributeHelper;
import hirs.attestationca.persist.provision.helper.ProvisionUtils;
import hirs.attestationca.persist.service.SupplyChainValidationService;
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
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Service
@Log4j2
public class CertificateRequestProcessor {
    private final SupplyChainValidationService supplyChainValidationService;
    private final CertificateRepository certificateRepository;
    private final DeviceRepository deviceRepository;
    private final X509Certificate acaCertificate;
    private final TPM2ProvisionerStateRepository tpm2ProvisionerStateRepository;
    private final PolicyRepository policyRepository;
    private final PublicKeyAlgorithm publicKeyAlgorithm;
    private final int certificateValidityInDays;
    private final PrivateKey privateKey;

    /**
     * Constructor.
     *
     * @param supplyChainValidationService   object that is used to run provisioning
     * @param certificateRepository          db connector for all certificates.
     * @param deviceRepository               database connector for Devices.
     * @param privateKey                     private key used for communication authentication
     * @param acaCertificate                 object used to create credential
     * @param certificateValidityInDays      int for the time in which a certificate is valid.
     * @param tpm2ProvisionerStateRepository db connector for provisioner state.
     * @param policyRepository               db connector for policies.
     */
    @Autowired
    public CertificateRequestProcessor(final SupplyChainValidationService supplyChainValidationService,
                                       final CertificateRepository certificateRepository,
                                       final DeviceRepository deviceRepository,
                                       final PrivateKey privateKey,
                                       @Qualifier("leafACACert") final X509Certificate acaCertificate,
                                       @Value("${aca.current.public.key.algorithm}") final String publicKeyAlgorithmStr,
                                       @Value("${aca.certificates.validity}") final int certificateValidityInDays,
                                       final TPM2ProvisionerStateRepository tpm2ProvisionerStateRepository,
                                       final PolicyRepository policyRepository) {
        this.certificateValidityInDays = certificateValidityInDays;
        this.supplyChainValidationService = supplyChainValidationService;
        this.certificateRepository = certificateRepository;
        this.deviceRepository = deviceRepository;
        this.acaCertificate = acaCertificate;
        this.tpm2ProvisionerStateRepository = tpm2ProvisionerStateRepository;
        this.publicKeyAlgorithm = PublicKeyAlgorithm.fromString(publicKeyAlgorithmStr);
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
     * Basic implementation of the ACA processCertificateRequest method.
     * Parses the nonce, validates its correctness, generates the signed,
     * public attestation certificate, stores it, and returns it to the client.
     *
     * @param certificateRequest request containing nonce from earlier identity
     *                           claim handshake
     * @return a certificateResponse containing the signed certificate
     */
    public byte[] processCertificateRequest(final byte[] certificateRequest) {
        log.info("Certificate Request has been received and is ready to be processed");

        if (ArrayUtils.isEmpty(certificateRequest)) {
            final String errorMsg = "The CertificateRequest sent by the client cannot be null or empty.";
            log.error(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        final PolicySettings policySettings = policyRepository.findByName("Default");

        // attempt to deserialize Protobuf CertificateRequest
        ProvisionerTpm2.CertificateRequest request;
        try {
            request = ProvisionerTpm2.CertificateRequest.parseFrom(certificateRequest);
        } catch (InvalidProtocolBufferException ipbe) {
            final String errorMsg = "Could not deserialize Protobuf Certificate Request object.";
            log.error(errorMsg);
            throw new CertificateProcessingException(errorMsg, ipbe);
        }

        String certificateRequestJsonString = "";
        try {
            certificateRequestJsonString = JsonFormat.printer().print(request);
        } catch (InvalidProtocolBufferException exception) {
            log.error("Certificate request could not be parsed properly into a JSON string");
        }

        // attempt to retrieve provisioner state based on nonce in request
        TPM2ProvisionerState tpm2ProvisionerState = getTpm2ProvisionerState(request);

        if (tpm2ProvisionerState != null) {
            // Reparse Identity Claim to gather necessary components
            byte[] identityClaim = tpm2ProvisionerState.getIdentityClaim();
            ProvisionerTpm2.IdentityClaim claim = ProvisionUtils.parseIdentityClaim(identityClaim);

            // Get endorsement public key
            PublicKey ekPub = ProvisionUtils.parsePublicKeyFromPublicDataSegment(publicKeyAlgorithm,
                    claim.getEkPublicArea().toByteArray());

            // Get attestation public key
            PublicKey akPub = ProvisionUtils.parsePublicKeyFromPublicDataSegment(publicKeyAlgorithm,
                    claim.getAkPublicArea().toByteArray());

            // Get Endorsement Credential if it exists or was uploaded
            EndorsementCredential endorsementCredential =
                    CredentialManagementHelper.parseEcFromIdentityClaim(claim, ekPub, certificateRepository);

            // Get Platform Credentials if they exist or were uploaded
            List<PlatformCredential> platformCredentials = CredentialManagementHelper.parsePcsFromIdentityClaim(claim,
                    endorsementCredential, certificateRepository);

            // Get LDevID public key if it exists
            PublicKey ldevidPub = null;
            if (claim.hasLdevidPublicArea()) {
                ldevidPub = ProvisionUtils.parsePublicKeyFromPublicDataSegment(publicKeyAlgorithm,
                        claim.getLdevidPublicArea().toByteArray());
            }

            // Get device name and device
            String deviceName = claim.getDv().getNw().getHostname();
            Device device = deviceRepository.findByName(deviceName);

            // Parse through the Provisioner supplied TPM Quote and pcr values
            // these fields are optional
            if (!request.getQuote().isEmpty()) {
                TPMInfo savedInfo = device.getDeviceInfo().getTpmInfo();
                TPMInfo tpmInfo = new TPMInfo(savedInfo.getTpmMake(),
                        savedInfo.getTpmVersionMajor(),
                        savedInfo.getTpmVersionMinor(),
                        savedInfo.getTpmVersionRevMajor(),
                        savedInfo.getTpmVersionRevMinor(),
                        savedInfo.getPcrValues(),
                        ProvisionUtils.parseTPMQuoteHash(request.getQuote().toStringUtf8())
                                .getBytes(StandardCharsets.UTF_8),
                        ProvisionUtils.parseTPMQuoteSignature(request.getQuote().toStringUtf8())
                                .getBytes(StandardCharsets.UTF_8));

                DeviceInfoReport dvReport = new DeviceInfoReport(
                        device.getDeviceInfo().getNetworkInfo(),
                        device.getDeviceInfo().getOSInfo(),
                        device.getDeviceInfo().getFirmwareInfo(),
                        device.getDeviceInfo().getHardwareInfo(), tpmInfo,
                        claim.getClientVersion());

                device.setDeviceInfo(dvReport);
                device = this.deviceRepository.save(device);
            }

            AppraisalStatus.Status validationResult = doQuoteValidation(device);
            if (validationResult == AppraisalStatus.Status.PASS) {
                // Create signed, attestation certificate
                X509Certificate attestationCertificate = generateCredential(akPub,
                        endorsementCredential, platformCredentials, deviceName, acaCertificate);
                if (ldevidPub != null) {
                    // Create signed LDevID certificate
                    X509Certificate ldevidCertificate = generateCredential(ldevidPub,
                            endorsementCredential, platformCredentials, deviceName, acaCertificate);
                    byte[] derEncodedAttestationCertificate = ProvisionUtils.getDerEncodedCertificate(
                            attestationCertificate);
                    byte[] derEncodedLdevidCertificate = ProvisionUtils.getDerEncodedCertificate(
                            ldevidCertificate);
                    String pemEncodedAttestationCertificate = ProvisionUtils.getPemEncodedCertificate(
                            attestationCertificate);
                    String pemEncodedLdevidCertificate = ProvisionUtils.getPemEncodedCertificate(
                            ldevidCertificate);

                    // We validated the nonce and made use of the identity claim so state can be deleted
                    tpm2ProvisionerStateRepository.delete(tpm2ProvisionerState);
                    boolean generateAtt = saveAttestationCertificate(certificateRepository,
                            derEncodedAttestationCertificate,
                            endorsementCredential, platformCredentials, device, false);
                    boolean generateLDevID =
                            saveAttestationCertificate(certificateRepository, derEncodedLdevidCertificate,
                                    endorsementCredential, platformCredentials, device, true);

                    ProvisionerTpm2.CertificateResponse.Builder certificateResponseBuilder =
                            ProvisionerTpm2.CertificateResponse.
                                    newBuilder().setStatus(ProvisionerTpm2.ResponseStatus.PASS);
                    if (generateAtt) {
                        certificateResponseBuilder =
                                certificateResponseBuilder.setCertificate(pemEncodedAttestationCertificate);
                    }
                    if (generateLDevID) {
                        certificateResponseBuilder =
                                certificateResponseBuilder.setLdevidCertificate(pemEncodedLdevidCertificate);
                    }
                    ProvisionerTpm2.CertificateResponse certificateResponse =
                            certificateResponseBuilder.build();

                    String certResponseJsonStringAfterSuccess = "";
                    try {
                        certResponseJsonStringAfterSuccess = JsonFormat.printer().print(request);
                    } catch (InvalidProtocolBufferException exception) {
                        log.error("Certificate response object after a successful validation, "
                                + "assuming the LDevId public key exists, could not be parsed "
                                + "properly into a json string");
                    }

                    if (!policySettings.isSaveProtobufToLogNeverEnabled()
                            && policySettings.isSaveProtobufToLogAlwaysEnabled()) {
                        log.info("------------- Start Of Protobuf Logging Of Certificate Request/Response "
                                + "After Successful Validation (LDevId public key does exist) "
                                + "-------------");

                        log.info("Certificate request object received after a successful validation "
                                        + " and if the LDevID public key exists {}",
                                certificateRequestJsonString.isEmpty()
                                        ? request : certificateRequestJsonString);

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
                    tpm2ProvisionerStateRepository.delete(tpm2ProvisionerState);

                    ProvisionerTpm2.CertificateResponse.Builder certificateResponseBuilder =
                            ProvisionerTpm2.CertificateResponse.
                                    newBuilder().setStatus(ProvisionerTpm2.ResponseStatus.PASS);

                    boolean generateAtt = saveAttestationCertificate(certificateRepository,
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
                        certResponseJsonStringAfterSuccess = JsonFormat.printer().print(request);
                    } catch (InvalidProtocolBufferException exception) {
                        log.error("Certificate response object after a successful validation, "
                                + "assuming the LDevId public key does not exist, could not be parsed "
                                + "propertly into a json string");
                    }

                    if (!policySettings.isSaveProtobufToLogNeverEnabled()
                            && policySettings.isSaveProtobufToLogAlwaysEnabled()) {

                        log.info("------------- Start Of Protobuf Logging Of Certificate Request/Response "
                                + "After Successful Validation (LDevId public key does not exist) "
                                + "-------------");

                        log.info("Certificate request object received after a successful validation "
                                        + " and if the LDevID public key does not exist {}",
                                certificateRequestJsonString.isEmpty()
                                        ? request : certificateRequestJsonString);

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
                    certResponseJsonStringAfterFailure = JsonFormat.printer().print(request);
                } catch (InvalidProtocolBufferException exception) {
                    log.error("Certificate response object after a failed validation could not be parsed"
                            + " properly into a json string");
                }

                if (!policySettings.isSaveProtobufToLogNeverEnabled()
                        && (policySettings.isSaveProtobufToLogAlwaysEnabled()
                        || policySettings.isSaveProtobufToLogOnFailedValEnabled())) {
                    log.info("------------- Start Of Protobuf Log Of Certificate Request/Response"
                            + " After Failed Validation -------------");

                    log.info("Certificate request object received after a failed validation:"
                            + " {}", request);
                    log.info("Certificate Request Response "
                                    + "object after a failed validation: {}",
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

                log.error("Could not process credential request."
                                + " Invalid nonce provided: {}",
                        certificateRequestJsonString.isEmpty() ? request : certificateRequestJsonString);

                log.info("------------- End Of Protobuf Log Of Certificate Request After Failed "
                        + "Validation (Invalid Nonce) -------------");
            }
            throw new CertificateProcessingException("Invalid nonce given in request by client.");
        }
    }

    /**
     * Helper method to unwrap the certificate request sent by the client and verify the
     * provided nonce.
     *
     * @param request Client Certificate Request containing nonce to complete identity claim
     * @return the {@link TPM2ProvisionerState} if valid nonce provided / null, otherwise
     */
    private TPM2ProvisionerState getTpm2ProvisionerState(
            final ProvisionerTpm2.CertificateRequest request) {
        if (request.hasNonce()) {
            byte[] nonce = request.getNonce().toByteArray();
            return TPM2ProvisionerState.getTPM2ProvisionerState(tpm2ProvisionerStateRepository,
                    nonce);
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
        SupplyChainValidationSummary scvs = supplyChainValidationService.validateQuote(
                device);
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
     * @param publicKey             cannot be null
     * @param endorsementCredential the endorsement credential
     * @param platformCredentials   the set of platform credentials
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

            X500Name issuer =
                    new X509CertificateHolder(acaCertificate.getEncoded()).getSubject();
            Date notBefore = new Date();
            Date notAfter = expiry.getTime();
            BigInteger serialNumber = BigInteger.valueOf(System.currentTimeMillis());

            SubjectPublicKeyInfo subjectPublicKeyInfo =
                    SubjectPublicKeyInfo.getInstance(publicKey.getEncoded());

            // The subject should be left blank, per spec
            X509v3CertificateBuilder builder =
                    new X509v3CertificateBuilder(issuer, serialNumber,
                            notBefore, notAfter, null /* subjectName */, subjectPublicKeyInfo);

            Extension subjectAlternativeName =
                    IssuedCertificateAttributeHelper.buildSubjectAlternativeNameFromCerts(
                            endorsementCredential, platformCredentials, deviceName);

            Extension authKeyIdentifier = IssuedCertificateAttributeHelper
                    .buildAuthorityKeyIdentifier(acaCertificate);

            builder.addExtension(subjectAlternativeName);
            if (authKeyIdentifier != null) {
                builder.addExtension(authKeyIdentifier);
            }
            // identify cert as an AIK with this extension
            if (IssuedCertificateAttributeHelper.EXTENDED_KEY_USAGE_EXTENSION != null) {
                builder.addExtension(IssuedCertificateAttributeHelper.EXTENDED_KEY_USAGE_EXTENSION);
            } else {
                log.warn("Failed to build extended key usage extension and add to AIK");
                throw new IllegalStateException("Extended Key Usage attribute unavailable. "
                        + "Unable to issue certificates");
            }

            // Add signing extension
            builder.addExtension(
                    Extension.keyUsage,
                    true,
                    new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment)
            );

            // Basic constraints
            builder.addExtension(
                    Extension.basicConstraints,
                    true,
                    new BasicConstraints(false)
            );

            ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSA")
                    .setProvider("BC").build(privateKey);
            X509CertificateHolder holder = builder.build(signer);
            return new JcaX509CertificateConverter()
                    .setProvider("BC").getCertificate(holder);
        } catch (IOException | OperatorCreationException | CertificateException exception) {
            throw new CertificateProcessingException("Encountered error while generating "
                    + "identity credential: " + exception.getMessage(), exception);
        }
    }

    /**
     * Helper method to create an {@link IssuedAttestationCertificate} object, set its
     * corresponding device and persist it.
     *
     * @param certificateRepository            db store manager for certificates
     * @param derEncodedAttestationCertificate the byte array representing the Attestation
     *                                         certificate
     * @param endorsementCredential            the endorsement credential used to generate the AC
     * @param platformCredentials              the platform credentials used to generate the AC
     * @param device                           the device to which the attestation certificate is tied
     * @param ldevID                           whether the certificate is a ldevid
     * @return whether the certificate was saved successfully
     */
    public boolean saveAttestationCertificate(final CertificateRepository certificateRepository,
                                              final byte[] derEncodedAttestationCertificate,
                                              final EndorsementCredential endorsementCredential,
                                              final List<PlatformCredential> platformCredentials,
                                              final Device device,
                                              final boolean ldevID) {
        List<IssuedAttestationCertificate> issuedAc;
        boolean generateCertificate = true;
        PolicySettings policySettings;
        Date currentDate = new Date();
        int days;
        try {
            // save issued certificate
            IssuedAttestationCertificate attCert = new IssuedAttestationCertificate(
                    derEncodedAttestationCertificate, endorsementCredential, platformCredentials, ldevID);

            policySettings = policyRepository.findByName("Default");

            Sort sortCriteria = Sort.by(Sort.Direction.DESC, "endValidity");
            issuedAc = certificateRepository.findByDeviceIdAndLdevID(device.getId(), ldevID,
                    sortCriteria);

            generateCertificate = ldevID ? policySettings.isIssueDevIdCertificateEnabled()
                    : policySettings.isIssueAttestationCertificateEnabled();

            if (issuedAc != null && !issuedAc.isEmpty()
                    && (ldevID ? policySettings.isGenerateDevIdCertificateOnExpiration()
                    : policySettings.isGenerateAttestationCertificateOnExpiration())) {
                if (issuedAc.getFirst().getEndValidity().after(currentDate)) {
                    // so the issued AC is not expired
                    // however are we within the threshold
                    days = ProvisionUtils.daysBetween(currentDate, issuedAc.getFirst().getEndValidity());
                    generateCertificate =
                            days < (ldevID ? policySettings.getDevIdReissueThreshold()
                                    : policySettings.getReissueThreshold());
                }
            }

            if (generateCertificate) {
                attCert.setDeviceId(device.getId());
                attCert.setDeviceName(device.getName());
                certificateRepository.save(attCert);
            }
        } catch (Exception e) {
            log.error("Error saving generated Attestation Certificate to database.", e);
            throw new CertificateProcessingException(
                    "Encountered error while storing Attestation Certificate: "
                            + e.getMessage(), e);
        }

        return generateCertificate;
    }
}
