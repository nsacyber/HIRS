package hirs.persist.service;

import hirs.data.persist.ReferenceManifest;
import hirs.persist.OrderedQuery;
import hirs.persist.ReferenceManifestSelector;

import java.util.Set;
import java.util.UUID;

/**
 * A <code>ReferenceManifestService</code> manages <code>ReferenceManifest</code>s. A
 * <code>ReferenceManifestService</code> is used to store and manage reference manifests. It has
 * support for the basic create, read, update, and delete methods.
 */
public interface ReferenceManifestService extends OrderedQuery<ReferenceManifest>,
        DefaultService<ReferenceManifest> {

    /**
     * Saves the <code>ReferenceManifest</code> in the database. This creates a new
     * database session and saves the device.
     *
     * @param rim ReferenceManifest to save
     * @return reference to saved rim
     */
    ReferenceManifest saveRIM(ReferenceManifest rim);

    /**
     * Updates a <code>ReferenceManifest</code>. This updates the database entries to
     * reflect the new values that should be set.
     *
     * @param rim ReferenceManifest object to save
     * @param uuid UUID for the database object
     * @return a ReferenceManifest object
     */
    ReferenceManifest updateReferenceManifest(ReferenceManifest rim, UUID uuid);

    /**
     * Deletes the <code>ReferenceManifest</code> in the database. This creates a new
     * database session and saves the device.
     *
     * @param rim ReferenceManifest to delete
     * @return reference to deleted rim
     */
    void deleteRIM(ReferenceManifest rim);

    /**
     * Retrieve Reference Manifest according to the given
     * {@link hirs.persist.ReferenceManifestSelector}.
     *
     * @param <T> the type of certificate that will be retrieved
     * @param referenceManifestSelector a {@link hirs.persist.ReferenceManifestSelector}
     *                                 to use for querying
     * @return a Set of matching Certificates, which may be empty
     */
    <T extends ReferenceManifest> Set<T> getReferenceManifest(
            ReferenceManifestSelector referenceManifestSelector);
}
