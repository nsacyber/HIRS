package hirs.attestationca.persist.entity.manager;

import hirs.attestationca.persist.entity.userdefined.certificate.IssuedAttestationCertificate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IssuedCertificateRepository extends JpaRepository<IssuedAttestationCertificate, UUID> {

    @Query(value = "SELECT * FROM Certificate WHERE DTYPE='IssuedAttestationCertificate' AND archiveFlag=false", nativeQuery = true)
    @Override
    List<IssuedAttestationCertificate> findAll();
    List<IssuedAttestationCertificate> findByDeviceId(UUID deviceId);
}