package hirs.attestationca.persist.provision.service;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import hirs.attestationca.configuration.provisionerTpm2.ProvisionerTpm2;
import hirs.attestationca.persist.entity.manager.PolicyRepository;
import hirs.attestationca.persist.entity.tpm.TPM2ProvisionerState;
import hirs.attestationca.persist.entity.userdefined.Device;
import hirs.attestationca.persist.entity.userdefined.PolicySettings;
import hirs.attestationca.persist.entity.userdefined.SupplyChainValidationSummary;
import hirs.attestationca.persist.entity.userdefined.certificate.EndorsementCredential;
import hirs.attestationca.persist.entity.userdefined.certificate.PlatformCredential;
import hirs.attestationca.persist.entity.userdefined.info.ComponentInfo;
import hirs.attestationca.persist.entity.userdefined.report.DeviceInfoReport;
import hirs.attestationca.persist.enums.AppraisalStatus;
import hirs.attestationca.persist.provision.helper.ProvisionUtils;
import hirs.utils.HexUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.PublicKey;
import java.util.List;
import java.util.Objects;

/**
 * Service class responsible for processing the Provisioner's Identity Claim.
 */
@Service
@Log4j2
public class IdentityClaimProcessorService {
    /**
     * Number of bytes to include in the TPM2.0 nonce.
     */
    public static final int NONCE_LENGTH = 20;
    private static final String PCR_QUOTE_MASK = "0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23";

    private final SupplyChainValidationService supplyChainValidationService;
    private final CredentialManagementService credentialManagementService;
    private final DeviceInfoProcessorService deviceInfoProcessorService;
    private final Tpm2ProvisionerStateService tpm2ProvisionerStateService;
    private final PolicyRepository policyRepository;

    /**
     * Constructor.
     *
     * @param supplyChainValidationService supply chain validation service
     * @param credentialManagementService  certificate processor service
     * @param deviceInfoProcessorService   device info processor service
     * @param tpm2ProvisionerStateService  tpm2 provisioner state service
     * @param policyRepository             policy repository
     */
    @Autowired
    public IdentityClaimProcessorService(
            final SupplyChainValidationService supplyChainValidationService,
            final CredentialManagementService credentialManagementService,
            final DeviceInfoProcessorService deviceInfoProcessorService,
            final Tpm2ProvisionerStateService tpm2ProvisionerStateService,
            final PolicyRepository policyRepository) {
        this.supplyChainValidationService = supplyChainValidationService;
        this.credentialManagementService = credentialManagementService;
        this.deviceInfoProcessorService = deviceInfoProcessorService;
        this.tpm2ProvisionerStateService = tpm2ProvisionerStateService;
        this.policyRepository = policyRepository;
    }

