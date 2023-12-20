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

    List<EndorsementCredential> findByArchiveFlag(boolean archiveFlag);
    Page<EndorsementCredential> findByArchiveFlag(boolean archiveFlag, Pageable pageable);
    EndorsementCredential findByHolderSerialNumber(BigInteger holderSerialNumber);
    EndorsementCredential findBySerialNumber(BigInteger serialNumber);
    List<EndorsementCredential> findByDeviceId(UUID deviceId);
}
