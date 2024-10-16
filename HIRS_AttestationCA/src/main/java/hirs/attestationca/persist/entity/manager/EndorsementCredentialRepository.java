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
     * Query that retrieves a
     *
     * @param archiveFlag
     * @return
     */
    List<EndorsementCredential> findByArchiveFlag(boolean archiveFlag);

    /**
     * Query that retrieves a
     *
     * @param archiveFlag
     * @param pageable
     * @return
     */
    Page<EndorsementCredential> findByArchiveFlag(boolean archiveFlag, Pageable pageable);

    /**
     * Query that retrieves a
     *
     * @param holderSerialNumber
     * @return
     */
    EndorsementCredential findByHolderSerialNumber(BigInteger holderSerialNumber);

    /**
     * Query that retrieves a
     *
     * @param serialNumber
     * @return
     */
    EndorsementCredential findBySerialNumber(BigInteger serialNumber);

    /**
     * Query that retrieves a
     *
     * @param deviceId
     * @return
     */
    List<EndorsementCredential> findByDeviceId(UUID deviceId);
}
