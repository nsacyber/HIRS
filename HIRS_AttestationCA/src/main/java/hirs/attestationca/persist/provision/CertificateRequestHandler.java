package hirs.attestationca.persist.provision;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import hirs.attestationca.configuration.provisionerTpm2.ProvisionerTpm2;
import hirs.attestationca.persist.entity.manager.CertificateRepository;
import hirs.attestationca.persist.entity.manager.DeviceRepository;
import hirs.attestationca.persist.entity.manager.TPM2ProvisionerStateRepository;
import hirs.attestationca.persist.entity.tpm.TPM2ProvisionerState;
import hirs.attestationca.persist.entity.userdefined.Device;
import hirs.attestationca.persist.entity.userdefined.SupplyChainValidationSummary;
import hirs.attestationca.persist.entity.userdefined.certificate.EndorsementCredential;
import hirs.attestationca.persist.entity.userdefined.certificate.PlatformCredential;
import hirs.attestationca.persist.entity.userdefined.info.TPMInfo;
import hirs.attestationca.persist.entity.userdefined.report.DeviceInfoReport;
import hirs.attestationca.persist.enums.AppraisalStatus;
import hirs.attestationca.persist.exceptions.CertificateProcessingException;
import hirs.attestationca.persist.provision.helper.ProvisionUtils;
import hirs.attestationca.persist.service.SupplyChainValidationService;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.ArrayUtils;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.List;

@Log4j2
public class CertificateRequestHandler extends AbstractRequestHandler {

    private SupplyChainValidationService supplyChainValidationService;
    private CertificateRepository certificateRepository;
    private DeviceRepository deviceRepository;
    private X509Certificate acaCertificate;
    private TPM2ProvisionerStateRepository tpm2ProvisionerStateRepository;

    /**
     * Constructor.
     * @param certificateRepository db connector for all certificates.
     * @param deviceRepository database connector for Devices.
     * @param validDays int for the time in which a certificate is valid.
     * @param tpm2ProvisionerStateRepository db connector for provisioner state.
     */
    public CertificateRequestHandler(final SupplyChainValidationService supplyChainValidationService,
                                     final CertificateRepository certificateRepository,
                                     final DeviceRepository deviceRepository,
                                     final PrivateKey privateKey,
                                     final X509Certificate acaCertificate,
                                     final int validDays,
                                     final TPM2ProvisionerStateRepository tpm2ProvisionerStateRepository) {
        super(privateKey, validDays);
        this.supplyChainValidationService = supplyChainValidationService;
        this.certificateRepository = certificateRepository;
        this.deviceRepository = deviceRepository;
        this.acaCertificate = acaCertificate;
        this.tpm2ProvisionerStateRepository = tpm2ProvisionerStateRepository;
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
        log.info("Certificate Request received...");

        if (ArrayUtils.isEmpty(certificateRequest)) {
            throw new IllegalArgumentException("The CertificateRequest sent by the client"
                    + " cannot be null or empty.");
        }

        // attempt to deserialize Protobuf CertificateRequest
        ProvisionerTpm2.CertificateRequest request;
        try {
            request = ProvisionerTpm2.CertificateRequest.parseFrom(certificateRequest);
        } catch (InvalidProtocolBufferException ipbe) {
            throw new CertificateProcessingException(
                    "Could not deserialize Protobuf Certificate Request object.", ipbe);
        }

        // attempt to retrieve provisioner state based on nonce in request
        TPM2ProvisionerState tpm2ProvisionerState = getTpm2ProvisionerState(request);
        if (tpm2ProvisionerState != null) {
            // Reparse Identity Claim to gather necessary components
            byte[] identityClaim = tpm2ProvisionerState.getIdentityClaim();
            ProvisionerTpm2.IdentityClaim claim = ProvisionUtils.parseIdentityClaim(identityClaim);

            // Get endorsement public key
            RSAPublicKey ekPub = ProvisionUtils.parsePublicKey(claim.getEkPublicArea().toByteArray());

            // Get attestation public key
            RSAPublicKey akPub = ProvisionUtils.parsePublicKey(claim.getAkPublicArea().toByteArray());

            // Get Endorsement Credential if it exists or was uploaded
            EndorsementCredential endorsementCredential = parseEcFromIdentityClaim(claim, ekPub, certificateRepository);

            // Get Platform Credentials if they exist or were uploaded
            List<PlatformCredential> platformCredentials = parsePcsFromIdentityClaim(claim,
                    endorsementCredential, certificateRepository);

            // Get device name and device
            String deviceName = claim.getDv().getNw().getHostname();
            Device device = deviceRepository.findByName(deviceName);

            // Parse through the Provisioner supplied TPM Quote and pcr values
            // these fields are optional
            if (request.getQuote() != null && !request.getQuote().isEmpty()) {
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
                byte[] derEncodedAttestationCertificate = ProvisionUtils.getDerEncodedCertificate(
                        attestationCertificate);

                // We validated the nonce and made use of the identity claim so state can be deleted
                tpm2ProvisionerStateRepository.delete(tpm2ProvisionerState);

                // Package the signed certificate into a response
                ByteString certificateBytes = ByteString
                        .copyFrom(derEncodedAttestationCertificate);
                ProvisionerTpm2.CertificateResponse response = ProvisionerTpm2.CertificateResponse
                        .newBuilder().setCertificate(certificateBytes)
                        .setStatus(ProvisionerTpm2.ResponseStatus.PASS)
                        .build();

                saveAttestationCertificate(certificateRepository, derEncodedAttestationCertificate,
                        endorsementCredential, platformCredentials, device);

                return response.toByteArray();
            } else {
                log.error("Supply chain validation did not succeed. "
                        + "Firmware Quote Validation failed. Result is: "
                        + validationResult);
                ProvisionerTpm2.CertificateResponse response = ProvisionerTpm2.CertificateResponse
                        .newBuilder()
                        .setStatus(ProvisionerTpm2.ResponseStatus.FAIL)
                        .build();
                return response.toByteArray();
            }
        } else {
            log.error("Could not process credential request. Invalid nonce provided: "
                    + request.getNonce().toString());
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
}
