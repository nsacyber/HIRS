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
     * Query that retrieves a list of issued attestation certificates using the provided archive flag.
     *
     * @param archiveFlag archive flag
     * @return a list of issued attestation certificates
     */
    List<IssuedAttestationCertificate> findByArchiveFlag(boolean archiveFlag);

    /**
     * Query that retrieves a page of issued attestation certificates using the provided archive flag
     * and pageable value.
     *
     * @param archiveFlag archive flag
     * @param pageable    pageable value
     * @return a page of issued attestation certificates
     */
    Page<IssuedAttestationCertificate> findByArchiveFlag(boolean archiveFlag, Pageable pageable);

    /**
     * Query that retrieves a list of issued attestation certificates using the provided device id.
     *
     * @param deviceId uuid representation of the device id
     * @return a list of issued attestation certificates
     */
    List<IssuedAttestationCertificate> findByDeviceId(UUID deviceId);
}
