
package hirs.persist;

import hirs.data.persist.ReferenceManifest;

/**
 * This class facilitates the persistence of {@link ReferenceManifest}s
 * including storage, retrieval, and deletion.
 */
public interface ReferenceManifestManager {

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
     * Delete the given RIM.
     *
     * @param referenceManifest the RIM to delete
     * @return true if the deletion succeeded, false otherwise.
     */
    boolean delete(ReferenceManifest referenceManifest);
}
