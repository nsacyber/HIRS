package hirs.attestationca.persist.entity.manager;

import hirs.attestationca.persist.entity.userdefined.certificate.IssuedAttestationCertificate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IssuedCertificateRepository extends JpaRepository<IssuedAttestationCertificate, UUID> {

    /**
     * Query that retrieves a
     *
     * @param archiveFlag
     * @return
     */
    List<IssuedAttestationCertificate> findByArchiveFlag(boolean archiveFlag);

    /**
     * Query that retrieves a
     *
     * @param archiveFlag
     * @param pageable
     * @return
     */
    Page<IssuedAttestationCertificate> findByArchiveFlag(boolean archiveFlag, Pageable pageable);

    /**
     * @param deviceId
     * @return
     */
    List<IssuedAttestationCertificate> findByDeviceId(UUID deviceId);
}