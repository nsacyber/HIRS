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

    // Variables to get policy settings from page
    private String pcValidate;
    private String pcAttributeValidate;
    private String ecValidate;

    /**
     * Constructor. Sets fields from policy.
     *
     * @param policy The supply chain policy
     */
    public PolicyPageModel(final SupplyChainPolicy policy) {
        this.enableEcValidation = policy.isEcValidationEnabled();
        this.enablePcCertificateValidation = policy.isPcValidationEnabled();
        this.enablePcCertificateAttributeValidation = policy.isPcAttributeValidationEnabled();
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

    @Override
    public String toString() {
        return "PolicyPageModel{"
                + "enableEcValidation=" + enableEcValidation
                + ", enablePcCertificateValidation=" + enablePcCertificateValidation
                + ", enablePcCertificateAttributeValidation="
                + enablePcCertificateAttributeValidation + '}';
    }
}
