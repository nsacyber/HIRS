package hirs.attestationca.persist.entity.manager;

import hirs.attestationca.persist.entity.userdefined.SupplyChainValidation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository interface for managing {@link SupplyChainValidation} entities in the database.
 *
 * <p>
 * The {@link SupplyChainValidationRepository} interface extends {@link JpaRepository} to provide basic CRUD
 * operations, including save, find, delete, and query methods. Custom query methods can be defined
 * using Spring Data JPA's query method naming conventions or with the Query annotation.
 * </p>
 */
@Repository
public interface SupplyChainValidationRepository extends JpaRepository<SupplyChainValidation, UUID> {
}
