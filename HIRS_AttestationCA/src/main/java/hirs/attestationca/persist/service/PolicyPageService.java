package hirs.attestationca.persist.service;

import hirs.attestationca.persist.entity.manager.PolicyRepository;
import hirs.attestationca.persist.entity.userdefined.PolicySettings;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * A service layer class responsible for encapsulating all business logic related to the ACA Policy Page.
 */
@Log4j2
@Service
public class PolicyPageService {

    private final PolicyRepository policyRepository;

    /**
     * Constructor for the Policy Page Service.
     *
     * @param policyRepository policy repository
     */
    @Autowired
    public PolicyPageService(final PolicyRepository policyRepository) {
        this.policyRepository = policyRepository;

        if (this.policyRepository.findByName("Default") == null) {
            this.policyRepository.saveAndFlush(new PolicySettings("Default",
                    "Settings are configured for no validation flags set."));
        }
    }

    /**
     * Updates the Platform Certificate Validation policy according to user input.
     *
     * @param isPcValidationOptionEnabled boolean value representation of the current policy option's state
     * @return true if the policy was updated successfully; otherwise, false.
     */
    public boolean updatePCValidationPolicy(final boolean isPcValidationOptionEnabled) {
        PolicySettings policySettings = getDefaultPolicy();

        if (!isPolicyValid(policySettings.isEcValidationEnabled(), isPcValidationOptionEnabled,
                policySettings.isPcAttributeValidationEnabled())) {
            log.error("Current ACA Policy after updating the ACA Platform Validation setting due to the"
                    + " current policy configuration.");
            return false;
        }

        policySettings.setPcValidationEnabled(isPcValidationOptionEnabled);

        if (!isPcValidationOptionEnabled) {
            policySettings.setPcAttributeValidationEnabled(false);
        }

        policyRepository.saveAndFlush(policySettings);

        log.debug("Current ACA Policy after updating the platform credential validation "
                + "policy: {}", policySettings);

        return true;
    }

    /**
     * Updates the Platform Certificate Attribute Validation policy according to user input.
     *
     * @param isPcAttributeValidationOptionEnabled boolean value representation of the current policy
     *                                             option's state
     * @return true if the policy was updated successfully; otherwise, false.
     */
    public boolean updatePCAttributeValidationPolicy(final boolean isPcAttributeValidationOptionEnabled) {
        PolicySettings policySettings = getDefaultPolicy();

        // If PC Attribute Validation is enabled without PC Validation, disallow change
        if (!isPolicyValid(policySettings.isEcValidationEnabled(),
                policySettings.isPcValidationEnabled(), isPcAttributeValidationOptionEnabled)) {
            log.error("To enable Platform Attribute Validation, Platform Credential Validation"
                    + " must also be enabled.");
            return false;
        }

        policySettings.setPcAttributeValidationEnabled(isPcAttributeValidationOptionEnabled);

        policyRepository.saveAndFlush(policySettings);

        log.debug("Current ACA Policy after updating the platform credential "
                + "attribute credential validation policy: {}", policySettings);

        return true;
    }

    /**
     * Updates the Ignore Revision Attribute policy according to user input.
     *
     * @param isIgnoreRevisionAttributeOptionEnabled boolean value representation of the current policy
     *                                               option's state
     * @return true if the policy was updated successfully; otherwise, false.
     */
    public boolean updateIgnoreRevisionAttributePolicy(final boolean isIgnoreRevisionAttributeOptionEnabled) {
        PolicySettings policySettings = getDefaultPolicy();

        if (isIgnoreRevisionAttributeOptionEnabled && !policySettings.isPcAttributeValidationEnabled()) {
            log.error("Ignore Component Revision Attribute cannot be "
                    + "enabled without PC Attribute validation policy enabled.");
            return false;
        }

        policySettings.setIgnoreRevisionEnabled(isIgnoreRevisionAttributeOptionEnabled);

        policyRepository.saveAndFlush(policySettings);

        log.debug("Current ACA Policy after updating the ignore component revision attribute"
                + " option policy: {}", policySettings);

        return true;
    }

