package hirs.attestationca.persist.entity.userdefined;

import com.google.common.base.Preconditions;
import hirs.attestationca.persist.entity.ArchivableEntity;
import hirs.attestationca.persist.enums.AppraisalStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.data.repository.CrudRepository;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;



/**
 * A container class to group multiple related {@link SupplyChainValidation} instances
 * together.
 */
@Entity
public class SupplyChainValidationSummary extends ArchivableEntity {

    @ManyToOne
    @JoinColumn(name = "device_id")
    private final Device device;

    private static final String DEVICE_ID_FIELD = "device.id";

    @Column
    @Enumerated(EnumType.STRING)
    private final AppraisalStatus.Status overallValidationResult;

    @Column(length = RESULT_MESSAGE_LENGTH)
    private final String message;

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
        this.message = Strings.EMPTY;
    }

    /**
     * This class enables the retrieval of SupplyChainValidationSummaries by their attributes.
     */
    public static class Selector {
        private final CrudRepository<SupplyChainValidationSummary, UUID>
                supplyChainValidationSummaryCrudManager;

        private final Map<String, Object> fieldValueSelections;

        /**
         * Construct a new Selector that will use the given {@link CrudRepository} to
         * retrieve SupplyChainValidationSummaries.
         *
         * @param supplyChainValidationSummaryCrudManager the summary manager to be used to retrieve
         *                                                supply chain validation summaries
         */
        public Selector(
                final CrudRepository<SupplyChainValidationSummary, UUID>
                        supplyChainValidationSummaryCrudManager) {
            Preconditions.checkArgument(
                    supplyChainValidationSummaryCrudManager != null,
                    "supply chain validation summary manager cannot be null"
            );

            this.supplyChainValidationSummaryCrudManager = supplyChainValidationSummaryCrudManager;
            this.fieldValueSelections = new HashMap<>();
        }

        /**
         * Construct the criterion that can be used to query for supply chain validation summaries
         * matching the configuration of this Selector.
         *
         * @return a Criterion that can be used to query for supply chain validation summaries
         * matching the configuration of this instance
         */
        public Predicate[] getCriterion(final CriteriaBuilder criteriaBuilder) {
            Predicate[] predicates = new Predicate[fieldValueSelections.size()];
            CriteriaQuery<SupplyChainValidationSummary> query = criteriaBuilder.createQuery(SupplyChainValidationSummary.class);
            Root<SupplyChainValidationSummary> root = query.from(SupplyChainValidationSummary.class);

            int i = 0;
            for (Map.Entry<String, Object> fieldValueEntry : fieldValueSelections.entrySet()) {
                predicates[i++] = criteriaBuilder.equal(root.get(fieldValueEntry.getKey()), fieldValueEntry.getValue());
            }

            return predicates;
        }

        /**
         * Set a field name and value to match.
         *
         * @param name the field name to query
         * @param value the value to query
         */
        protected void setFieldValue(final String name, final Object value) {
            Object valueToAssign = value;

            Preconditions.checkArgument(
                    value != null,
                    "field value cannot be null."
            );

            if (value instanceof String) {
                Preconditions.checkArgument(
                        StringUtils.isNotEmpty((String) value),
                        "field value cannot be empty."
                );
            }

            if (value instanceof byte[]) {
                byte[] valueBytes = (byte[]) value;

                Preconditions.checkArgument(
                        ArrayUtils.isNotEmpty(valueBytes),
                        "field value cannot be empty."
                );

                valueToAssign = Arrays.copyOf(valueBytes, valueBytes.length);
            }

            fieldValueSelections.put(name, valueToAssign);
        }


        /**
         * Specify a device id that supply chain validation summaries must have to be considered
         * as matching.
         *
         * @param device the device id to query
         * @return this instance (for chaining further calls)
         */
        public Selector byDeviceId(final UUID device) {
            setFieldValue(DEVICE_ID_FIELD, device);
            return this;
        }
    }

    /**
     * Get a Selector for use in retrieving SupplyChainValidationSummary.
     *
     * @param certMan the CrudManager to be used to retrieve persisted supply chain validation
     *                summaries
     * @return a SupplyChainValidationSummary.Selector instance to use for retrieving certificates
     */
    public static SupplyChainValidationSummary.Selector select(
            final CrudRepository<SupplyChainValidationSummary, UUID> certMan) {
        return new SupplyChainValidationSummary.Selector(certMan);
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
        AppraisalStatus status = calculateValidationResult(validations);
        this.overallValidationResult = status.getAppStatus();
        this.validations = new HashSet<>(validations);
        this.message = status.getMessage();
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
     * @return the fail message if there is a failure.
     */
    public String getMessage() {
        return message;
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
    private AppraisalStatus calculateValidationResult(
            final Collection<SupplyChainValidation> validations) {
        boolean hasAnyFailures = false;
        StringBuilder failureMsg = new StringBuilder();

        for (SupplyChainValidation validation : validations) {
            switch (validation.getValidationResult()) {
                // if any error, then process overall as error immediately.
                case ERROR:
                    return new AppraisalStatus(AppraisalStatus.Status.ERROR,
                            validation.getMessage());
                case FAIL:
                    hasAnyFailures = true;
                    failureMsg.append(String.format("%s%n", validation.getValidationType()));
                    break;
                default:
                    break;
            }
        }
        // if failures, but no error, indicate failure result.
        if (hasAnyFailures) {
            return new AppraisalStatus(AppraisalStatus.Status.FAIL,
                    failureMsg.toString());
        }
        return new AppraisalStatus(AppraisalStatus.Status.PASS,
                Strings.EMPTY);
    }
}
