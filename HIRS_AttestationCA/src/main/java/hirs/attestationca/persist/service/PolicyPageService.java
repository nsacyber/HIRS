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

    /**
     * @param isPcValidationOptionEnabled
     * @return
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
     * @param isPcAttributeValidationOptionEnabled
     * @return
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
     * @param isIgnoreRevisionAttributeOptionEnabled
     * @return
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
     * @param isIssuedAttestationOptionEnabled
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
     * @param isDevIdOptionEnabled
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

    public boolean updateECValidationPolicy(boolean ecValidationOptionEnabled) {
        return true;
    }

    /**
     * Helper function to get a fresh load of the default policy from the DB.
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
}