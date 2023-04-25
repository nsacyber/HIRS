package hirs.attestationca.persist.entity.manager;

import hirs.attestationca.persist.entity.userdefined.SupplyChainValidation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SupplyChainValidationRepository extends JpaRepository<SupplyChainValidation, UUID> {
}