    /**
     * Updates the Issued Attestation Certificate generation policy according to user input.
     *
     * @param isIssuedAttestationOptionEnabled boolean value representation of the current policy option's
     *                                         state
     */
    public void updateIssuedAttestationGenerationPolicy(final boolean isIssuedAttestationOptionEnabled) {
        PolicySettings policySettings = getDefaultPolicy();

        if (!isIssuedAttestationOptionEnabled) {
            policySettings.setGenerateOnExpiration(false);
        }

        policySettings.setIssueAttestationCertificate(isIssuedAttestationOptionEnabled);

        policyRepository.saveAndFlush(policySettings);

        log.debug("Current ACA Policy after updating the issued attestation certificate generation "
                + " policy: {}", policySettings);
    }

    /**
     * Updates the DevId validation policy according to user input.
     *
     * @param isLDevIdOptionEnabled boolean value representation of the current policy option's state
     */
    public void updateLDevIdGenerationPolicy(final boolean isLDevIdOptionEnabled) {
        PolicySettings policySettings = getDefaultPolicy();

        if (!isLDevIdOptionEnabled) {
            policySettings.setDevIdExpirationFlag(false);
        }

        policySettings.setIssueDevIdCertificate(isLDevIdOptionEnabled);

        policyRepository.saveAndFlush(policySettings);

        log.debug("Current ACA Policy after updating the ldevid validation "
                + " policy: {}", policySettings);
    }

    /**
     * Updates the Endorsement Credential validation policy according to user input.
     *
     * @param isEcValidationOptionEnabled boolean value representation of the current policy option's state
     * @return true if the policy was updated successfully; otherwise, false.
     */
    public boolean updateECValidationPolicy(final boolean isEcValidationOptionEnabled) {
        PolicySettings policySettings = getDefaultPolicy();

        if (!isPolicyValid(isEcValidationOptionEnabled, policySettings.isPcValidationEnabled(),
                policySettings.isPcAttributeValidationEnabled())) {
            log.error("To disable Endorsement Credential Validation, Platform Validation"
                    + " must also be disabled.");
            return false;
        }

        policySettings.setEcValidationEnabled(isEcValidationOptionEnabled);

        policyRepository.saveAndFlush(policySettings);

        log.debug("Current ACA Policy after updating the endorsement credential "
                + "validation policy: {}", policySettings);

        return true;
    }

    /**
     * Updates the firmware validation policy according to user input.
     *
     * @param isFirmwareValidationOptionEnabled boolean value representation of the current policy
     *                                          option's state
     * @return true if the policy was updated successfully; otherwise, false.
     */
    public boolean updateFirmwareValidationPolicy(final boolean isFirmwareValidationOptionEnabled) {
        PolicySettings policySettings = getDefaultPolicy();

        //If firmware is enabled without PC attributes, disallow change
        if (isFirmwareValidationOptionEnabled && !policySettings.isPcAttributeValidationEnabled()) {
            log.error("Firmware validation cannot be enabled without PC Attributes policy enabled.");
            return false;
        }

        policySettings.setFirmwareValidationEnabled(isFirmwareValidationOptionEnabled);
        policySettings.setIgnoreGptEnabled(isFirmwareValidationOptionEnabled);

        if (!isFirmwareValidationOptionEnabled) {
            policySettings.setIgnoreImaEnabled(false);
            policySettings.setIgnoretBootEnabled(false);
            policySettings.setIgnoreOsEvtEnabled(false);
        }

        policyRepository.saveAndFlush(policySettings);

        log.debug("Current ACA Policy after updating the firmware validation "
                + " policy: {}", policySettings);

        return true;
    }

    /**
     * Updates the ignore IMA policy according to user input.
     *
     * @param isIgnoreImaOptionEnabled boolean value representation of the current policy option's state
     * @return true if the policy was updated successfully; otherwise, false.
     */
    public boolean updateIgnoreImaPolicy(final boolean isIgnoreImaOptionEnabled) {
        PolicySettings policySettings = getDefaultPolicy();

        //If Ignore IMA is enabled without firmware, disallow change
        if (isIgnoreImaOptionEnabled && !policySettings.isFirmwareValidationEnabled()) {
            log.error("Ignore IMA cannot be enabled without Firmware Validation policy enabled.");
            return false;
        }

        policySettings.setIgnoreImaEnabled(isIgnoreImaOptionEnabled);

        policyRepository.saveAndFlush(policySettings);

        log.debug("Current ACA Policy after updating the ignore IMA policy:"
                + " {}", policySettings);

        return true;
    }

