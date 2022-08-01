package hirs.attestationca.repository;

import hirs.data.persist.ReferenceDigestValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Setting up for new creation for CRUD operations.
 */
@Repository
public interface ReferenceDigestValueRepository extends JpaRepository<ReferenceDigestValue, UUID> {
}
