package hirs.attestationca.persist.entity.manager;

import hirs.attestationca.persist.entity.userdefined.certificate.EndorsementCredential;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigInteger;
import java.util.List;
import java.util.UUID;

/**
 * Repository interface for managing {@link EndorsementCredential} entities in the database.
 *
 * <p>
 * The {@link EndorsementCredentialRepository} interface extends {@link JpaRepository} to provide basic CRUD operations,
 * including save, find, delete, and query methods. Custom query methods can be defined
 * using Spring Data JPA's query method naming conventions or with the Query annotation.
 * </p>
 */
@Repository
public interface EndorsementCredentialRepository extends JpaRepository<EndorsementCredential, UUID> {

    /**
     * Query that retrieves a count of {@link EndorsementCredential} objects in the database filtered by the
     * provided archive flag.
     *
     * @param archiveFlag archive flag
     * @return a count of {@link EndorsementCredential} objects
     */
    long countByArchiveFlag(boolean archiveFlag);

    /**
     * Query that retrieves a page of {@link EndorsementCredential} objects filtered by the specified archive flag.
     *
     * @param archiveFlag archive flag
     * @param pageable    pageable value
     * @return a page of {@link EndorsementCredential} objects
     */
    Page<EndorsementCredential> findByArchiveFlag(boolean archiveFlag, Pageable pageable);

    /**
     * Query that retrieves an {@link EndorsementCredential} object using the provided serial number.
     *
     * @param serialNumber big integer representation of the serial number
     * @return an {@link EndorsementCredential} object
     */
    EndorsementCredential findBySerialNumber(BigInteger serialNumber);

    /**
     * Query that retrieves a list of {@link EndorsementCredential} objects using the provided device id.
     *
     * @param deviceId uuid representation of the device id
     * @return a list of {@link EndorsementCredential} objects
     */
    List<EndorsementCredential> findByDeviceId(UUID deviceId);
}
