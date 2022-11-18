package hirs.attestationca.portal.model;

import hirs.attestationca.policy.SupplyChainPolicy;

/**
 * PolicyPage model object to demonstrate data exchange between policy.jsp page
 * form form and controller.
 */
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
    public PolicyPageModel(final SupplyChainPolicy policy) {
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
        this.enableIgnoreIma = policy.isIgnoreImaEnabled();
        this.enableIgnoreTboot = policy.isIgnoreTbootEnabled();
        this.enableIgnoreGpt = policy.isIgnoreGptEnabled();
        this.enableIgnoreOsEvt = policy.isIgnoreOsEvtEnabled();
        this.expirationValue = policy.getValidityDays();
        this.thresholdValue = policy.getReissueThreshold();
        this.devIdExpirationValue = policy.getDevIdValidityDays();
        this.devIdReissueThreshold = policy.getDevIdReissueThreshold();
        this.devIdThresholdValue = policy.getDevIdReissueThreshold();
    }

    /**
     * Default constructor required by Spring.
     */
    public PolicyPageModel() {
    }

    /**
     * Gets the EC Validation state.
     *
     * @return the validation state.
     */
    public boolean getEnableEcValidation() {
        return enableEcValidation;
    }

    /**
     * Gets the Platform Certificate Validation state.
     *
     * @return the validation state.
     */
    public boolean getEnablePcCertificateValidation() {
        return enablePcCertificateValidation;
    }

    /**
     * Gets the Platform Certificate AttributeValidation state.
     *
     * @return the validation state.
     */
    public boolean getEnablePcCertificateAttributeValidation() {
        return enablePcCertificateAttributeValidation;
    }

    /**
     * Gets the Firmware Validation state.
     *
     * @return the validation state.
     */
    public boolean getEnableFirmwareValidation() {
        return enableFirmwareValidation;
    }

    /**
     * Gets the Attestation Certificate issued State.
     *
     * @return the issued state.
     */
    public boolean isIssueAttestationCertificate() {
        return issueAttestationCertificate;
    }

    /**
     * Gets the Dev ID Certificate issued State.
     *
     * @return the issued state.
     */
    public boolean isIssueDevIdCertificate() {
        return issueDevIdCertificate;
    }

    /**
     * Gets the state of generating a certificate.
     *
     * @return true or false
     */
    public boolean isGenerateOnExpiration() {
        return generateOnExpiration;
    }

    /**
     *  Gets the Enable Ignore IMA state.
     * @return the validation state.
     */
    public boolean getEnableIgnoreIma() {
        return enableIgnoreIma;
    }

    /**
     * Gets the Enable Ignore TBoot state.
     * @return the validation state.
     */
    public boolean getEnableIgnoreTboot() {
        return enableIgnoreTboot;
    }

    /**
     * Gets the Enable Ignore GPT state.
     * @return the validation state.
     */
    public boolean getEnableIgnoreGpt() {
        return enableIgnoreGpt;
    }

    /**
     * Gets the Enable Ignore Os Events state.
     * @return the validation state.
     */
    public boolean getEnableIgnoreOsEvt() {
        return enableIgnoreOsEvt;
    }

    /**
     * Gets the EC Validation value.
     *
     * @return the model string representation of this field (checked or unchecked)
     */
    public String getEcValidate() {
        return ecValidate;
    }

    /**
     * Gets the Platform Certificate Validation value.
     *
     * @return the model string representation of this field (checked or unchecked)
     */
    public String getPcValidate() {
        return pcValidate;
    }

    /**
     * Gets the platform certificate attribute validation value.
     *
     * @return the model string representation of this field (checked or unchecked)
     */
    public String getPcAttributeValidate() {
        return pcAttributeValidate;
    }

    /**
     * Gets the Firmware Validation value.
     *
     * @return the model string representation of this field (checked or unchecked)
     */
    public String getFmValidate() {
        return fmValidate;
    }

    /**
     * Gets the attestation certificate issued state.
     *
     * @return the model string representation of this field.
     */
    public String getAttestationCertificateIssued() {
        return attestationCertificateIssued;
    }

    /**
     * Gets the DevID certificate issued state.
     *
     * @return the model string representation of this field.
     */
    public String getDevIdCertificateIssued() {
        return devIdCertificateIssued;
    }

    /**
     * Gets the number of selected valid days.
     *
     * @return the number of the days for validity
     */
    public String getNumOfValidDays() {
        return numOfValidDays;
    }

    /**
     * Gets the number of selected threshold days.
     *
     * @return the number of the days for reissue
     */
    public String getReissueThreshold() {
        return reissueThreshold;
    }

    /**
     * Gets the number of selected threshold days.
     *
     * @return the number of the days for reissue
     */
    public String getDevIdReissueThreshold() {
        return devIdReissueThreshold;
    }

    /**
     * Gets the Ignore IMA validation value.
     *
     * @return the model string representation of this field (checked or unchecked)
     */
    public String getIgnoreIma() {
        return ignoreIma;
    }

    /**
     * Gets the Ignore TBoot validation value.
     *
     * @return the model string representation of this field (checked or unchecked)
     */
    public String getIgnoretBoot() {
        return ignoretBoot;
    }

    /**
     * Gets the Ignore GPT validation value.
     *
     * @return the model string representation of this field (checked or unchecked)
     */
    public String getIgnoreGpt() {
        return ignoreGpt;
    }

    /**
     * Gets the Ignore Os Evt validation value.
     *
     * @return the model string representation of this field (checked or unchecked)
     */
    public String getIgnoreOsEvt() {
        return ignoreOsEvt;
    }

    /**
     * Sets the EC Validation state.
     *
     * @param enableEcValidation true if performing validation, false otherwise
     */
    public void setEnableEcValidation(final boolean enableEcValidation) {
        this.enableEcValidation = enableEcValidation;
    }

    /**
     * Sets the Platform Certificate Validation state.
     *
     * @param enablePcCertificateValidation true if performing validation, false otherwise
     */
    public void setEnablePcCertificateValidation(final boolean enablePcCertificateValidation) {
        this.enablePcCertificateValidation = enablePcCertificateValidation;
    }

    /**
     * Sets the Platform Certificate Attribute Validation state.
     *
     * @param enablePcCertificateAttributeValidation true if performing validation, false otherwise
     */
    public void setEnablePcCertificateAttributeValidation(
            final boolean enablePcCertificateAttributeValidation) {
        this.enablePcCertificateAttributeValidation = enablePcCertificateAttributeValidation;
    }

    /**
     * Sets the Firmware Validation state.
     *
     * @param enableFirmwareValidation true if performing validation, false otherwise
     */
    public void setEnableFirmwareValidation(final boolean enableFirmwareValidation) {
        this.enableFirmwareValidation = enableFirmwareValidation;
    }

    /**
     * Sets the Attestation Certificate Issued state.
     *
     * @param issueAttestationCertificate true if generating Certificates.
     */
    public void setIssueAttestationCertificate(final boolean issueAttestationCertificate) {
        this.issueAttestationCertificate = issueAttestationCertificate;
    }

    /**
     * Sets the Dev ID Certificate Issued state.
     *
     * @param issueDevIdCertificate true if generating Certificates.
     */
    public void setIssueDevIdCertificate(final boolean issueDevIdCertificate) {
        this.issueDevIdCertificate = issueDevIdCertificate;
    }

    /**
     * Setter for the state of generating a certificate.
     *
     * @param generateOnExpiration true or false
     */
    public void setGenerateOnExpiration(final boolean generateOnExpiration) {
        this.generateOnExpiration = generateOnExpiration;
    }

    /**
     * Sets the Enable Ignore IMA state.
     *
     * @param enableIgnoreIma true if performing validation, false otherwise
     */
    public void setEnableIgnoreIma(final boolean enableIgnoreIma) {
        this.enableIgnoreIma = enableIgnoreIma;
    }

    /**
     * Sets the Enable Ignore TBoot state.
     *
     * @param enableIgnoreTboot true if performing validation, false otherwise
     */
    public void setEnableIgnoreTboot(final boolean enableIgnoreTboot) {
        this.enableIgnoreTboot = enableIgnoreTboot;
    }

    /**
     * Sets the Enable Ignore GPT state.
     *
     * @param enableIgnoreGpt true if performing validation, false otherwise
     */
    public void setEnableIgnoreGpt(final boolean enableIgnoreGpt) {
        this.enableIgnoreGpt = enableIgnoreGpt;
    }

    /**
     * Sets the Enable Ignore Os Events state.
     *
     * @param enableIgnoreOsEvt true if performing validation, false otherwise
     */
    public void setEnableIgnoreOsEvt(final boolean enableIgnoreOsEvt) {
        this.enableIgnoreOsEvt = enableIgnoreOsEvt;
    }

    /**
     * Sets the Platform Certificate Validation state.
     *
     * @param pcValidate "checked" if enabling validation, false otherwise
     */
    public void setPcValidate(final String pcValidate) {
        this.pcValidate = pcValidate;
    }

    /**
     * Sets the EC Validation state.
     *
     * @param ecValidate "checked" if enabling validation, false otherwise
     */
    public void setEcValidate(final String ecValidate) {
        this.ecValidate = ecValidate;
    }

    /**
     * Sets the PC Attribute Validation state.
     *
     * @param pcAttributeValidate "checked" if enabling validation, false otherwise
     */
    public void setPcAttributeValidate(final String pcAttributeValidate) {
        this.pcAttributeValidate = pcAttributeValidate;
    }

    /**
     * Sets the Firmware state.
     *
     * @param fmValidate "checked" if enabling validation, false otherwise
     */
    public void setFmValidate(final String fmValidate) {
        this.fmValidate = fmValidate;
    }

    /**
     * Sets the Issued Attestation Certificate state.
     *
     * @param attestationCertificateIssued "checked" if generating certificates.
     */
    public void setAttestationCertificateIssued(
            final String attestationCertificateIssued) {
        this.attestationCertificateIssued = attestationCertificateIssued;
    }

    /**
     * Sets the Issued DevID Certificate state.
     *
     * @param devIdCertificateIssued "checked" if generating certificates.
     */
    public void setDevIdCertificateIssued(final String devIdCertificateIssued) {
        this.devIdCertificateIssued = devIdCertificateIssued;
    }

    /**
     * Gets the attestation certificate issued state.
     *
     * @return the model string representation of this field.
     */
    public String getGenerationExpirationOn() {
        return generationExpirationOn;
    }

    /**
     * Sets the generation expiration state.
     *
     * @param generationExpirationOn "checked" if generating expiration is on.
     */
    public void setGenerationExpirationOn(
            final String generationExpirationOn) {
        this.generationExpirationOn = generationExpirationOn;
    }

    /**
     * Gets the attestation certificate issued state.
     *
     * @return the model string representation of this field.
     */
    public String getDevIdExpirationChecked() {
        return devIdExpirationChecked;
    }

    /**
     * Sets the generation expiration state.
     *
     * @param devIdExpirationChecked "checked" if generating expiration is on.
     */
    public void setDevIdExpirationChecked(
            final String devIdExpirationChecked) {
        this.devIdExpirationChecked = devIdExpirationChecked;
    }

    /**
     * Gets the DevID certificate issued state.
     *
     * @return the model string representation of this field.
     */
    public boolean getDevIdExpirationFlag() {
        return devIdExpirationFlag;
    }

    /**
     * Sets the generation expiration state.
     *
     * @param devIdExpirationFlag "checked" if generating expiration is on.
     */
    public void setDevIdExpirationFlag(final boolean devIdExpirationFlag) {
        this.devIdExpirationFlag = devIdExpirationFlag;
    }

    /**
     * Sets the Ignore IMA state.
     *
     * @param ignoreIma "checked" if enabling validation, false otherwise
     */
    public void setIgnoreIma(final String ignoreIma) {
        this.ignoreIma = ignoreIma;
    }

    /**
     * Sets the Ignore Tboot state.
     *
     * @param ignoretBoot "checked" if enabling validation, false otherwise
     */
    public void setIgnoretBoot(final String ignoretBoot) {
        this.ignoretBoot = ignoretBoot;
    }

    /**
     * Sets the Ignore GPT state.
     *
     * @param ignoreGpt "checked" if enabling validation, false otherwise
     */
    public void setIgnoreGpt(final String ignoreGpt) {
        this.ignoreGpt = ignoreGpt;
    }

    /**
     * Sets the Ignore Os Events state.
     *
     * @param ignoreOsEvt "checked" if enabling validation, false otherwise
     */
    public void setIgnoreOsEvt(final String ignoreOsEvt) {
        this.ignoreOsEvt = ignoreOsEvt;
    }

    /**
     * Getter for the expiration value.
     * @return the value
     */
    public String getExpirationValue() {
        return expirationValue;
    }

    /**
     * Setter for the expiration value.
     * @param expirationValue string value
     */
    public void setExpirationValue(final String expirationValue) {
        this.expirationValue = expirationValue;
    }

    /**
     * Getter for the DevID expiration value.
     * @return the value
     */
    public String getDevIdExpirationValue() {
        return devIdExpirationValue;
    }

    /**
     * Setter for the DevID expiration value.
     * @param devIdExpirationValue string value
     */
    public void setDevIdExpirationValue(final String devIdExpirationValue) {
        this.devIdExpirationValue = devIdExpirationValue;
    }

    /**
     * Getter for the expiration value.
     * @return the thresholdValue
     */
    public String getThresholdValue() {
        return thresholdValue;
    }

    /**
     * Setter for the expiration value.
     * @param thresholdValue string value
     */
    public void setThresholdValue(final String thresholdValue) {
        this.thresholdValue = thresholdValue;
    }

    /**
     * Getter for the expiration value.
     * @return the devIdThresholdValue
     */
    public String getDevIdThresholdValue() {
        return devIdThresholdValue;
    }

    /**
     * Setter for the expiration value.
     * @param devIdThresholdValue string value
     */
    public void setDevIdThresholdValue(final String devIdThresholdValue) {
        this.devIdThresholdValue = devIdThresholdValue;
    }

    @Override
    public String toString() {
        return "PolicyPageModel{"
                + "enableEcValidation=" + enableEcValidation
                + ", enablePcCertificateValidation=" + enablePcCertificateValidation
                + ", enablePcCertificateAttributeValidation="
                + enablePcCertificateAttributeValidation
                + ", enableFirmwareValidation=" + enableFirmwareValidation
                + ", issueAttestationCertificate=" + issueAttestationCertificate
                + ", issueDevIdCertificate=" + issueDevIdCertificate
                + ", generateOnExpiration=" + generateOnExpiration
                + ", numOfValidDays=" + numOfValidDays
                + ", reissueThreshold=" + reissueThreshold
                + ", enableIgnoreIma=" + enableIgnoreIma
                + ", enableIgnoreTboot=" + enableIgnoreTboot
                + ", enableIgnoreGpt=" + enableIgnoreGpt
                + ", enableIgnoreOsEvt=" + enableIgnoreOsEvt
                + ", expirationValue=" + expirationValue
                + ", thresholdValue=" + thresholdValue
                + ", devIdExpirationValue=" + devIdExpirationValue
                + ", devIdReissueThreshold=" + devIdReissueThreshold
                + ", devIdThresholdValue=" + devIdThresholdValue
                + "}";
    }
}
