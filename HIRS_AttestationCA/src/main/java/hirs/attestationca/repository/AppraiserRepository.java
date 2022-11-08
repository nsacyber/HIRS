package hirs.attestationca.repository;

import hirs.appraiser.Appraiser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Setting up for new creation for CRUD operations.
 */
@Repository
public interface AppraiserRepository extends JpaRepository<Appraiser, UUID> {

    /**
     * Finds a <code>Appraiser</code>.
     * If the <code>Appraiser</code> is successfully retrieved then a reference to
     * it is returned.
     *
     * @param name the name to search by
     * @return reference to saved appraiser
     */
    Appraiser findByName(String name);
}
