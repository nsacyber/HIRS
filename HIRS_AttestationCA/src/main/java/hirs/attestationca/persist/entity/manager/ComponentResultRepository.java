package hirs.attestationca.persist.entity.manager;

import hirs.attestationca.persist.entity.userdefined.certificate.ComponentResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ComponentResultRepository extends JpaRepository<ComponentResult, UUID> {

    @Query(value = "SELECT * FROM ComponentResult where certificateId = ?1", nativeQuery = true)
    List<ComponentResult> findByCertificateId(UUID certificateId);
}
