package hirs.attestationca.portal.page;

import hirs.attestationca.persist.entity.userdefined.PolicySettings;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * PolicyPage model object to demonstrate data exchange between policy.html page
 * form and controller.
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
public class PolicyPageModel {
    private boolean ecValidationEnabled;
    private boolean pcValidationEnabled;
    private boolean pcAttributeValidationEnabled;
    private boolean ignoreRevisionAttributeEnabled;
    private boolean firmwareValidationEnabled;
    private boolean issueAttestationCertificateEnabled;
    private boolean issueDevIdCertificateEnabled;
    private boolean generateAttestationCertOnExpirationEnabled;
    private boolean generateDevIdCertOnExpirationEnabled;
    private boolean ignoreImaEnabled;
    private boolean ignoreTbootEnabled;
    private boolean ignoreGptEnabled;
    private boolean ignoreOsEvtEnabled;
    private boolean ignorePcieVpdAttributeEnabled;
    private boolean logProtobufOnFailedVal;
    private boolean logProtobufNever;
    private boolean logProtobufAlways;

    // Variables to get policy settings from page
    private Long reissueThreshold;
    private Long devIdReissueThreshold;
    private Long generateAttestCertExpirationValue;
    private Long generateDevIdCertExpirationValue;
    private Long generateAttestCertThresholdValue;
    private Long generateDevIdCertThresholdValue;
    private String saveProtobufToLogOption;

    /**
     * Constructor. Sets fields from policy.
     *
     * @param policySettings The supply chain policy
     */
    public PolicyPageModel(final PolicySettings policySettings) {
        this.ecValidationEnabled = policySettings.isEcValidationEnabled();
        this.pcValidationEnabled = policySettings.isPcValidationEnabled();
        this.pcAttributeValidationEnabled = policySettings.isPcAttributeValidationEnabled();
        this.ignoreRevisionAttributeEnabled = policySettings.isIgnoreRevisionEnabled();
        this.firmwareValidationEnabled = policySettings.isFirmwareValidationEnabled();
        this.issueAttestationCertificateEnabled = policySettings.isIssueAttestationCertificateEnabled();
        this.issueDevIdCertificateEnabled = policySettings.isIssueDevIdCertificateEnabled();
        this.generateAttestationCertOnExpirationEnabled =
                policySettings.isGenerateAttestationCertificateOnExpiration();
        this.generateDevIdCertOnExpirationEnabled = policySettings.isGenerateDevIdCertificateOnExpiration();
        this.logProtobufOnFailedVal = policySettings.isSaveProtobufToLogOnFailedValEnabled();
        this.logProtobufAlways = policySettings.isSaveProtobufToLogAlwaysEnabled();
        this.logProtobufNever = policySettings.isSaveProtobufToLogNeverEnabled();
        this.ignorePcieVpdAttributeEnabled = policySettings.isIgnorePcieVpdEnabled();
        this.ignoreImaEnabled = policySettings.isIgnoreImaEnabled();
        this.ignoreTbootEnabled = policySettings.isIgnoretBootEnabled();
        this.ignoreGptEnabled = policySettings.isIgnoreGptEnabled();
        this.ignoreOsEvtEnabled = policySettings.isIgnoreOsEvtEnabled();

        this.reissueThreshold = policySettings.getReissueThreshold();
        this.devIdReissueThreshold = policySettings.getDevIdReissueThreshold();
        this.generateAttestCertExpirationValue = policySettings.getValidityDays();
        this.generateAttestCertThresholdValue = policySettings.getReissueThreshold();
        this.generateDevIdCertExpirationValue = policySettings.getDevIdValidityDays();
        this.generateDevIdCertThresholdValue = policySettings.getDevIdReissueThreshold();

    }
}
