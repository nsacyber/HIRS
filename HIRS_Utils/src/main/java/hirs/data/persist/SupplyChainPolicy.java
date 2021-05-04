package hirs.data.persist;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;

/**
 * Class represents Supply Chain policy. Supply Chain Policy identifies the methods in
 * SupplyChainValidator that should be used in order to validate a supply chain.
 * By default, the policy does not enable any validations.
 */
@Entity
public class SupplyChainPolicy extends Policy {
    /**
     * Name of the default Supply Chain Policy.
     */
    public static final String DEFAULT_POLICY = "Default Supply Chain Policy";
    /**
     * Number of days in 10 years.
     */
    public static final String TEN_YEARS = "3651";
    /**
     * Number of days in 1 year.
     */
    public static final String YEAR = "365";

    @Column(nullable = false)
    private boolean enableEcValidation = false;

    @Column(nullable = false)
    private boolean enablePcValidation = false;

    @Column(nullable = false)
    private boolean enablePcAttributeValidation = false;

    @Column(nullable = false)
    private boolean enableFirmwareValidation = false;

    @Column(nullable = false)
    private boolean enableUtcValidation = false;

    @Column(nullable = false)
    private boolean enableExpiredCertificateValidation = false;

    @Column(nullable = false)
    private boolean replaceEC = false;

    @Column(nullable = false)
    private boolean issueAttestationCertificate = true;

    @Column(nullable = false)
    private String validityDays = TEN_YEARS;

    @Column(nullable = false)
    private String reissueThreshold = YEAR;

    @Column(nullable = false)
    private boolean generateOnExpiration = false;

    @Embedded
    private PCRPolicy pcrPolicy = new PCRPolicy();

    /**
     * Constructor used to initialize SupplyChainPolicy object.
     *
     * @param name
     *        A name used to uniquely identify and reference the Supply Chain policy.
     */
    public SupplyChainPolicy(final String name) {
        super(name);
    }

    /**
     * Constructor used to initialize SupplyChainPolicy object.
     *
     * @param name
     *        A name used to uniquely identify and reference the supply chain policy.
     * @param description
     *        Optional description of the policy that can be added by the user
     */
    public SupplyChainPolicy(final String name, final String description) {
        super(name, description);
    }

    /**
     * Default constructor necessary for Hibernate.
     */
    protected SupplyChainPolicy() {
        super();
    }

    /**
     * Returns the setting for whether or not to validate endorsement credentials.
     *
     * @return setting for whether or not to validate endorsement credentials
     */
    public boolean isEcValidationEnabled() {
        return enableEcValidation;
    }

    /**
     * Set whether or not to validate endorsement credentials.
     *
     * @param enableEcValidation whether or not to validate endorsement credentials.
     */
    public void setEcValidationEnabled(final boolean enableEcValidation) {
        this.enableEcValidation = enableEcValidation;
    }

    /**
     * Returns whether or not to validate platform credentials by checking the signatures
     * associated with its certificate chain. This does not enable validation of the PC
     * attributes.
     *
     * @return whether or not to validate platform credential certificates.
     */
    public boolean isPcValidationEnabled() {
        return enablePcValidation;
    }

    /**
     * Sets whether or not to validate platform credentials by checking the signatures
     * associated with its certificate chain. This does not enable validation of the PC
     * attributes.
     *
     * @param enablePcValidation whether or not to validate platform credential
     *                                      certificates.
     */
    public void setPcValidationEnabled(final boolean enablePcValidation) {
        this.enablePcValidation = enablePcValidation;
    }

    /**
     * Returns whether or not to validate the attributes in a platform credential, such as the
     * serial number of the motherboard, a pointer to the EC of the installed TPM, and the
     * chassis number.
     *
     * @return whether or not to validate the attributes in a platform credential.
     */
    public boolean isPcAttributeValidationEnabled() {
        return enablePcAttributeValidation;
    }

    /**
     * Sets  whether or not to validate the attributes in a platform credential, such as the
     * serial number of the motherboard, a pointer to the EC of the installed TPM, and the
     * chassis number.
     *
     * @param enablePcAttributeValidation whether or not to validate the attributes in a platform
     *                                    credential.
     */
    public void setPcAttributeValidationEnabled(final boolean enablePcAttributeValidation) {
        this.enablePcAttributeValidation = enablePcAttributeValidation;
    }

    /**
     * Returns whether or not to validate the firmware on the device.
     *
     * @return whether or not to validate the firmware.
     */
    public boolean isFirmwareValidationEnabled() {
        return enableFirmwareValidation;
    }

    /**
     * Sets  whether or not to validate the firmware on the device.
     *
     * @param enableFirmwareValidation whether or not to validate the firmware.
     */
    public void setFirmwareValidationEnabled(final boolean enableFirmwareValidation) {
        this.enableFirmwareValidation = enableFirmwareValidation;
    }

    /**
     * Returns whether or not to validate the ignore ima on the device.
     *
     * @return whether or not to validate the ignore imag.
     */
    public boolean isIgnoreImaEnabled() {
        return this.pcrPolicy.isEnableIgnoreIma();
    }

