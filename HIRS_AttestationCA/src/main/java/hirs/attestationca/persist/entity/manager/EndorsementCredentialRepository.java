package hirs.attestationca.persist.entity.manager;

import hirs.attestationca.persist.entity.userdefined.certificate.EndorsementCredential;
import hirs.attestationca.persist.entity.userdefined.certificate.PlatformCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigInteger;
import java.util.List;
import java.util.UUID;

@Repository
public interface EndorsementCredentialRepository extends JpaRepository<EndorsementCredential, UUID> {

    @Override
    List<EndorsementCredential> findAll();
    EndorsementCredential findByHolderSerialNumber(BigInteger holderSerialNumber);
    List<EndorsementCredential> findByDeviceId(UUID deviceId);
}
