package hirs.attestationca.persist.entity.manager;

import hirs.attestationca.persist.entity.userdefined.certificate.attributes.ComponentAttributeResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ComponentAttributeRepository extends JpaRepository<ComponentAttributeResult, UUID> {
    List<ComponentAttributeResult> findByComponentId(UUID componentId);
}
