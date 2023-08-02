package hirs.attestationca.persist.entity.manager;

import hirs.attestationca.persist.entity.userdefined.SupplyChainValidation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SupplyChainValidationRepository extends JpaRepository<SupplyChainValidation, UUID> {
    List<SupplyChainValidation> findByValidationType(String validateType);
    List<SupplyChainValidation> findByValidationResult(String validationResult);
}