    /**
     * Updates the ignore TBoot policy according to user input.
     *
     * @param isIgnoreTbootOptionEnabled boolean value representation of the current policy option's state
     * @return true if the policy was updated successfully; otherwise, false.
     */
    public boolean updateIgnoreTBootPolicy(final boolean isIgnoreTbootOptionEnabled) {
        PolicySettings policySettings = getDefaultPolicy();

        //If Ignore TBoot is enabled without firmware, disallow change
        if (isIgnoreTbootOptionEnabled && !policySettings.isFirmwareValidationEnabled()) {
            log.error("Ignore TBoot cannot be enabled without Firmware Validation policy enabled.");
            return false;
        }

        policySettings.setIgnoretBootEnabled(isIgnoreTbootOptionEnabled);

        policyRepository.saveAndFlush(policySettings);

        log.debug("Current ACA Policy after updating the ignore TBoot policy:"
                + " {}", policySettings);

        return true;
    }

    /**
     * Updates the ignore GPT events policy according to user input.
     *
     * @param isIgnoreGptOptionEnabled boolean value representation of the current policy option's state
     * @return true if the policy was updated successfully; otherwise, false.
     */
    public boolean updateIgnoreGptEventsPolicy(final boolean isIgnoreGptOptionEnabled) {
        PolicySettings policySettings = getDefaultPolicy();

        //If Ignore TBoot is enabled without firmware, disallow change
        if (isIgnoreGptOptionEnabled && !policySettings.isFirmwareValidationEnabled()) {
            log.error("Ignore GPT Events cannot be enabled without Firmware Validation policy enabled.");
            return false;
        }

        policySettings.setIgnoreGptEnabled(isIgnoreGptOptionEnabled);

        policyRepository.saveAndFlush(policySettings);

        log.debug("Current ACA Policy after updating the ignore GPT events policy:"
                + " {}", policySettings);

        return true;
    }

    /**
     * Updates the ignore OS events policy according to user input.
     *
     * @param isIgnoreOSEvtOptionEnabled boolean value representation of the current policy option's state
     * @return true if the policy was updated successfully; otherwise, false.
     */
    public boolean updateIgnoreOSEventsPolicy(final boolean isIgnoreOSEvtOptionEnabled) {
        PolicySettings policySettings = getDefaultPolicy();

        //If Ignore OS events is enabled without firmware, disallow change
        if (isIgnoreOSEvtOptionEnabled && !policySettings.isFirmwareValidationEnabled()) {
            log.error("Ignore OS Events cannot be enabled without Firmware Validation policy enabled.");
            return false;
        }

        if (isIgnoreOSEvtOptionEnabled) {
            policySettings.setIgnoreGptEnabled(true);
        }

        policySettings.setIgnoreOsEvtEnabled(isIgnoreOSEvtOptionEnabled);

        policyRepository.saveAndFlush(policySettings);

        log.debug("Current ACA Policy after updating the ignore OS events policy:"
                + " {}", policySettings);

        return true;
    }

    /**
     * Updates the Attestation Certificate generation expiration date using the provided user input.
     *
     * @param expirationValue                    expiration value
     * @param isGenerateIssuedCertificateEnabled boolean value representation of the current policy
     *                                           option's state
     * @return string message that describes the result of this policy update
     */
    public String updateAttestationCertExpirationPolicy(final String expirationValue,
                                                        final boolean isGenerateIssuedCertificateEnabled) {
        PolicySettings policySettings = getDefaultPolicy();

        String successMessage;

        boolean canGenerateIssuedCertificateEnabled = isGenerateIssuedCertificateEnabled;

        if (policySettings.isIssueAttestationCertificate()) {
            String numOfDays;
            if (canGenerateIssuedCertificateEnabled) {
                successMessage = "Attestation Certificate generation expiration time enabled.";
                numOfDays = (expirationValue != null) ? expirationValue : PolicySettings.TEN_YEARS;
            } else {
                successMessage = "Attestation Certificate generation expiration time disabled.";
                numOfDays = policySettings.getValidityDays();
            }
            policySettings.setValidityDays(numOfDays);
        } else {
            canGenerateIssuedCertificateEnabled = false;
            successMessage = "Attestation Certificate generation is disabled, cannot set time expiration";
        }

        policySettings.setGenerateOnExpiration(canGenerateIssuedCertificateEnabled);

        policyRepository.saveAndFlush(policySettings);

        log.debug("Current ACA Policy after updating the attestation certificate "
                + "generation expiration value policy: {}", policySettings);

        return successMessage;
    }

