package hirs.attestationca.persist.service;

import hirs.attestationca.persist.entity.manager.PolicyRepository;
import hirs.attestationca.persist.entity.userdefined.PolicySettings;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * A service layer class responsible for encapsulating all business logic related to the Policy Page.
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
     * Updates the Platform Certificate Validation policy option according to user input.
     *
     * @param isPcValidationOptionEnabled boolean value representation of the current
     *                                    pc validation policy option's state
     * @return true if the policy option was updated successfully; otherwise, false.
     */
    public boolean updatePCValidationPolicy(final boolean isPcValidationOptionEnabled) {
        PolicySettings policySettings = getDefaultPolicy();

        // If PC policy setting changes are invalid, log the error and return false
        if (!isPolicyValid(policySettings.isEcValidationEnabled(), isPcValidationOptionEnabled,
                policySettings.isPcAttributeValidationEnabled())) {
            log.error("The policy setting changes made while updating the platform credential "
                    + "validation policy are invalid.");
            return false;
        }

        if (isPcValidationOptionEnabled) {
            policySettings.setPcValidationEnabled(true);
        } else {
            policySettings.setPcValidationEnabled(false);
            policySettings.setPcAttributeValidationEnabled(false);
        }

        // save the policy to the DB
        policyRepository.saveAndFlush(policySettings);

        log.debug("ACA Policy has been set to the following after user has attempted to" +
                "update the platform credential validation policy: {}", policySettings);

        return true;
    }

    /**
     * Updates the Platform Certificate Attribute Validation policy option according to user input.
     *
     * @param isPcAttributeValidationOptionEnabled boolean value representation of the current
     *                                             pc attribute validation policy option's state
     * @return true if the policy option was updated successfully; otherwise, false.
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

        // save the policy to the DB
        policyRepository.saveAndFlush(policySettings);

        log.debug("Current ACA Policy after attempt to update the platform credential "
                + "attribute credential validation policy: {}", policySettings);

        return true;
    }

    /**
     * Updates the Ignore Revision Attribute policy option according to user input.
     *
     * @param isIgnoreRevisionAttributeOptionEnabled boolean value representation of the current ignore
     *                                               revision attribute policy option's state
     * @return true if the policy option was updated successfully; otherwise, false.
     */
    public boolean updateIgnoreRevisionAttributePolicy(boolean isIgnoreRevisionAttributeOptionEnabled) {
        PolicySettings policySettings = getDefaultPolicy();

        if (isIgnoreRevisionAttributeOptionEnabled && !policySettings.isPcAttributeValidationEnabled()) {
            log.error("Ignore Component Revision Attribute cannot be "
                    + "enabled without PC Attribute validation policy enabled.");
            return false;
        }

        policySettings.setIgnoreRevisionEnabled(isIgnoreRevisionAttributeOptionEnabled);

        // save the policy to the DB
        policyRepository.saveAndFlush(policySettings);

        log.debug("Current ACA Policy after attempt to update the ignore revision attribute"
                + " option policy: {}", policySettings);

        return true;
    }

    /**
     * Updates the Issued Attestation Certificate policy option according to user input.
     *
     * @param isIssuedAttestationOptionEnabled boolean value representation of the current Issued
     *                                         Attestation policy option's state
     */
    public void updateAttestationValidationPolicy(boolean isIssuedAttestationOptionEnabled) {
        PolicySettings policySettings = getDefaultPolicy();

        if (!isIssuedAttestationOptionEnabled) {
            policySettings.setGenerateOnExpiration(false);
        }

        policySettings.setIssueAttestationCertificate(isIssuedAttestationOptionEnabled);

        // save the policy to the DB
        policyRepository.saveAndFlush(policySettings);

        log.debug("Current ACA Policy after attempt to update the issued attestation validation "
                + " policy: {}", policySettings);
    }

    /**
     * Updates the DevId validation policy option according to user input.
     *
     * @param isDevIdOptionEnabled boolean value representation of the current DevIds Validation
     *                             policy option's state
     */
    public void updateDevIdValidationPolicy(boolean isDevIdOptionEnabled) {
        PolicySettings policySettings = getDefaultPolicy();

        if (!isDevIdOptionEnabled) {
            policySettings.setDevIdExpirationFlag(false);
        }

        policySettings.setIssueDevIdCertificate(isDevIdOptionEnabled);

        // save the policy to the DB
        policyRepository.saveAndFlush(policySettings);

        log.debug("Current ACA Policy after attempt to update the dev id validation "
                + " policy: {}", policySettings);
    }

    /**
     * Updates the Endorsement Credential validation policy option according to user input.
     *
     * @param isEcValidationOptionEnabled boolean value representation of the current EC Validation
     *                                    policy option's state
     * @return true if the policy option was updated successfully; otherwise, false.
     */
    public boolean updateECValidationPolicy(boolean isEcValidationOptionEnabled) {
        PolicySettings policySettings = getDefaultPolicy();

        if (!isPolicyValid(isEcValidationOptionEnabled, policySettings.isPcValidationEnabled(),
                policySettings.isPcAttributeValidationEnabled())) {
            log.error("To disable Endorsement Credential Validation, Platform Validation"
                    + " must also be disabled.");
            return false;
        }

        policySettings.setEcValidationEnabled(isEcValidationOptionEnabled);

        // save the policy to the DB
        policyRepository.saveAndFlush(policySettings);

        log.debug("Current ACA Policy after attempt to update the endorsement credential "
                + "validation policy: {}", policySettings);

        return true;
    }

    /**
     * Updates the firmware validation policy option according to user input.
     *
     * @param isFirmwareValidationOptionEnabled boolean value representation of the current firmware
     *                                          validation policy option's state
     * @return true if the policy option was updated successfully; otherwise, false.
     */
    public boolean updateFirmwareValidationPolicy(boolean isFirmwareValidationOptionEnabled) {
        PolicySettings policySettings = getDefaultPolicy();

        //If firmware is enabled without PC attributes, disallow change
        if (isFirmwareValidationOptionEnabled && !policySettings.isPcAttributeValidationEnabled()) {
            log.error("Firmware validation cannot be enabled without PC Attributes policy enabled.");
            return false;
        }

        if (isFirmwareValidationOptionEnabled) {
            policySettings.setFirmwareValidationEnabled(true);
            policySettings.setIgnoreGptEnabled(true);
        } else {
            policySettings.setFirmwareValidationEnabled(false);
            policySettings.setIgnoreImaEnabled(false);
            policySettings.setIgnoretBootEnabled(false);
            policySettings.setIgnoreOsEvtEnabled(false);
        }

        // save the policy to the DB
        policyRepository.saveAndFlush(policySettings);

        log.debug("Current ACA Policy after attempt to update the firmware validation "
                + " policy: {}", policySettings);

        return true;
    }

    /**
     * Updates the ignore IMA policy option according to user input.
     *
     * @param ignoreImaOptionEnabled boolean value representation of the current IMA policy option's state
     * @return true if the policy option was updated successfully; otherwise, false.
     */
    public boolean updateIgnoreImaPolicy(boolean ignoreImaOptionEnabled) {
        PolicySettings policySettings = getDefaultPolicy();

        //If Ignore IMA is enabled without firmware, disallow change
        if (ignoreImaOptionEnabled && !policySettings.isFirmwareValidationEnabled()) {
            log.error("Ignore IMA cannot be enabled without Firmware Validation policy enabled.");
            return false;
        }

        policySettings.setIgnoreImaEnabled(ignoreImaOptionEnabled);

        // save the policy to the DB
        policyRepository.saveAndFlush(policySettings);

        log.debug("Current ACA Policy after attempt to update the ignore IMA policy:"
                + " {}", policySettings);

        return true;
    }

    /**
     * Updates the ignore TBoot policy option according to user input.
     *
     * @param ignoreTbootOptionEnabled boolean value representation of the current TBoot
     *                                 policy option's state
     * @return true if the policy option was updated successfully; otherwise, false.
     */
    public boolean updateIgnoreTBootPolicy(boolean ignoreTbootOptionEnabled) {
        PolicySettings policySettings = getDefaultPolicy();

        //If Ignore TBoot is enabled without firmware, disallow change
        if (ignoreTbootOptionEnabled && !policySettings.isFirmwareValidationEnabled()) {
            log.error("Ignore TBoot cannot be enabled without Firmware Validation policy enabled.");
            return false;
        }

        policySettings.setIgnoretBootEnabled(ignoreTbootOptionEnabled);

        // save the policy to the DB
        policyRepository.saveAndFlush(policySettings);

        log.debug("Current ACA Policy after attempt to update the ignore TBoot policy:"
                + " {}", policySettings);

        return true;
    }

    /**
     * @param ignoreGptOptionEnabled
     * @return
     */
    public boolean updateIgnoreGptPolicy(boolean ignoreGptOptionEnabled) {
        PolicySettings policySettings = getDefaultPolicy();

        //If Ignore TBoot is enabled without firmware, disallow change
        if (ignoreGptOptionEnabled && !policySettings.isFirmwareValidationEnabled()) {
            log.error("Ignore GPT Events cannot be enabled without Firmware Validation policy enabled.");
            return false;
        }

        policySettings.setIgnoreGptEnabled(ignoreGptOptionEnabled);

        // save the policy to the DB
        policyRepository.saveAndFlush(policySettings);

        log.debug("Current ACA Policy after attempt to update the ignore GPT events policy:"
                + " {}", policySettings);

        return true;
    }

    /**
     * @param ignoreOSOptionEnabled
     * @return
     */
    public boolean updateIgnoreOSEventsPolicy(boolean ignoreOSOptionEnabled) {
        PolicySettings policySettings = getDefaultPolicy();

        //todo
//        //If Ignore TBoot is enabled without firmware, disallow change
//        if (ignoreGptOptionEnabled && !policySettings.isFirmwareValidationEnabled()) {
//            log.error("Ignore GPT Events cannot be enabled without Firmware Validation policy enabled.");
//            return false;
//        }

        policySettings.setIgnoreOsEvtEnabled(ignoreOSOptionEnabled);

        // save the policy to the DB
        policyRepository.saveAndFlush(policySettings);

        log.debug("Current ACA Policy after attempt to update the ignore OS events policy:"
                + " {}", policySettings);

        return true;
    }

    public boolean updateSaveProtobufDataPolicy() {


        return true;
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