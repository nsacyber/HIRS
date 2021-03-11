package hirs.persist;

import com.google.common.base.Preconditions;
import hirs.data.persist.ReferenceDigestRecord;
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
 * This class is used to select one or many RIMs in conjunction
 * with a {@link ReferenceDigestManager}.  To make use of this object,
 * use (some ReferenceManifest).select(ReferenceManifestManager).
 *
 * @param <T> the type of Reference Integrity Manifest that will be retrieved.
 */
public abstract class ReferenceDigestSelector<T extends ReferenceDigestRecord> {
    /**
     * String representing the database field for the manufacturer.
     */
    public static final String PLATFORM_MANUFACTURER = "manufacturer";
    /**
     * String representing the database field for the manufacturer id.
     */
    public static final String PLATFORM_MODEL = "model";

    private final ReferenceDigestManager referenceDigestManager;
    private final Class<T> referenceTypeClass;

    private final Map<String, Object> fieldValueSelections;
    private boolean excludeArchivedRims;

    /**
     * Default Constructor.
     *
     * @param referenceDigestManager the RIM manager to be used to retrieve RIMs
     * @param referenceTypeClass the type of Reference Manifest to process.
     */
    public ReferenceDigestSelector(final ReferenceDigestManager referenceDigestManager,
                                   final Class<T> referenceTypeClass) {
        this(referenceDigestManager, referenceTypeClass, true);
    }

    /**
     * Standard Constructor for the Selector.
     *
     * @param referenceDigestManager the RIM manager to be used to retrieve RIMs
     * @param referenceTypeClass the type of Reference Manifest to process.
     * @param excludeArchivedRims true if excluding archived RIMs
     */
    public ReferenceDigestSelector(final ReferenceDigestManager referenceDigestManager,
                                   final Class<T> referenceTypeClass,
                                   final boolean excludeArchivedRims) {
        Preconditions.checkArgument(
                referenceDigestManager != null,
                "reference manifest manager cannot be null"
        );

        Preconditions.checkArgument(
                referenceTypeClass != null,
                "type cannot be null"
        );

        this.referenceDigestManager = referenceDigestManager;
        this.referenceTypeClass = referenceTypeClass;
        this.excludeArchivedRims = excludeArchivedRims;
        this.fieldValueSelections = new HashMap<>();
    }

    /**
     * Specify the entity id that rims must have to be considered as matching.
     *
     * @param manufacturer the UUID to query
     * @return this instance (for chaining further calls)
     */
    public ReferenceDigestSelector<T> byManufacturer(final String manufacturer) {
        setFieldValue(PLATFORM_MANUFACTURER, manufacturer);
        return this;
    }

    /**
     * Specify the hash code of the bytes that rim must match.
     *
     * @param model the hash code of the bytes to query for
     * @return this instance (for chaining further calls)
     */
    public ReferenceDigestSelector<T> byModel(final String model) {
        setFieldValue(PLATFORM_MODEL, model);
        return this;
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
     * {@link hirs.data.persist.ReferenceDigestRecord}. This method is best used
     * when selecting on a unique attribute. If the result set contains more
     * than one RIM, one is chosen arbitrarily and returned. If no matching RIMs
     * are found, this method returns null.
     *
     * @return a matching digest record or null if none is found
     */
    public T getDigestRecord() {
        Set<T> rims = execute();
        if (rims.isEmpty()) {
            return null;
        }
        return rims.iterator().next();
    }

    /**
     * Retrieve the result set as a set of
     * {@link hirs.data.persist.ReferenceDigestRecord}s. This method is best used
     * when selecting on non-unique attributes. ReferenceManifests are populated
     * into the set in no specific order. If no matching certificates are found,
     * the returned Set will be empty.
     *
     * @return a Set of matching RIMs, possibly empty
     */
    public Set<T> getDigestRecords() {
        return Collections.unmodifiableSet(new HashSet<>(execute()));
    }

    /**
     * Construct the criterion that can be used to query for rims matching the
     * configuration of this {@link hirs.persist.ReferenceDigestSelector}.
     *
     * @return a Criterion that can be used to query for rims matching the
     * configuration of this instance
     */
    Criterion getCriterion() {
        Conjunction conj = new Conjunction();

        for (Map.Entry<String, Object> fieldValueEntry : fieldValueSelections.entrySet()) {
            conj.add(Restrictions.eq(fieldValueEntry.getKey(), fieldValueEntry.getValue()));
        }

        if (this.excludeArchivedRims) {
            conj.add(Restrictions.isNull(Certificate.ARCHIVE_FIELD));
        }

        return conj;
    }

    /**
     * @return the rim class that this instance will query
     */
    public Class<T> getReferenceDigestClass() {
        return this.referenceTypeClass;
    }

    // construct and execute query
    private Set<T> execute() {
        Set<T> results = this.referenceDigestManager.get(this);
        return results;
    }

    /**
     * Configures the selector to query for archived and unarchived rims.
     *
     * @return the selector
     */
    public ReferenceDigestSelector<T> includeArchived() {
        this.excludeArchivedRims = false;
        return this;
    }
}