    /**
     * Updates the LDevId Certificate generation expiration date using the provided user input.
     *
     * @param ldevIdExpirationValue              ldevid expiration value
     * @param isGenerateLDevIdCertificateEnabled boolean value representation of the current policy option's
     *                                           state
     * @return string message that describes the result of this policy update
     */
    public String updateLDevIdExpirationPolicy(final String ldevIdExpirationValue,
                                               final boolean isGenerateLDevIdCertificateEnabled) {
        PolicySettings policySettings = getDefaultPolicy();

        boolean canGenerateLDevIdCertificateEnabled = isGenerateLDevIdCertificateEnabled;

        String successMessage;

        if (policySettings.isIssueDevIdCertificate()) {
            String numOfDays;
            if (canGenerateLDevIdCertificateEnabled) {
                successMessage = "DevID Certificate generation expiration time enabled.";
                numOfDays =
                        (ldevIdExpirationValue != null) ? ldevIdExpirationValue : PolicySettings.TEN_YEARS;
            } else {
                successMessage = "DevID Certificate generation expiration time disabled.";
                numOfDays = policySettings.getDevIdValidityDays();
            }
            policySettings.setDevIdValidityDays(numOfDays);
        } else {
            canGenerateLDevIdCertificateEnabled = false;
            successMessage = "DevID Certificate generation is disabled, "
                    + "cannot set time expiration";
        }

        policySettings.setDevIdExpirationFlag(canGenerateLDevIdCertificateEnabled);

        policyRepository.saveAndFlush(policySettings);

        log.debug("Current ACA Policy after updating the devid certificate generation expiration "
                + "value policy: {}", policySettings);

        return successMessage;
    }

    /**
     * Updates the Attestation Certificate generation threshold value using the provided user input.
     *
     * @param thresholdValue                     threshold value
     * @param reissueThresholdValue              reissue threshold value
     * @param isGenerateIssuedCertificateEnabled boolean value representation of the current
     *                                           policy option's state
     * @return string message that describes the result of this policy update
     */
    public String updateAttestationCertThresholdPolicy(final String thresholdValue,
                                                       final String reissueThresholdValue,
                                                       final boolean isGenerateIssuedCertificateEnabled) {
        PolicySettings policySettings = getDefaultPolicy();

        boolean canGenerateIssuedCertificateEnabled = isGenerateIssuedCertificateEnabled;

        String successMessage;

        if (policySettings.isIssueAttestationCertificate()) {
            String threshold = canGenerateIssuedCertificateEnabled ? thresholdValue : reissueThresholdValue;
            successMessage = canGenerateIssuedCertificateEnabled
                    ? "Attestation Certificate generation threshold time enabled."
                    : "Attestation Certificate generation threshold time disabled.";

            if (threshold == null || threshold.isEmpty()) {
                threshold = PolicySettings.YEAR;
            }

            policySettings.setReissueThreshold(threshold);
        } else {
            canGenerateIssuedCertificateEnabled = false;
            successMessage = "Attestation Certificate generation is disabled, "
                    + "cannot set time expiration";
        }

        policySettings.setGenerateOnExpiration(canGenerateIssuedCertificateEnabled);

        policyRepository.saveAndFlush(policySettings);

        log.debug("Current ACA Policy after updating the aca attestation certificate generation "
                + "threshold policy: {}", policySettings);

        return successMessage;
    }

