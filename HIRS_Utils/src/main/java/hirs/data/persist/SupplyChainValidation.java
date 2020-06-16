package hirs.data.persist;

import com.google.common.base.Preconditions;
import hirs.data.persist.certificate.Certificate;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import java.util.Collections;
import java.util.List;

/**
 * Stores results of a single element of the supply chain validation process.
 */
@Entity
public class SupplyChainValidation extends ArchivableEntity {
    /**
     * Used to indicate which type of validation a result is related to.
     */
    public enum ValidationType {
        /**
         * Validation of an endorsement credential.
         */
        ENDORSEMENT_CREDENTIAL,

        /**
         * Validation of a platform credential and also delta platform credentials from spec 1.1.
         */
        PLATFORM_CREDENTIAL,

        /**
         * Validation of a platform credential's attributes.
         */
        PLATFORM_CREDENTIAL_ATTRIBUTES,

        /**
         * Validation of the device firmware.
         */
        FIRMWARE
    }

    @Column
    private final ValidationType validationType;

    @Column
    private final AppraisalStatus.Status validationResult;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "CertificatesUsedToValidate",
            joinColumns = { @JoinColumn(name = "validation_id", nullable = false) })
    private final List<Certificate> certificatesUsed;

    @Column(length = MAX_MESSAGE_LENGTH)
    private final String message;

    /**
     * Default constructor necessary for Hibernate.
     */
    public SupplyChainValidation() {
        this.validationType = null;
        this.validationResult = AppraisalStatus.Status.ERROR;
        this.certificatesUsed = Collections.emptyList();
        this.message = null;
    }

    /**
     * Construct a new SupplyChainValidation instance.
     *
     * @param validationType the type of validation this instance will represent; not null
     * @param validationResult whether the validation was successful or not
     * @param certificatesUsed certificates used, if any, in the validation process; not null
     * @param message a related information or error message; may be null
     */
    public SupplyChainValidation(final ValidationType validationType,
                                 final AppraisalStatus.Status validationResult,
                                 final List<Certificate> certificatesUsed,
                                 final String message) {
        Preconditions.checkArgument(
                validationType != null,
                "Cannot construct a SupplyChainValidation with a null ValidationType"
        );

        Preconditions.checkArgument(
                certificatesUsed != null,
                "Cannot construct a SupplyChainValidation with a null certificatesUsed"
        );

        this.validationType = validationType;
        this.validationResult = validationResult;
        this.certificatesUsed = certificatesUsed;
        this.message = message;
    }

    /**
     * @return the type of validation that this object represents
     */
    public ValidationType getValidationType() {
        return validationType;
    }

    /**
     * @return the appraisal result
     */
    public AppraisalStatus.Status getResult() {
        return validationResult;
    }

    /**
     * @return certificates used in the process of performing the validation; may be empty
     */
    public List<Certificate> getCertificatesUsed() {
        return Collections.unmodifiableList(certificatesUsed);
    }

    /**
     * @return a related informational or error message encountered while performing the validation
     */
    public String getMessage() {
        return message;
    }
}
