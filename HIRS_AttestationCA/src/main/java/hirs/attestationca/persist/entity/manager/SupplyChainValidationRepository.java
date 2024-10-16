package hirs.attestationca.persist.entity.manager;

import hirs.attestationca.persist.entity.userdefined.SupplyChainValidation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SupplyChainValidationRepository extends JpaRepository<SupplyChainValidation, UUID> {
    /**
     * Query that retrieves a list of supply chain validation using the provided validate type.
     *
     * @param validateType validate type
     * @return a list of supply chain validation
     */
    List<SupplyChainValidation> findByValidationType(String validateType);

    /**
     * Query that retrieves a list of supply chain validation using the provided validation result.
     *
     * @param validationResult validation result
     * @return a list of supply chain validation
     */
    List<SupplyChainValidation> findByValidationResult(String validationResult);
}
