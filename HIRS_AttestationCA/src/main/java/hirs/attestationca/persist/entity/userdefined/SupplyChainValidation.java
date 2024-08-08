package hirs.attestationca.persist.entity.userdefined;

import com.google.common.base.Preconditions;
import hirs.attestationca.persist.entity.ArchivableEntity;
import hirs.attestationca.persist.entity.userdefined.rim.BaseReferenceManifest;
import hirs.attestationca.persist.enums.AppraisalStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import lombok.Getter;

import java.util.ArrayList;
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

    @Getter
    @Column
    private final ValidationType validationType;

    @Getter
    @Column
    private final AppraisalStatus.Status validationResult;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "CertificatesUsedToValidate",
            joinColumns = { @JoinColumn(name = "validation_id", nullable = false) })
    private final List<Certificate> certificatesUsed;

    @Getter
    @Column(length = RESULT_MESSAGE_LENGTH)
    private final String message;

    @Getter
    @Column
    private String rimId;

    /**
     * Default constructor necessary for Hibernate.
     */
    public SupplyChainValidation() {
        this.validationType = null;
        this.validationResult = AppraisalStatus.Status.ERROR;
        this.certificatesUsed = Collections.emptyList();
        this.message = null;
        this.rimId = "";
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
                                 final List<ArchivableEntity> certificatesUsed,
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
        this.certificatesUsed = new ArrayList<>();
        this.rimId = "";
        for (ArchivableEntity ae : certificatesUsed) {
            if (ae instanceof ReferenceManifest rm) {
                this.rimId = rm.getId().toString();
                break;
            } else if (ae instanceof Certificate) {
                this.certificatesUsed.add((Certificate) ae);
            }
        }

        this.message = message;
    }

    /**
     * @return certificates used in the process of performing the validation; may be empty
     */
    public List<Certificate> getCertificatesUsed() {
        return Collections.unmodifiableList(certificatesUsed);
    }
}
