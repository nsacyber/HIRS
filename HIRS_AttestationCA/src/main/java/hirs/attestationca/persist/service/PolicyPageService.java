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

        if (!isPcAttributeValidationOptionEnabled) {
            policySettings.setIgnorePcieVpdEnabled(false);
            policySettings.setIgnoreRevisionEnabled(false);
        }

        policyRepository.saveAndFlush(policySettings);

        log.debug("Current ACA Policy after updating the platform credential "
                + "attribute credential validation policy: {}", policySettings);

        return true;
    }

    /**
     * Updates the Ignore Revision Attribute policy under the platform credential attribute validation policy
     * setting according to user input.
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
     * Updates the Ignore PCIE VPD Attribute policy under the platform credential attribute validation
     * policy setting according to user input.
     *
     * @param isIgnorePcieVpdOptionEnabled boolean value representation of the current policy
     *                                     option's state
     * @return true if the policy was updated successfully; otherwise, false.
     */
    public boolean updateIgnorePCIEVpdPolicy(final boolean isIgnorePcieVpdOptionEnabled) {
        PolicySettings policySettings = getDefaultPolicy();

        if (isIgnorePcieVpdOptionEnabled && !policySettings.isPcAttributeValidationEnabled()) {
            log.error("Ignore PCIE VPD Attribute cannot be enabled without PC Attribute "
                    + "validation policy enabled.");
            return false;
        }

        policySettings.setIgnorePcieVpdEnabled(isIgnorePcieVpdOptionEnabled);

        policyRepository.saveAndFlush(policySettings);

        log.debug("Current ACA Policy after updating the ignore "
                + "pcie vpd policy: {}", policySettings);

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
     * Updates the ignore IMA policy under the firmware validation policy setting according to user input.
     *
     * @param isIgnoreImaOptionEnabled boolean value representation of the current policy option's state
     * @return true if the policy was updated successfully; otherwise, false.
     */
    public boolean updateIgnoreImaPolicy(final boolean isIgnoreImaOptionEnabled) {
        PolicySettings policySettings = getDefaultPolicy();

        //If Ignore IMA is enabled and firmware validation is not enabled, disallow change
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
     * Updates the ignore TBoot policy under the firmware validation policy setting
     * according to user input.
     *
     * @param isIgnoreTbootOptionEnabled boolean value representation of the current policy option's state
     * @return true if the policy was updated successfully; otherwise, false.
     */
    public boolean updateIgnoreTBootPolicy(final boolean isIgnoreTbootOptionEnabled) {
        PolicySettings policySettings = getDefaultPolicy();

        //If Ignore TBoot is enabled and firmware validation is not enabled, disallow change
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
     * Updates the ignore GPT events policy under the firmware validation policy setting
     * according to user input.
     *
     * @param isIgnoreGptOptionEnabled boolean value representation of the current policy option's state
     * @return true if the policy was updated successfully; otherwise, false.
     */
    public boolean updateIgnoreGptEventsPolicy(final boolean isIgnoreGptOptionEnabled) {
        PolicySettings policySettings = getDefaultPolicy();

        //If Ignore TBoot is enabled and firmware validation is not enabled, disallow change
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
     * Updates the ignore OS events policy under the firmware validation policy setting
     * according to user input.
     *
     * @param isIgnoreOSEvtOptionEnabled boolean value representation of the current policy option's state
     * @return true if the policy was updated successfully; otherwise, false.
     */
    public boolean updateIgnoreOSEventsPolicy(final boolean isIgnoreOSEvtOptionEnabled) {
        PolicySettings policySettings = getDefaultPolicy();

        //If Ignore OS events is enabled and firmware validation is not enabled, disallow change
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
     * Updates the Issued Attestation Certificate generation policy according to user input.
     *
     * @param isIssuedAttestationOptionEnabled boolean value representation of the current policy option's
     *                                         state
     */
    public void updateIssuedAttestationGenerationPolicy(final boolean isIssuedAttestationOptionEnabled) {
        PolicySettings policySettings = getDefaultPolicy();

        if (!isIssuedAttestationOptionEnabled) {
            policySettings.setGenerateAttestationCertificateOnExpiration(false);
        }

        policySettings.setIssueAttestationCertificateEnabled(isIssuedAttestationOptionEnabled);

        policyRepository.saveAndFlush(policySettings);

        log.debug("Current ACA Policy after updating the issued attestation certificate generation "
                + " policy: {}", policySettings);
    }

    /**
     * Updates the Attestation Certificate generation expiration date under the generate attestation
     * certificate policy setting using the provided user input.
     *
     * @param canGenerateAttestationCertificateOnExpiration boolean value representation of the current
     *                                                      policy option's state
     * @param attestCertExpirationValue                     attestation certificate expiration value
     * @return true if the policy was updated successfully; otherwise, false.
     */
    public boolean updateAttestationCertExpirationPolicy(
            final boolean canGenerateAttestationCertificateOnExpiration,
            final int attestCertExpirationValue) {
        PolicySettings policySettings = getDefaultPolicy();

        if (canGenerateAttestationCertificateOnExpiration
                && !policySettings.isIssueAttestationCertificateEnabled()) {
            log.error("Cannot set the attestation certificate expiration value when"
                    + " the attestation certificate generation expiration policy is not enabled.");
            return false;
        }

        policySettings.setGenerateAttestationCertificateOnExpiration(
                canGenerateAttestationCertificateOnExpiration);

        if (canGenerateAttestationCertificateOnExpiration) {
            final int numOfDays = (attestCertExpirationValue != 0) ? attestCertExpirationValue
                    : PolicySettings.TEN_YEARS_IN_DAYS;
            policySettings.setValidityDays(numOfDays);
        }

        policyRepository.saveAndFlush(policySettings);

        log.debug("Current ACA Policy after updating the attestation certificate "
                + "generation expiration value policy: {}", policySettings);

        return true;
    }

    /**
     * Updates the Attestation Certificate generation threshold value under the generate attestation
     * certificate policy setting  using the provided user input.
     * <p>
     *
     * @param canGenerateAttestationCertificateOnExpiration boolean value representation of the current
     *                                                      policy option's state
     * @param attestCertThresholdValue                      threshold value
     * @return true if the policy was updated successfully; otherwise, false.
     */
    public boolean updateAttestationCertThresholdPolicy(
            final boolean canGenerateAttestationCertificateOnExpiration,
            final int attestCertThresholdValue) {
        PolicySettings policySettings = getDefaultPolicy();

        if (canGenerateAttestationCertificateOnExpiration
                && !policySettings.isIssueAttestationCertificateEnabled()) {
            log.error("Cannot set the attestation certificate threshold value when"
                    + " the attestation certificate generation expiration policy is not enabled.");
            return false;
        }

        policySettings.setGenerateAttestationCertificateOnExpiration(
                canGenerateAttestationCertificateOnExpiration);

        if (canGenerateAttestationCertificateOnExpiration) {
            final int threshold =
                    (attestCertThresholdValue != 0) ? attestCertThresholdValue
                            : PolicySettings.A_YEAR_IN_DAYS;
            policySettings.setReissueThreshold(threshold);
        }

        policyRepository.saveAndFlush(policySettings);

        log.debug("Current ACA Policy after updating the attestation certificate generation "
                + "threshold policy: {}", policySettings);

        return true;
    }

    /**
     * Updates the LDevID validation policy according to user input.
     *
     * @param isLDevIDOptionEnabled boolean value representation of the current policy option's state
     */
    public void updateLDevIDGenerationPolicy(final boolean isLDevIDOptionEnabled) {
        PolicySettings policySettings = getDefaultPolicy();

        if (!isLDevIDOptionEnabled) {
            policySettings.setGenerateDevIdCertificateOnExpiration(false);
        }

        policySettings.setIssueDevIdCertificateEnabled(isLDevIDOptionEnabled);

        policyRepository.saveAndFlush(policySettings);

        log.debug("Current ACA Policy after updating the LDevID validation "
                + " policy: {}", policySettings);
    }

    /**
     * Updates the LDevID Certificate generation expiration date under the generate LDevID
     * certificate policy setting using the provided user input.
     *
     * @param canGenerateLDevIDCertificateOnExpiration boolean value representation of the current
     *                                                 policy option's state
     * @param lDevIDExpirationValue                    LDevID certificate expiration value
     * @return true if the policy was updated successfully; otherwise, false.
     */
    public boolean updateLDevIDExpirationPolicy(final boolean canGenerateLDevIDCertificateOnExpiration,
                                                final int lDevIDExpirationValue) {
        PolicySettings policySettings = getDefaultPolicy();

        if (canGenerateLDevIDCertificateOnExpiration
                && !policySettings.isIssueDevIdCertificateEnabled()) {
            log.error("Cannot set the LDevID certificate expiration value when"
                    + " the LDevID certificate generation expiration policy is not enabled.");
            return false;
        }

        policySettings.setGenerateDevIdCertificateOnExpiration(canGenerateLDevIDCertificateOnExpiration);

        if (canGenerateLDevIDCertificateOnExpiration) {
            final int numOfDays = (lDevIDExpirationValue != 0) ? lDevIDExpirationValue
                    : PolicySettings.TEN_YEARS_IN_DAYS;
            policySettings.setDevIdValidityDays(numOfDays);
        }

        policyRepository.saveAndFlush(policySettings);

        log.debug("Current ACA Policy after updating the LDevID certificate generation expiration "
                + "value policy: {}", policySettings);

        return true;
    }

    /**
     * Updates the LDevID Certificate generation threshold value under the generate LDevID
     * certificate policy setting using the provided user input.
     *
     * @param canGenerateLDevIDCertificateOnExpiration boolean value representation of the current policy option's
     *                                                 state
     * @param lDevIDThresholdValue                     LDevID threshold value
     * @return true if the policy was updated successfully; otherwise, false.
     */
    public boolean updateLDevIDThresholdPolicy(final boolean canGenerateLDevIDCertificateOnExpiration,
                                               final int lDevIDThresholdValue) {
        PolicySettings policySettings = getDefaultPolicy();

        if (canGenerateLDevIDCertificateOnExpiration
                && !policySettings.isIssueDevIdCertificateEnabled()) {
            log.error("Cannot set the LDevID certificate threshold value when"
                    + " the LDevID certificate generation expiration policy is not enabled.");
            return false;
        }

        policySettings.setGenerateDevIdCertificateOnExpiration(canGenerateLDevIDCertificateOnExpiration);

        if (canGenerateLDevIDCertificateOnExpiration) {
            final int lDevIDThreshold =
                    (lDevIDThresholdValue != 0) ? lDevIDThresholdValue : PolicySettings.A_YEAR_IN_DAYS;
            policySettings.setDevIdReissueThreshold(lDevIDThreshold);
        }

        policyRepository.saveAndFlush(policySettings);

        log.debug("Current ACA Policy after updating the LDevID certificate generation "
                + "threshold policy: {}", policySettings);

        return true;
    }

    /**
     * Updates save protobuf data to the ACA log policy using the provided user input.
     *
     * @param saveProtobufToLogOption string value representation of the current policy
     *                                option's state
     */
    public void updateSaveProtobufDataToLogPolicy(final String saveProtobufToLogOption) {
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