    /**
     * Updates the LDevId Certificate generation threshold value using the provided user input.
     *
     * @param ldevIdThresholdValue               ldevid threshold value
     * @param ldevIdReissueThresholdValue        ldevid reissue threshold value
     * @param isGenerateLDevIdCertificateEnabled boolean value representation of the current policy option's
     *                                           state
     * @return string message that describes the result of this policy update
     */
    public String updateDevIdThresholdPolicy(final String ldevIdThresholdValue,
                                             final String ldevIdReissueThresholdValue,
                                             final boolean isGenerateLDevIdCertificateEnabled) {
        PolicySettings policySettings = getDefaultPolicy();

        boolean canGenerateLDevIdCertificateEnabled = isGenerateLDevIdCertificateEnabled;

        String successMessage;

        if (policySettings.isIssueDevIdCertificate()) {
            String threshold = canGenerateLDevIdCertificateEnabled
                    ? ldevIdThresholdValue
                    : ldevIdReissueThresholdValue;

            successMessage = canGenerateLDevIdCertificateEnabled
                    ? "DevID Certificate generation threshold time enabled."
                    : "DevID Certificate generation threshold time disabled.";

            if (threshold == null || threshold.isEmpty()) {
                threshold = PolicySettings.YEAR;
            }

            policySettings.setDevIdReissueThreshold(threshold);
        } else {
            canGenerateLDevIdCertificateEnabled = false;
            successMessage = "DevID Certificate generation is disabled, cannot set threshold time";
        }

        policySettings.setDevIdExpirationFlag(canGenerateLDevIdCertificateEnabled);

        policyRepository.saveAndFlush(policySettings);

        log.debug("Current ACA Policy after updating the ldevid certificate generation "
                + "threshold policy: {}", policySettings);

        return successMessage;
    }

    /**
     * Updates save protobuf data to the ACA log policy using the provided user input.
     *
     * @param saveProtobufToLogOption string value representation of the current policy
     *                                option's state
     */
    public void updateSaveProtobufDataToLogPolicy(
            final String saveProtobufToLogOption) {
        PolicySettings policySettings = getDefaultPolicy();

        switch (saveProtobufToLogOption) {
            case "always-log-protobuf" -> {
                policySettings.setSaveProtobufToLogAlwaysEnabled(true);
                policySettings.setSaveProtobufToLogNeverEnabled(false);
                policySettings.setSaveProtobufToLogOnFailedValEnabled(false);
            }
            case "log-protobuf-on-fail-val" -> {
                policySettings.setSaveProtobufToLogOnFailedValEnabled(true);
                policySettings.setSaveProtobufToLogNeverEnabled(false);
                policySettings.setSaveProtobufToLogAlwaysEnabled(false);
            }
            case "never-log-protobuf" -> {
                policySettings.setSaveProtobufToLogNeverEnabled(true);
                policySettings.setSaveProtobufToLogAlwaysEnabled(false);
                policySettings.setSaveProtobufToLogOnFailedValEnabled(false);
            }
            default -> throw new IllegalArgumentException("There must be exactly three valid options for "
                    + "setting the policy to save protobuf data to the ACA log.");
        }

        policyRepository.saveAndFlush(policySettings);

        log.debug("Current ACA Policy after updating the save protobuf data to ACA log "
                + "policy: {}", policySettings);
    }

    /**
     * Retrieves a fresh load of the default policy from the DB.
     *
     * @return The default Supply Chain Policy
     */
    public PolicySettings getDefaultPolicy() {
        PolicySettings defaultSettings = this.policyRepository.findByName("Default");

        if (defaultSettings == null) {
            defaultSettings = new PolicySettings("Default",
                    "Settings are configured for no validation flags set.");
        }
        return defaultSettings;
    }

    /**
     * Takes in policy setting states and determines if policy configuration is
     * valid or not. PC Attribute Validation must have PC Validation Enabled PC
     * Validation must have EC Validation enabled
     *
     * @param isEcEnabled    EC Validation Policy State
     * @param isPcEnabled    PC Validation Policy State
     * @param isPcAttEnabled PC Attribute Validation Policy State
     * @return True if policy combination is valid
     */
    private boolean isPolicyValid(final boolean isEcEnabled, final boolean isPcEnabled,
                                  final boolean isPcAttEnabled) {
        if (isPcAttEnabled && !isPcEnabled) {
            return false;
        } else {
            return !isPcEnabled || isEcEnabled;
        }
    }
}
