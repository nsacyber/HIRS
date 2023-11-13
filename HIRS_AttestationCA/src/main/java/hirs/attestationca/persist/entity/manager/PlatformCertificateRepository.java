package hirs.attestationca.persist.entity.manager;

import hirs.attestationca.persist.entity.userdefined.certificate.PlatformCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PlatformCertificateRepository extends JpaRepository<PlatformCredential, UUID> {

    @Query(value = "SELECT * FROM Certificate WHERE DTYPE='PlatformCredential' AND archiveFlag=false", nativeQuery = true)
    @Override
    List<PlatformCredential> findAll();
    List<PlatformCredential> findByDeviceId(UUID deviceId);
}
