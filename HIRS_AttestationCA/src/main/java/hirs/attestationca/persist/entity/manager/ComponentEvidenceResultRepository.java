package hirs.attestationca.persist.entity.manager;

import hirs.attestationca.persist.entity.userdefined.certificate.ComponentEvidenceResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Repository interface for managing {@link ComponentEvidenceResult} entities in the database.
 */
public interface ComponentEvidenceResultRepository extends JpaRepository<ComponentEvidenceResult, UUID> {
    /**
     * Retrieves the evidence results for components, newest first.
     *
     * @param componentIds component result ids
     * @return evidence results, newest first
     */
    List<ComponentEvidenceResult> findByComponentIdInOrderByCreateTimeDesc(Collection<UUID> componentIds);
}