    /**
     * Sets whether or not validate the ignore ima on the device.
     * @param enableIgnoreIma whether or not to validate the ignore ima
     */
    public void setIgnoreImaEnabled(final boolean enableIgnoreIma) {
        this.pcrPolicy.setEnableIgnoreIma(enableIgnoreIma);
    }

    /**
     * Returns whether or not to validate the ignore tboot on the device.
     *
     * @return whether or not to validate the ignore tboot
     */
    public boolean isIgnoreTbootEnabled() {
        return this.pcrPolicy.isEnableIgnoretBoot();
    }

    /**
     * Sets whether or not validate the ignore tboot on the device.
     * @param enableIgnoreTboot whether or not to validate the ignore tboot
     */
    public void setIgnoreTbootEnabled(final boolean enableIgnoreTboot) {
        this.pcrPolicy.setEnableIgnoretBoot(enableIgnoreTboot);
    }

    /**
     * Returns whether or not to validate the ignore GPT on the device.
     *
     * @return whether or not to validate the ignore GPT
     */
    public boolean isIgnoreGptEnabled() {
        return this.pcrPolicy.isEnableIgnoreGpt();
    }

    /**
     * Sets whether or not validate the ignore GPT on the device.
     * @param enableIgnoreGpt whether or not to validate the ignore GPT
     */
    public void setIgnoreGptEnabled(final boolean enableIgnoreGpt) {
        this.pcrPolicy.setEnableIgnoreGpt(enableIgnoreGpt);
    }

    /**
     * Returns whether or not to allow expired credentials and certificates to be considered
     * valid if their supply chain is otherwise verified.
     *
     * @return whether or not to allow expired credentials and certificates to be considered
     * valid
     */
    public boolean isExpiredCertificateValidationEnabled() {
        return enableExpiredCertificateValidation;
    }

    /**
     * Sets whether or not to allow expired credentials and certificates to be considered
     * valid if their supply chain is otherwise verified.
     *
     * @param enableExpiredCertificateValidation whether or not to allow expired credentials and
     *                                           certificates to be considered
     * valid
     */
    public void setExpiredCertificateValidationEnabled(
            final boolean enableExpiredCertificateValidation) {
        this.enableExpiredCertificateValidation = enableExpiredCertificateValidation;
    }

    /**
     * Returns whether or not to automatically replace endorsement credentials with ACA generated
     * ones.
     *
     * @return whether or not to automatically replace endorsement credentials with ACA generated
     * ones.
     */
    public boolean isReplaceEC() {
        return replaceEC;
    }

    /**
     * Sets whether or not to automatically replace endorsement credentials with ACA generated
     * ones.
     *
     * @param replaceEC whether or not to automatically replace endorsement credentials with ACA
     *                  generated ones.
     */
    public void setReplaceEC(final boolean replaceEC) {
        this.replaceEC = replaceEC;
    }

    /**
     * Getter for the current PCR Policy.
     * @return the PCR Policy
     */
    public PCRPolicy getPcrPolicy() {
        return pcrPolicy;
    }

    /**
     * Setter to update the current PCR Policy.
     * @param pcrPolicy to apply
     */
    public void setPcrPolicy(final PCRPolicy pcrPolicy) {
        this.pcrPolicy = pcrPolicy;
    }

    /**
     * Returns whether or not to generate an Attestation Issued Certificate.
     * @return current state for generation.
     */
    public boolean isIssueAttestationCertificate() {
        return issueAttestationCertificate;
    }

    /**
     * Sets whether or not to generate an Attestation Issued Certificate.
     * @param issueAttestationCertificate the flag for generation.
     */
    public void setIssueAttestationCertificate(final boolean issueAttestationCertificate) {
        this.issueAttestationCertificate = issueAttestationCertificate;
    }

    /**
     * Getter for the number of days for the certificates validity.
     * @return number of days
     */
    public String getValidityDays() {
        return validityDays;
    }

    /**
     * Setter for the number of days for validity.
     * @param validityDays validity.
     */
    public void setValidityDays(final String validityDays) {
        this.validityDays = validityDays;
    }

    /**
     * Getter for the number of days before the expiration to reissue
     * a certificate.
     * @return number of days
     */
    public String getReissueThreshold() {
        return reissueThreshold;
    }

    /**
     * Setter for the number of days before the expiration to reissue
     * a certificate.
     * @param reissueThreshold validity.
     */
    public void setReissueThreshold(final String reissueThreshold) {
        this.reissueThreshold = reissueThreshold;
    }

    /**
     * Getter for the state of when to generate a certificate.
     * @return true or false
     */
    public boolean isGenerateOnExpiration() {
        return generateOnExpiration;
    }

    /**
     * Setter for the state of when to generate a certificate.
     * @param generateOnExpiration sets true or false
     */
    public void setGenerateOnExpiration(final boolean generateOnExpiration) {
        this.generateOnExpiration = generateOnExpiration;
    }
}
