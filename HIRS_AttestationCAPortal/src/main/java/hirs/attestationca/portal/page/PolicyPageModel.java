package hirs.attestationca.portal.page;

import hirs.attestationca.portal.entity.userdefined.SupplyChainSettings;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * PolicyPage model object to demonstrate data exchange between policy.jsp page
 * form form and controller.
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
    private boolean enableFirmwareValidation;
    private boolean issueAttestationCertificate;
    private boolean issueDevIdCertificate;
    private boolean generateOnExpiration;
    private boolean devIdExpirationFlag;
    private boolean enableIgnoreIma;
    private boolean enableIgnoreTboot;
    private boolean enableIgnoreGpt;
    private boolean enableIgnoreOsEvt;

    // Variables to get policy settings from page
    private String pcValidate;
    private String pcAttributeValidate;
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

    /**
     * Constructor. Sets fields from policy.
     *
     * @param policy The supply chain policy
     */
    public PolicyPageModel(final SupplyChainSettings policy) {
        this.enableEcValidation = policy.isEcValidationEnabled();
        this.enablePcCertificateValidation = policy.isPcValidationEnabled();
        this.enablePcCertificateAttributeValidation = policy.isPcAttributeValidationEnabled();
        this.enableFirmwareValidation = policy.isFirmwareValidationEnabled();
        this.issueAttestationCertificate = policy.isIssueAttestationCertificate();
        this.issueDevIdCertificate = policy.isIssueDevIdCertificate();
        this.generateOnExpiration = policy.isGenerateOnExpiration();
        this.devIdExpirationFlag = policy.isDevIdExpirationFlag();
        this.numOfValidDays = policy.getValidityDays();
        this.reissueThreshold = policy.getReissueThreshold();
        this.expirationValue = policy.getValidityDays();
        this.thresholdValue = policy.getReissueThreshold();
        this.devIdExpirationValue = policy.getDevIdValidityDays();
        this.devIdReissueThreshold = policy.getDevIdReissueThreshold();
        this.devIdThresholdValue = policy.getDevIdReissueThreshold();
        // pcrPolicy
        this.enableIgnoreIma = policy.isIgnoreImaEnabled();
        this.enableIgnoreTboot = policy.isIgnoretBootEnabled();
        this.enableIgnoreGpt = policy.isIgnoreGptEnabled();
        this.enableIgnoreOsEvt = policy.isIgnoreOsEvtEnabled();
    }
}
