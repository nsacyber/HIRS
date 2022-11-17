package hirs.attestationca.repository;

import hirs.attestationca.entity.certificate.Certificate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Setting up for new creation for CRUD operations.
 */
@Repository
public interface CertificateRepository extends JpaRepository<Certificate, UUID> {
}
