package hirs.persist.service;

import hirs.data.persist.ReferenceManifest;
import hirs.persist.OrderedQuery;

import java.util.UUID;

/**
 * A <code>ReferenceManifestService</code> manages <code>ReferenceManifest</code>s. A
 * <code>ReferenceManifestService</code> is used to store and manage reference manifests. It has
 * support for the basic create, read, update, and delete methods.
 */
public interface ReferenceManifestService extends OrderedQuery<ReferenceManifest> {

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
}
