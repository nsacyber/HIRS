package hirs.attestationca.persist.service.selector;

import com.google.common.base.Preconditions;
import hirs.attestationca.persist.entity.userdefined.Certificate;
import hirs.attestationca.persist.entity.userdefined.ReferenceManifest;
import hirs.attestationca.persist.service.ReferenceManifestService;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * This class is used to select one or many RIMs in conjunction
 * with a {@link ReferenceManifestService}.  To make use of this object,
 * use (some ReferenceManifest).select(ReferenceManifestManager).
 *
 * @param <T> the type of Reference Integrity Manifest that will be retrieved.
 */
public abstract class ReferenceManifestSelector<T extends ReferenceManifest> {
    /**
     * String representing the database field for the manufacturer.
     */
    public static final String PLATFORM_MANUFACTURER = "platformManufacturer";
    /**
     * String representing the database field for the manufacturer id.
     */
    public static final String PLATFORM_MANUFACTURER_ID = "platformManufacturerId";
    /**
     * String representing the database field for the model.
     */
    public static final String PLATFORM_MODEL = "platformModel";
    /**
     * String representing the database field for the filename.
     */
    public static final String RIM_FILENAME_FIELD = "fileName";
    private static final String RIM_TYPE_FIELD = "rimType";

    private final ReferenceManifestService referenceManifestManager;
    private final Class<T> referenceTypeClass;

    private final Map<String, Object> fieldValueSelections;
    private boolean excludeArchivedRims;

    /**
     * Default Constructor.
     *
     * @param referenceManifestManager the RIM manager to be used to retrieve RIMs
     * @param referenceTypeClass the type of Reference Manifest to process.
     */
    public ReferenceManifestSelector(final ReferenceManifestService referenceManifestManager,
                                     final Class<T> referenceTypeClass) {
        this(referenceManifestManager, referenceTypeClass, true);
    }

    /**
     * Standard Constructor for the Selector.
     *
     * @param referenceManifestManager the RIM manager to be used to retrieve RIMs
     * @param referenceTypeClass the type of Reference Manifest to process.
     * @param excludeArchivedRims true if excluding archived RIMs
     */
    public ReferenceManifestSelector(final ReferenceManifestService referenceManifestManager,
                                     final Class<T> referenceTypeClass,
                                     final boolean excludeArchivedRims) {
        Preconditions.checkArgument(
                referenceManifestManager != null,
                "reference manifest manager cannot be null"
        );

        Preconditions.checkArgument(
                referenceTypeClass != null,
                "type cannot be null"
        );

        this.referenceManifestManager = referenceManifestManager;
        this.referenceTypeClass = referenceTypeClass;
        this.excludeArchivedRims = excludeArchivedRims;
        this.fieldValueSelections = new HashMap<>();
    }

    /**
     * Specify the entity id that rims must have to be considered as matching.
     *
     * @param uuid the UUID to query
     * @return this instance (for chaining further calls)
     */
    public ReferenceManifestSelector<T> byEntityId(final UUID uuid) {
        setFieldValue(Certificate.ID_FIELD, uuid);
        return this;
    }

    /**
     * Specify the file name of the object to grab.
     * @param fileName the name of the file associated with the rim
     * @return instance of the manifest in relation to the filename.
     */
    public ReferenceManifestSelector<T> byFileName(final String fileName) {
        setFieldValue(RIM_FILENAME_FIELD, fileName);
        return this;
    }

    /**
     * Specify the RIM Type to match.
     * @param rimType the type of rim
     * @return this instance
     */
    public ReferenceManifestSelector<T> byRimType(final String rimType) {
        setFieldValue(RIM_TYPE_FIELD, rimType);
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
     * {@link ReferenceManifest}. This method is best used
     * when selecting on a unique attribute. If the result set contains more
     * than one RIM, one is chosen arbitrarily and returned. If no matching RIMs
     * are found, this method returns null.
     *
     * @return a matching RIM or null if none is found
     */
    public T getRIM() {
        Set<T> rims = execute();
        if (rims.isEmpty()) {
            return null;
        }
        return rims.iterator().next();
    }

    /**
     * Retrieve the result set as a set of
     * {@link ReferenceManifest}s. This method is best used
     * when selecting on non-unique attributes. ReferenceManifests are populated
     * into the set in no specific order. If no matching certificates are found,
     * the returned Set will be empty.
     *
     * @return a Set of matching RIMs, possibly empty
     */
    public Set<T> getRIMs() {
        return Set.copyOf(execute());
    }

    /**
     * Construct the criterion that can be used to query for rims matching the
     * configuration of this {@link ReferenceManifestSelector}.
     *
     * @return a Criterion that can be used to query for rims matching the
     * configuration of this instance
     */
    Predicate[] getCriterion(final CriteriaBuilder criteriaBuilder) {
        Predicate[] predicates = new Predicate[fieldValueSelections.size()];
        CriteriaQuery<T> query = criteriaBuilder.createQuery(getReferenceManifestClass());
        Root<T> root = query.from(getReferenceManifestClass());

        int i = 0;
        for (Map.Entry<String, Object> fieldValueEntry : fieldValueSelections.entrySet()) {
            predicates[i++] = criteriaBuilder.equal(root.get(fieldValueEntry.getKey()), fieldValueEntry.getValue());
        }

        if (this.excludeArchivedRims) {
            predicates[i] = criteriaBuilder.isNull(root.get(Certificate.ARCHIVE_FIELD));
        }

        return predicates;
    }

    /**
     * @return the rim class that this instance will query
     */
    public Class<T> getReferenceManifestClass() {
        return this.referenceTypeClass;
    }

    // construct and execute query
    private Set<T> execute() {
        Set<T> results = this.referenceManifestManager.get(this);
        return results;
    }

    /**
     * Configures the selector to query for archived and unarchived rims.
     *
     * @return the selector
     */
    public ReferenceManifestSelector<T> includeArchived() {
        this.excludeArchivedRims = false;
        return this;
    }
}
