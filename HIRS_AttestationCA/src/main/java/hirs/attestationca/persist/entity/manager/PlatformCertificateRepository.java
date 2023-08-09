package hirs.attestationca.persist.entity.manager;

import hirs.attestationca.persist.entity.userdefined.certificate.PlatformCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PlatformCertificateRepository extends JpaRepository<PlatformCredential, UUID> {

    @Override
    List<PlatformCredential> findAll();
    List<PlatformCredential> findByDeviceId(UUID deviceId);
}
