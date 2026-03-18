package hirs.attestationca.persist.entity.manager;

import hirs.attestationca.persist.entity.userdefined.certificate.IDevIDCertificate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface IDevIDCertificateRepository extends JpaRepository<IDevIDCertificate, UUID> {

    /**
     * Query that retrieves a count of IDevId certificates in the database filtered by the provided archive flag.
     *
     * @param archiveFlag archive flag
     * @return a count of idevid certificates
     */
    long countByArchiveFlag(boolean archiveFlag);

    /**
     * Query that retrieves a page of IDevId certificates filtered by the specified archive flag.
     *
     * @param archiveFlag archive flag
     * @param pageable    pageable value
     * @return a page of IDevId certificates
     */
    Page<IDevIDCertificate> findByArchiveFlag(boolean archiveFlag, Pageable pageable);
}
