package hirs.attestationca.portal.page;

import hirs.attestationca.persist.entity.userdefined.PolicySettings;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * PolicyPage model object to demonstrate data exchange between policy.jsp page
 * form and controller.
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
public class PolicyPageModel {
    // Variables to communicate policy settings to page
    private boolean enableEcValidation;
    private boolean enablePcCertificateValidation;
    private boolean enablePcCertificateAttributeValidation;
    private boolean enableIgnoreRevisionAttribute;
    private boolean enableFirmwareValidation;
    private boolean issueAttestationCertificate;
    private boolean issueDevIdCertificate;
    private boolean generateOnExpiration;
    private boolean devIdExpirationFlag;
    private boolean enableIgnoreIma;
    private boolean enableIgnoreTboot;
    private boolean enableIgnoreGpt;
    private boolean enableIgnoreOsEvt;
    private boolean enableSaveProtobufToLog;
    private boolean enableSaveSuccessProtobufToLog;
    private boolean enableSaveFailedProtobufToLog;

    // Variables to get policy settings from page
    private String pcValidate;
    private String pcAttributeValidate;
    private String ignoreRevisionAttribute;
    private String ecValidate;
    private String fmValidate;
    private String attestationCertificateIssued;
    private String devIdCertificateIssued;
    private String generationExpirationOn;
    private String devIdExpirationChecked;
    private String numOfValidDays;
    private String reissueThreshold;
    private String devIdReissueThreshold;
    private String ignoreIma;
    private String ignoretBoot;
    private String ignoreGpt;
    private String ignoreOsEvt;
    private String expirationValue;
    private String devIdExpirationValue;
    private String thresholdValue;
    private String devIdThresholdValue;
    private String saveProtobufToLogValue;

    /**
     * Constructor. Sets fields from policy.
     *
     * @param policySettings The supply chain policy
     */
    public PolicyPageModel(final PolicySettings policySettings) {
        this.enableEcValidation = policySettings.isEcValidationEnabled();
        this.enablePcCertificateValidation = policySettings.isPcValidationEnabled();
        this.enablePcCertificateAttributeValidation = policySettings.isPcAttributeValidationEnabled();
        this.enableIgnoreRevisionAttribute = policySettings.isIgnoreRevisionEnabled();
        this.enableFirmwareValidation = policySettings.isFirmwareValidationEnabled();
        this.issueAttestationCertificate = policySettings.isIssueAttestationCertificate();
        this.issueDevIdCertificate = policySettings.isIssueDevIdCertificate();
        this.generateOnExpiration = policySettings.isGenerateOnExpiration();
        this.devIdExpirationFlag = policySettings.isDevIdExpirationFlag();
        this.numOfValidDays = policySettings.getValidityDays();
        this.reissueThreshold = policySettings.getReissueThreshold();
        this.expirationValue = policySettings.getValidityDays();
        this.thresholdValue = policySettings.getReissueThreshold();
        this.devIdExpirationValue = policySettings.getDevIdValidityDays();
        this.devIdReissueThreshold = policySettings.getDevIdReissueThreshold();
        this.devIdThresholdValue = policySettings.getDevIdReissueThreshold();
        this.enableSaveProtobufToLog = policySettings.isSaveProtobufDataToLogEnabled();
        // pcrPolicy
        this.enableIgnoreIma = policySettings.isIgnoreImaEnabled();
        this.enableIgnoreTboot = policySettings.isIgnoretBootEnabled();
        this.enableIgnoreGpt = policySettings.isIgnoreGptEnabled();
        this.enableIgnoreOsEvt = policySettings.isIgnoreOsEvtEnabled();
    }
}
