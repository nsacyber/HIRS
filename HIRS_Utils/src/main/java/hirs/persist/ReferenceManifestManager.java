
package hirs.persist;

import hirs.data.persist.ReferenceManifest;

import java.util.Set;

/**
 * This class facilitates the persistence of {@link ReferenceManifest}s
 * including storage, retrieval, and deletion.
 */
public interface ReferenceManifestManager extends OrderedListQuerier<ReferenceManifest> {

    /**
     * Persists a new Reference Manifest.
     *
     * @param referenceManifest the ReferenceManifest
     * @return the persisted ReferenceManifest
     */
    ReferenceManifest save(ReferenceManifest referenceManifest);

    /**
     * Updates an existing ReferenceManifest.
     * @param referenceManifest the rim to update
     */
    void update(ReferenceManifest referenceManifest);

    /**
     * Retrieve RIMs according to the given {@link ReferenceManifestSelector}.
     *
     * @param <T> the type of reference manifest that will be retrieved
     *  @param referenceManifestSelector a {@link ReferenceManifestSelector} to use for querying
     * @return a Set of matching RIMs, which may be empty
     */
    <T extends ReferenceManifest> Set<T> get(ReferenceManifestSelector referenceManifestSelector);

    /**
     * Delete the given RIM.
     *
     * @param referenceManifest the RIM to delete
     * @return true if the deletion succeeded, false otherwise.
     */
    boolean delete(ReferenceManifest referenceManifest);

    /**
     * Remove a ReferenceManifest from the database.
     *
     * @param referenceManifest the referenceManifest to delete
     * @return true if deletion was successful, false otherwise
     */
    boolean deleteReferenceManifest(ReferenceManifest referenceManifest);
}
