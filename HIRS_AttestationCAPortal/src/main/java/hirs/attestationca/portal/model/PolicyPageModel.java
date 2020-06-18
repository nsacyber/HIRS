package hirs.attestationca.portal.model;

import hirs.data.persist.SupplyChainPolicy;

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
    private boolean enableIgnoreIma;
    private boolean enableIgnoreTboot;

    // Variables to get policy settings from page
    private String pcValidate;
    private String pcAttributeValidate;
    private String ecValidate;
    private String fmValidate;
    private String ignoreIma;
    private String ignoretBoot;

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
        this.enableIgnoreIma = policy.isIgnoreImaEnabled();
        this.enableIgnoreTboot = policy.isIgnoreTbootEnabled();
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

    @Override
    public String toString() {
        return "PolicyPageModel{"
                + "enableEcValidation=" + enableEcValidation
                + ", enablePcCertificateValidation=" + enablePcCertificateValidation
                + ", enablePcCertificateAttributeValidation="
                + enablePcCertificateAttributeValidation
                + ", enableFirmwareValidation=" + enableFirmwareValidation + '}';
    }
}
