package hirs.data.persist;

import com.google.common.base.Preconditions;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * A container class to group multiple related {@link SupplyChainValidation} instances
 * together.
 */
@Entity
public class SupplyChainValidationSummary extends ArchivableEntity {
    @ManyToOne
    @JoinColumn(name = "device_id")
    private final Device device;

    @Column
    @Enumerated(EnumType.STRING)
    private final AppraisalStatus.Status overallValidationResult;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER,
            targetEntity = SupplyChainValidation.class, orphanRemoval = true)
    private final Set<SupplyChainValidation> validations;

    /**
     * Default constructor necessary for Hibernate.
     */
    protected SupplyChainValidationSummary() {
        this.device = null;
        overallValidationResult = AppraisalStatus.Status.FAIL;
        validations = Collections.emptySet();
    }

    /**
     * Construct a new SupplyChainValidationSummary.
     *
     * @param device device that underwent supply chain validation
     * @param validations a Collection of Validations that should comprise this summary; not null
     */
    public SupplyChainValidationSummary(final Device device,
            final Collection<SupplyChainValidation> validations) {

        Preconditions.checkArgument(
                device != null,
                "Cannot construct a SupplyChainValidationSummary with a null device"
        );

        Preconditions.checkArgument(
                validations != null,
                "Cannot construct a SupplyChainValidationSummary with a null validations list"
        );


        this.device = device;
        this.overallValidationResult = calculateValidationResult(validations);
        this.validations = new HashSet<>(validations);

    }

    /**
     * This retrieves the device associated with the supply chain validation summaries.
     *
     * @return the validated device
     */
    public Device getDevice() {
        return device;
    }

    /**
     * @return the overall appraisal result
     */
    public AppraisalStatus.Status getOverallValidationResult() {
        return overallValidationResult;
    }

    /**
     * @return the validations that this summary contains
     */
    public Set<SupplyChainValidation> getValidations() {
        return Collections.unmodifiableSet(validations);
    }

    /**
     * A utility method that helps determine the overall appraisal result.
     *
     * @param validations the validations to evaluate
     * @return the overall appraisal result
     */
    private AppraisalStatus.Status calculateValidationResult(
            final Collection<SupplyChainValidation> validations) {
        boolean hasAnyFailures = false;

        for (SupplyChainValidation validation : validations) {
            switch (validation.getResult()) {
                // if any error, then process overall as error immediately.
                case ERROR:
                    return AppraisalStatus.Status.ERROR;
                case FAIL:
                    hasAnyFailures = true;
                    break;
                default:
                    break;
            }

        }
        // if failures, but no error, indicate failure result.
        if (hasAnyFailures) {
            return AppraisalStatus.Status.FAIL;
        }
        return AppraisalStatus.Status.PASS;
    }
}
