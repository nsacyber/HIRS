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
     * Query that retrieves a list of endorsement credentials using the provided archive flag.
     *
     * @param archiveFlag archive flag
     * @return a list of endorsement credentials
     */
    List<EndorsementCredential> findByArchiveFlag(boolean archiveFlag);

    /**
     * Query that retrieves a page of endorsement credentials using provided archive flag and pageable value.
     *
     * @param archiveFlag archive flag
     * @param pageable    pageable value
     * @return a page of endorsement credentials
     */
    Page<EndorsementCredential> findByArchiveFlag(boolean archiveFlag, Pageable pageable);

    /**
     * Query that retrieves an endorsement credential using the provided holder serial number.
     *
     * @param holderSerialNumber big integer representation of the holder serial number
     * @return an endorsement credential
     */
    EndorsementCredential findByHolderSerialNumber(BigInteger holderSerialNumber);

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
