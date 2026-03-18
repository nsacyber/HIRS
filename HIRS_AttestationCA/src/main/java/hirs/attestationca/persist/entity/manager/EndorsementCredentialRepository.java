package hirs.attestationca.persist.entity.manager;

import hirs.attestationca.persist.entity.userdefined.certificate.EndorsementCredential;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigInteger;
import java.util.List;
import java.util.UUID;

@Repository
public interface EndorsementCredentialRepository extends JpaRepository<EndorsementCredential, UUID> {

    /**
     * Query that retrieves a count of endorsement credentials in the database filtered by the provided archive flag.
     *
     * @param archiveFlag archive flag
     * @return a count of endorsement credentials
     */
    long countByArchiveFlag(boolean archiveFlag);

    /**
     * Query that retrieves a page of endorsement credentials filtered by the specified archive flag,
     * sorted by create time in descending order.
     *
     * @param archiveFlag archive flag
     * @param pageable    pageable value
     * @return a page of endorsement credentials
     */
    Page<EndorsementCredential> findByArchiveFlagOrderByCreateTimeDesc(boolean archiveFlag, Pageable pageable);

    /**
     * Query that retrieves an endorsement credential using the provided serial number.
     *
     * @param serialNumber big integer representation of the serial number
     * @return an endorsement credential
     */
    EndorsementCredential findBySerialNumber(BigInteger serialNumber);

    /**
     * Query that retrieves a list of endorsement credentials using the provided device id.
     *
     * @param deviceId uuid representation of the device id
     * @return an endorsement credential
     */
    List<EndorsementCredential> findByDeviceId(UUID deviceId);
}