    /**
     * Basic implementation of the ACA processIdentityClaimTpm2 method. Parses the identity claim,
     * stores the device info, performs supply chain validation, generates a nonce,
     * and wraps that nonce with the make credential process before returning it to the client.
     * attCert.setPcrValues(pcrValues);
     *
     * @param identityClaimByteArray the request to process, cannot be null
     * @return an identity claim response for the specified request containing a wrapped blob
     */
    public byte[] processIdentityClaimTpm2(final byte[] identityClaimByteArray) {
        log.info("Identity Claim has been received and is ready to be processed");

        if (ArrayUtils.isEmpty(identityClaimByteArray)) {
            final String errorMsg = "The IdentityClaim sent by the client cannot be null or empty.";
            log.error(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        final PolicySettings policySettings = policyRepository.findByName("Default");

        // attempt to deserialize Protobuf Identity Claim
        ProvisionerTpm2.IdentityClaim identityClaim = ProvisionUtils.parseIdentityClaim(identityClaimByteArray);

        String identityClaimJsonString = "";
        try {
            identityClaimJsonString = JsonFormat.printer().print(identityClaim);
        } catch (InvalidProtocolBufferException exception) {
            log.error("Identity claim could not be parsed properly into a json string");
        }

        // parse the EK Public key from the IdentityClaim
        PublicKey endorsementCredentialPublicKey =
                ProvisionUtils.parsePublicKeyFromPublicDataSegment(identityClaim.getEkPublicArea().toByteArray());

        AppraisalStatus.Status validationResult = AppraisalStatus.Status.FAIL;

        try {
            validationResult = doSupplyChainValidation(identityClaim, endorsementCredentialPublicKey);
        } catch (Exception ex) {
            log.error(ex.getMessage());
        }

        ByteString blobStr = ByteString.copyFrom(new byte[]{});

        if (validationResult == AppraisalStatus.Status.PASS) {
            PublicKey akPub =
                    ProvisionUtils.parsePublicKeyFromPublicDataSegment(identityClaim.getAkPublicArea().toByteArray());
            byte[] nonce = ProvisionUtils.generateRandomBytes(NONCE_LENGTH);
            blobStr = ProvisionUtils.tpm20MakeCredential(endorsementCredentialPublicKey, akPub,
                    nonce);

            String pcrQuoteMask = PCR_QUOTE_MASK;

            String strNonce = HexUtils.byteArrayToHexString(nonce);
            log.info("Sending nonce: {}", strNonce);
            log.info("Persisting identity claim of length: {}", identityClaimByteArray.length);

            tpm2ProvisionerStateService.saveTPM2ProvisionerState(
                    new TPM2ProvisionerState(nonce, identityClaimByteArray));

            if (policySettings.isIgnoreImaEnabled()) {
                pcrQuoteMask = PCR_QUOTE_MASK.replace("10,", "");
            }

            // Package response
            ProvisionerTpm2.IdentityClaimResponse identityClaimResponse
                    = ProvisionerTpm2.IdentityClaimResponse.newBuilder()
                    .setCredentialBlob(blobStr).setPcrMask(pcrQuoteMask)
                    .setStatus(ProvisionerTpm2.ResponseStatus.PASS)
                    .build();

            String identityClaimResponseJsonStringAfterSuccess = "";
            try {
                identityClaimResponseJsonStringAfterSuccess =
                        JsonFormat.printer().print(identityClaimResponse);
            } catch (InvalidProtocolBufferException exception) {
                log.error("Identity claim response after a successful validation "
                        + "could not be parsed properly into a json string");
            }

            if (!policySettings.isSaveProtobufToLogNeverEnabled()
                    && policySettings.isSaveProtobufToLogAlwaysEnabled()) {

                log.info("----------------- Start Of Protobuf Logging Of Identity Claim/Response "
                        + " After Successful Validation -----------------");

                log.info("Identity Claim object received after a "
                        + "successful validation: {}", identityClaimJsonString.isEmpty()
                        ? identityClaim : identityClaimJsonString);

                log.info("Identity Claim Response object after a "
                        + "successful validation: {}", identityClaimResponseJsonStringAfterSuccess.isEmpty()
                        ? identityClaimResponse : identityClaimResponseJsonStringAfterSuccess);

                log.info("----------------- End Of Protobuf Logging Of Identity Claim/Response "
                        + " After Successful Validation -----------------");
            }

            return identityClaimResponse.toByteArray();
        } else {
            log.error("Supply chain validation did not succeed. Result is: {}", validationResult);
            // empty response
            ProvisionerTpm2.IdentityClaimResponse identityClaimResponse
                    = ProvisionerTpm2.IdentityClaimResponse.newBuilder()
                    .setCredentialBlob(blobStr)
                    .setStatus(ProvisionerTpm2.ResponseStatus.FAIL)
                    .build();

            String identityClaimResponseJsonStringAfterFailure = "";
            try {
                identityClaimResponseJsonStringAfterFailure =
                        JsonFormat.printer().print(identityClaimResponse);
            } catch (InvalidProtocolBufferException exception) {
                log.error("Identity claim response after a failed validation "
                        + "could not be parsed properly into a json string");
            }

            if (!policySettings.isSaveProtobufToLogNeverEnabled()
                    && (policySettings.isSaveProtobufToLogAlwaysEnabled()
                    || policySettings.isSaveProtobufToLogOnFailedValEnabled())) {
                log.info("----------------- Start Of Protobuf Logging Of Identity Claim/Response "
                        + " After Failed Validation -----------------");

                log.info("Identity Claim object received after a "
                        + "failed validation: {}", identityClaimJsonString.isEmpty()
                        ? identityClaim : identityClaimJsonString);

                log.info("Identity Claim Response object after a "
                        + "failed validation: {}", identityClaimResponseJsonStringAfterFailure.isEmpty()
                        ? identityClaimResponse : identityClaimResponseJsonStringAfterFailure);

                log.info("----------------- End Of Protobuf Logging Of Identity Claim/Response "
                        + " After Failed Validation -----------------");
            }

            return identityClaimResponse.toByteArray();
        }
    }

    /**
     * Performs supply chain validation.
     *
     * @param identityClaim the identity claim
     * @param ekPublicKey   the public endorsement key
     * @return the {@link AppraisalStatus} of the supply chain validation
     */
    private AppraisalStatus.Status doSupplyChainValidation(final ProvisionerTpm2.IdentityClaim identityClaim,
                                                           final PublicKey ekPublicKey) throws IOException {

        // Find an endorsement credential to validate
        EndorsementCredential endorsementCredential =
                credentialManagementService.parseEcFromIdentityClaim(identityClaim, ekPublicKey);

        // Find platform credentials to validate
        List<PlatformCredential> platformCredentials =
                credentialManagementService.parsePcsFromIdentityClaim(identityClaim, endorsementCredential);

        // Parse and store the device info
        Device device = deviceInfoProcessorService.processDeviceInfo(identityClaim);

        // Parse and store the device components
        DeviceInfoReport deviceInfo = Objects.requireNonNull(device.getDeviceInfo());
        List<ComponentInfo> componentInfoList = deviceInfoProcessorService.processDeviceComponents(
                deviceInfo.getNetworkInfo().getHostname(),
                identityClaim.getPaccorOutput());

        // Store the platform certificates' components
        credentialManagementService.saveOrUpdatePlatformCertificateComponents(platformCredentials);

        // Perform supply chain validation
        SupplyChainValidationSummary summary = supplyChainValidationService.validateSupplyChain(
                endorsementCredential, platformCredentials, device, componentInfoList);
        device.setSummaryId(summary.getId().toString());

        // Update the validation result in the device and update the updated device in the database
        AppraisalStatus.Status validationResult = summary.getOverallValidationResult();
        device.setSupplyChainValidationStatus(validationResult);
        deviceInfoProcessorService.saveOrUpdateDevice(device);

        return validationResult;
    }
}
