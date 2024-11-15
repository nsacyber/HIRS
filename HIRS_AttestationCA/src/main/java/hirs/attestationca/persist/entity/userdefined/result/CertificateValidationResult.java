package hirs.attestationca.persist.entity.userdefined.result;

import lombok.Getter;
import lombok.Setter;

/**
 * An <code>CertificateValidationResult</code> represents the result of a certificate validation
 * operation.
 */
@Getter
@Setter
public class CertificateValidationResult {
    private CertificateValidationStatus validationStatus;
    private String validationResultMessage;

    /**
     * Sets the certificate validation status and result message.
     *
     * @param status        enum representing the certificate validation status
     * @param resultMessage String representing certificate validation message
     */
    public final void setCertValidationStatusAndResultMessage(
            final CertificateValidationStatus status,
            final String resultMessage) {
        this.validationStatus = status;
        this.validationResultMessage = resultMessage;
    }


    /**
     * Enum used to represent certificate validation status.
     */
    public enum CertificateValidationStatus {

        /**
         * Represents a passing validation.
         */
        PASS,

        /**
         * Represents a failed validation.
         */
        FAIL,

        /**
         * Represents a validation error.
         */
        ERROR
    }
}
