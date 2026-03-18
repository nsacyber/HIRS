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

    /**
     * Query that retrieves a count of platform credentials in the database filtered by the provided archive flag.
     *
     * @param archiveFlag archive flag
     * @return a count of platform credentials
     */
    long countByArchiveFlag(boolean archiveFlag);

    /**
     * Query that retrieves a page of platform credentials filtered by the specified archive flag,
     * sorted by create time in descending order.
     *
     * @param archiveFlag archive flag
     * @param pageable    pageable
     * @return a page of platform credentials
     */
    Page<PlatformCredential> findByArchiveFlagOrderByCreateTimeDesc(boolean archiveFlag, Pageable pageable);

    /**
     * Query that retrieves a list of platform credentials using the provided device id.
     *
     * @param deviceId uuid representation of the device id
     * @return a list of platform credentials
     */
    List<PlatformCredential> findByDeviceId(UUID deviceId);
}
