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
import java.util.UUID;

/**
 * This class is used to select one or many RIMs in conjunction
 * with a {@link ReferenceManifestManager}.  To make use of this object,
 * use (some ReferenceManifest).select(ReferenceManifestManager).
 *
 * @param <ReferenceManifest> the type of referenceManifest that will be retrieved
 */
public abstract class ReferenceManifestSelector<ReferenceManifest> {
    private static final String PLATFORM_MANUFACTURER = "platformManufacturer";
    private static final String PLATFORM_MODEL = "platformModel";

    private final ReferenceManifestManager referenceManifestManager;

    private final Map<String, Object> fieldValueSelections;
    private boolean excludeArchivedRims;

    /**
     * Default Constructor.
     *
     * @param referenceManifestManager the RIM manager to be used to retrieve RIMs
     */
    public ReferenceManifestSelector(final ReferenceManifestManager referenceManifestManager) {
        this(referenceManifestManager, true);
    }

    /**
     * Standard Constructor for the Selector.
     *
     * @param referenceManifestManager the RIM manager to be used to retrieve RIMs
     * @param excludeArchivedRims true if excluding archived RIMs
     */
    public ReferenceManifestSelector(final ReferenceManifestManager referenceManifestManager,
            final boolean excludeArchivedRims) {
        Preconditions.checkArgument(
                referenceManifestManager != null,
                "reference manifest manager cannot be null"
        );

        this.referenceManifestManager = referenceManifestManager;
        this.excludeArchivedRims = excludeArchivedRims;
        this.fieldValueSelections = new HashMap<>();
    }

    /**
     * Specify the entity id that rims must have to be considered as matching.
     *
     * @param uuid the UUID to query
     * @return this instance (for chaining further calls)
     */
    public ReferenceManifestSelector byEntityId(final UUID uuid) {
        setFieldValue(Certificate.ID_FIELD, uuid);
        return this;
    }

    /**
     * Specify the platform manufacturer that rims must have to be considered
     * as matching.
     * @param manufacturer string for the manufacturer
     * @return this instance
     */
    public ReferenceManifestSelector byManufacturer(final String manufacturer) {
        setFieldValue(PLATFORM_MANUFACTURER, manufacturer);
        return this;
    }

    /**
     * Specify the platform model that rims must have to be considered
     * as matching.
     * @param model string for the model
     * @return this instance
     */
    public ReferenceManifestSelector byModel(final String model) {
        setFieldValue(PLATFORM_MODEL, model);
        return this;
    }

    /**
     * Specify the hash code of the bytes that rim must match.
     *
     * @param rimHash the hash code of the bytes to query for
     * @return this instance (for chaining further calls)
     */
    public ReferenceManifestSelector byHashCode(final int rimHash) {
        setFieldValue(hirs.data.persist.ReferenceManifest.RIM_HASH_FIELD, rimHash);
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
     * {@link hirs.data.persist.ReferenceManifest}. This method is best used
     * when selecting on a unique attribute. If the result set contains more
     * than one RIM, one is chosen arbitrarily and returned. If no matching RIMs
     * are found, this method returns null.
     *
     * @return a matching RIM or null if none is found
     */
    public hirs.data.persist.ReferenceManifest getRIM() {
        Set<hirs.data.persist.ReferenceManifest> certs = execute();
        if (certs.isEmpty()) {
            return null;
        }
        return certs.iterator().next();
    }

    /**
     * Retrieve the result set as a set of
     * {@link hirs.data.persist.ReferenceManifest}s. This method is best used
     * when selecting on non-unique attributes. ReferenceManifests are populated
     * into the set in no specific order. If no matching certificates are found,
     * the returned Set will be empty.
     *
     * @return a Set of matching RIMs, possibly empty
     */
    public Set<hirs.data.persist.ReferenceManifest> getRIMs() {
        return Collections.unmodifiableSet(new HashSet<>(execute()));
    }

    /**
     * Construct the criterion that can be used to query for rims matching the
     * configuration of this {@link ReferenceManifestSelector}.
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
    public Class<hirs.data.persist.ReferenceManifest> getReferenceManifestClass() {
        return hirs.data.persist.ReferenceManifest.class;
    }

    // construct and execute query
    private Set<hirs.data.persist.ReferenceManifest> execute() {
        Set<hirs.data.persist.ReferenceManifest> results = this.referenceManifestManager.get(this);
        return results;
    }

    /**
     * Configures the selector to query for archived and unarchived rims.
     *
     * @return the selector
     */
    public ReferenceManifestSelector<ReferenceManifest> includeArchived() {
        this.excludeArchivedRims = false;
        return this;
    }
}
