package hirs.attestationca.persist.entity.manager;

import hirs.attestationca.persist.entity.userdefined.SupplyChainValidation;
import hirs.attestationca.persist.enums.AppraisalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
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
    /**
     * Query that retrieves a list of supply chain validation using the provided validate type.
     *
     * @param validationType string representation of the validate type
     * @return a list of supply chain validation
     */
    List<SupplyChainValidation> findByValidationType(SupplyChainValidation.ValidationType validationType);

    /**
     * Query that retrieves a list of supply chain validation using the provided validation result.
     *
     * @param validationResult string representation of the validation result
     * @return a list of supply chain validation
     */
    List<SupplyChainValidation> findByValidationResult(AppraisalStatus.Status validationResult);
}
