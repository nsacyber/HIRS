package hirs.attestationca.persist.entity.manager;

import hirs.attestationca.persist.entity.userdefined.certificate.IDevIDCertificate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository interface for managing {@link IDevIDCertificate} entities in the database.
 *
 * <p>
 * The {@link IDevIDCertificateRepository} interface extends {@link JpaRepository} to provide basic CRUD operations,
 * including save, find, delete, and query methods. Custom query methods can be defined
 * using Spring Data JPA's query method naming conventions or with the Query annotation.
 * </p>
 */
@Repository
public interface IDevIDCertificateRepository extends JpaRepository<IDevIDCertificate, UUID> {

    /**
     * Query that retrieves a count of {@link IDevIDCertificate} objects in the database filtered by the provided
     * archive flag.
     *
     * @param archiveFlag archive flag
     * @return a count of {@link IDevIDCertificate} objects
     */
    long countByArchiveFlag(boolean archiveFlag);

    /**
     * Query that retrieves a page of {@link IDevIDCertificate} objects filtered by the specified archive flag.
     *
     * @param archiveFlag archive flag
     * @param pageable    pageable value
     * @return a page of {@link IDevIDCertificate} objects
     */
    Page<IDevIDCertificate> findByArchiveFlag(boolean archiveFlag, Pageable pageable);
}
