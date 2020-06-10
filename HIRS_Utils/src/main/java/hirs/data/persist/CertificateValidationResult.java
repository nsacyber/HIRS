package hirs.data.persist;

import hirs.data.persist.enums.CertificateValidationStatus;



/**
 * An <code>CertificateValidationResult</code> represents the result of a certificate validatin
 * operation.
 *
 */
public class CertificateValidationResult {

    /**
     * Certificate validation status.
     */
    private CertificateValidationStatus validationStatus;

    /**
     * Certificate validation result message.
     */
    private String validationResultMessage;

    /**
     * Gets the appraisal result.
     *
     * @return AppraisalStatus appraisalResult
     */
    public final CertificateValidationStatus getValidationStatus() {
        return validationStatus;
    }

    /**
     * Sets the certificate validation result.
     *
     * @param validationStatus
     *          enum representing the certificate validation status
     */
    public final void setValidationStatus(final CertificateValidationStatus validationStatus) {
        this.validationStatus = validationStatus;
    }

    /**
     * Gets the certificate validation result message.
     *
     * @return String validationResultMessage
     */
    public final String getValidationResultMessage() {
        return validationResultMessage;
    }

    /**
     * Sets the certificate validation result message.
     *
     * @param validationResultMessage
     *          String representing certificate validation message
     */
    public final void setvalidationResultMessage(final String validationResultMessage) {
        this.validationResultMessage = validationResultMessage;
    }

    /**
     * Sets the certificate validation status and result message.
     *
     * @param status        enum representing the certificate validation status
     * @param resultMessage String representing certificate validation message
     */
    public final void setCertValidationStatusAndResultMessage(final CertificateValidationStatus
           status, final String resultMessage) {
        this.validationStatus = status;
        this.validationResultMessage = resultMessage;
    }
}
