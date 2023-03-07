package hirs.attestationca.persist.entity.manager;

import hirs.attestationca.persist.entity.userdefined.Certificate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CertificateRepository extends JpaRepository<Certificate, UUID> {
}
