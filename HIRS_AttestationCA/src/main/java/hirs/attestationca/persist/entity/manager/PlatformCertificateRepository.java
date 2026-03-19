package hirs.attestationca.persist.entity.manager;

import hirs.attestationca.persist.entity.userdefined.certificate.PlatformCredential;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface for managing {@link PlatformCredential} entities in the database.
 *
 * <p>
 * The {@link PlatformCertificateRepository} interface extends {@link JpaRepository} to provide basic CRUD operations,
 * including save, find, delete, and query methods. Custom query methods can be defined
 * using Spring Data JPA's query method naming conventions or with the Query annotation.
 * </p>
 */
@Repository
public interface PlatformCertificateRepository extends JpaRepository<PlatformCredential, UUID> {

    /**
     * Query that retrieves a count of {@link PlatformCredential} objects in the database filtered by the provided archive flag.
     *
     * @param archiveFlag archive flag
     * @return a count of {@link PlatformCredential} objects
     */
    long countByArchiveFlag(boolean archiveFlag);

    /**
     * Query that retrieves a page of {@link PlatformCredential} objects filtered by the specified archive flag.
     *
     * @param archiveFlag archive flag
     * @param pageable    pageable
     * @return a page of {@link PlatformCredential} objects
     */
    Page<PlatformCredential> findByArchiveFlag(boolean archiveFlag, Pageable pageable);

    /**
     * Query that retrieves a list of {@link PlatformCredential} objects using the provided device id.
     *
     * @param deviceId uuid representation of the device id
     * @return a list of {@link PlatformCredential} objects
     */
    List<PlatformCredential> findByDeviceId(UUID deviceId);
}
