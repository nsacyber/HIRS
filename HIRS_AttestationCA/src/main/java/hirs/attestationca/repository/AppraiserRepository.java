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

    Appraiser findByName(String name);
}
