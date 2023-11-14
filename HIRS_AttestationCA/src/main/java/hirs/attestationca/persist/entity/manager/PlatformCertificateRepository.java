package hirs.attestationca.persist.entity.manager;

import hirs.attestationca.persist.entity.userdefined.certificate.PlatformCredential;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PlatformCertificateRepository extends JpaRepository<PlatformCredential, UUID> {

    List<PlatformCredential> findByArchiveFlag(boolean archiveFlag);
    Page<PlatformCredential> findByArchiveFlag(boolean archiveFlag, Pageable pageable);
    List<PlatformCredential> findByDeviceId(UUID deviceId);
}
