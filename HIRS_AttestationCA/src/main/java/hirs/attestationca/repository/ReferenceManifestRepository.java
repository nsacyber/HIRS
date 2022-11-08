package hirs.attestationca.repository;

import hirs.data.persist.ReferenceManifest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Setting up for new creation for CRUD operations.
 * @param <T> super type for ReferenceManifest child type
 */
@Repository
public interface ReferenceManifestRepository<T extends ReferenceManifest>
        extends JpaRepository<ReferenceManifest, UUID> {

    /**
     * Saves the <code>ReferenceManifest</code> in the database. This creates a new
     * database session and saves the device.
     *
     * @param rim ReferenceManifest to save
     * @return reference to saved rim
     */
    T saveRIM(T rim);
}
