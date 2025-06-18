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
     * Updates the Platform Certificate Validation policy according to user input.
     *
     * @param isPcValidationOptionEnabled boolean value representation of the current policy option's state
     * @return true if the policy was updated successfully; otherwise, false.
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
     * Updates the Platform Certificate Attribute Validation policy according to user input.
     *
     * @param isPcAttributeValidationOptionEnabled boolean value representation of the current policy option's state
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

        // save the policy to the DB
        policyRepository.saveAndFlush(policySettings);

        log.debug("Current ACA Policy after updating the platform credential "
                + "attribute credential validation policy: {}", policySettings);

        return true;
    }

    /**
     * Updates the Ignore Revision Attribute policy according to user input.
     *
     * @param isIgnoreRevisionAttributeOptionEnabled boolean value representation of the current policy option's state
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

        // save the policy to the DB
        policyRepository.saveAndFlush(policySettings);

        log.debug("Current ACA Policy after updating the ignore revision attribute"
                + " option policy: {}", policySettings);

        return true;
    }

    /**
     * Updates the Issued Attestation Certificate policy according to user input.
     *
     * @param isIssuedAttestationOptionEnabled boolean value representation of the current policy option's state
     */
    public void updateAttestationValidationPolicy(final boolean isIssuedAttestationOptionEnabled) {
        PolicySettings policySettings = getDefaultPolicy();

        if (!isIssuedAttestationOptionEnabled) {
            policySettings.setGenerateOnExpiration(false);
        }

        policySettings.setIssueAttestationCertificate(isIssuedAttestationOptionEnabled);

        // save the policy to the DB
        policyRepository.saveAndFlush(policySettings);

        log.debug("Current ACA Policy after updating the issued attestation validation "
                + " policy: {}", policySettings);
    }

    /**
     * Updates the DevId validation policy according to user input.
     *
     * @param isDevIdOptionEnabled boolean value representation of the current policy option's state
     */
    public void updateDevIdValidationPolicy(final boolean isDevIdOptionEnabled) {
        PolicySettings policySettings = getDefaultPolicy();

        if (!isDevIdOptionEnabled) {
            policySettings.setDevIdExpirationFlag(false);
        }

        policySettings.setIssueDevIdCertificate(isDevIdOptionEnabled);

        // save the policy to the DB
        policyRepository.saveAndFlush(policySettings);

        log.debug("Current ACA Policy after updating the devid validation "
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

        // save the policy to the DB
        policyRepository.saveAndFlush(policySettings);

        log.debug("Current ACA Policy after updating the endorsement credential "
                + "validation policy: {}", policySettings);

        return true;
    }

    /**
     * Updates the firmware validation policy according to user input.
     *
     * @param isFirmwareValidationOptionEnabled boolean value representation of the current policy option's state
     * @return true if the policy was updated successfully; otherwise, false.
     */
    public boolean updateFirmwareValidationPolicy(final boolean isFirmwareValidationOptionEnabled) {
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

        // save the policy to the DB
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

        // save the policy to the DB
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

        // save the policy to the DB
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
            log.error("Ignore Os Events cannot be enabled without Firmware Validation policy enabled.");
            return false;
        }

        if (isIgnoreOSEvtOptionEnabled) {
            policySettings.setIgnoreGptEnabled(true);
        }

        policySettings.setIgnoreOsEvtEnabled(isIgnoreOSEvtOptionEnabled);

        // save the policy to the DB
        policyRepository.saveAndFlush(policySettings);

        log.debug("Current ACA Policy after updating the ignore OS events policy:"
                + " {}", policySettings);

        return true;
    }

    /**
     * @param generateCertificateEnabled
     * @return string message that describes the result of this policy update
     */
    public String updateExpireOnValidationPolicy(final String expirationValue,
                                                 boolean generateCertificateEnabled) {
        PolicySettings policySettings = getDefaultPolicy();

        String successMessage;

        if (policySettings.isIssueAttestationCertificate()) {
            String numOfDays;
            if (generateCertificateEnabled) {
                successMessage = "Attestation Certificate generation expiration time enabled.";
                numOfDays = (expirationValue != null) ? expirationValue : PolicySettings.TEN_YEARS;
            } else {
                successMessage = "Attestation Certificate generation expiration time disabled.";
                numOfDays = policySettings.getValidityDays();
            }
            policySettings.setValidityDays(numOfDays);
        } else {
            generateCertificateEnabled = false;
            successMessage = "Attestation Certificate generation is disabled, cannot set time expiration";
        }

        policySettings.setGenerateOnExpiration(generateCertificateEnabled);

        // save the policy to the DB
        policyRepository.saveAndFlush(policySettings);

        log.debug("Current ACA Policy after updating the ignore OS events policy:"
                + " {}", policySettings);

        return successMessage;
    }

    /**
     * @return
     */
    public String updateDevIdExpireOnValPolicy(final String devIdExpirationValue,
                                               boolean generateDevIdCertificateEnabled) {
        PolicySettings policySettings = getDefaultPolicy();

        String successMessage;

        if (policySettings.isIssueDevIdCertificate()) {
            String numOfDays;
            if (generateDevIdCertificateEnabled) {
                successMessage = "DevID Certificate generation expiration time enabled.";
                numOfDays = (devIdExpirationValue != null) ? devIdExpirationValue : PolicySettings.TEN_YEARS;
            } else {
                successMessage = "DevID Certificate generation expiration time disabled.";
                numOfDays = policySettings.getDevIdValidityDays();
            }
            policySettings.setDevIdValidityDays(numOfDays);
        } else {
            generateDevIdCertificateEnabled = false;
            successMessage = "DevID Certificate generation is disabled, "
                    + "cannot set time expiration";
        }

        policySettings.setDevIdExpirationFlag(generateDevIdCertificateEnabled);

        log.debug("Current ACA Policy after updating the ignore OS events policy:"
                + " {}", policySettings);

        return successMessage;
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