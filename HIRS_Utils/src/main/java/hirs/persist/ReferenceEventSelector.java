package hirs.persist;

import com.google.common.base.Preconditions;
import hirs.data.persist.certificate.Certificate;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.criterion.Conjunction;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This class is used to select one or many TPM Events in conjunction
 * with a {@link hirs.persist.ReferenceEventManager}.  To make use of this object,
 * use (some ReferenceDigestValue).select(ReferenceEventManager).
 *
 * @param <ReferenceDigestValue> the type of DB Object that will be retrieved.
 */
public abstract class ReferenceEventSelector<ReferenceDigestValue> {
    private static final String RIM_TYPE_FIELD = "rimType";
    private static final String DIGEST_VALUE_FIELD = "digestValue";

    private final ReferenceEventManager referenceEventManager;

    private final Map<String, Object> fieldValueSelections;
    private boolean excludeArchivedValues;


    /**
     * Standard Constructor for the Selector.
     *
     * @param referenceEventManager the RIM manager to be used to retrieve RIMs
     */
    public ReferenceEventSelector(final ReferenceEventManager referenceEventManager) {
        this(referenceEventManager, true);
    }

    /**
     * Standard Constructor for the Selector.
     *
     * @param referenceEventManager the RIM manager to be used to retrieve RIMs
     * @param excludeArchivedValues true if excluding archived RIMs
     */
    public ReferenceEventSelector(final ReferenceEventManager referenceEventManager,
                                  final boolean excludeArchivedValues) {
        Preconditions.checkArgument(
                referenceEventManager != null,
                "reference event manager cannot be null"
        );

        this.referenceEventManager = referenceEventManager;
        this.excludeArchivedValues = excludeArchivedValues;
        this.fieldValueSelections = new HashMap<>();
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
                String.format("field value (%s) cannot be null.", name)
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
                    String.format("field value (%s) cannot be empty.", name)
            );

            valueToAssign = Arrays.copyOf(valueBytes, valueBytes.length);
        }

        fieldValueSelections.put(name, valueToAssign);
    }

    /**
     * Retrieve the result set as a single
     * {@link hirs.data.persist.ReferenceDigestValue}. This method is best used
     * when selecting on a unique attribute. If the result set contains more
     * than one RIM, one is chosen arbitrarily and returned. If no matching RIMs
     * are found, this method returns null.
     *
     * @return a matching ReferenceDigestValue or null if none is found
     */
    public hirs.data.persist.ReferenceDigestValue getDigestValue() {
        Set<hirs.data.persist.ReferenceDigestValue> events = getDigestValues();
        if (events.isEmpty()) {
            return null;
        }
        return events.iterator().next();
    }

    /**
     * Retrieve the result set as a set of
     * {@link hirs.data.persist.ReferenceDigestValue}s. This method is best used
     * when selecting on non-unique attributes. ReferenceManifests are populated
     * into the set in no specific order. If no matching certificates are found,
     * the returned Set will be empty.
     *
     * @return a Set of matching ReferenceDigestValues, possibly empty
     */
    public Set<hirs.data.persist.ReferenceDigestValue> getDigestValues() {
        return Collections.unmodifiableSet(new HashSet<hirs.data.persist.ReferenceDigestValue>(
                this.referenceEventManager.getEventList()));
    }
    /**
     * Construct the criterion that can be used to query for rims matching the
     * configuration of this {@link ReferenceEventSelector}.
     *
     * @return a Criterion that can be used to query for rims matching the
     * configuration of this instance
     */
    Criterion getCriterion() {
        Conjunction conj = new Conjunction();

        for (Map.Entry<String, Object> fieldValueEntry : fieldValueSelections.entrySet()) {
            conj.add(Restrictions.eq(fieldValueEntry.getKey(), fieldValueEntry.getValue()));
        }

        if (this.excludeArchivedValues) {
            conj.add(Restrictions.isNull(Certificate.ARCHIVE_FIELD));
        }

        return conj;
    }

    /**
     * Configures the selector to query for archived and unarchived rims.
     *
     * @return the selector
     */
    public ReferenceEventSelector includeArchived() {
        this.excludeArchivedValues = false;
        return this;
    }
}
