package hirs.attestationca.persist.entity.manager;

import hirs.attestationca.persist.entity.userdefined.certificate.EndorsementCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigInteger;
import java.util.List;
import java.util.UUID;

@Repository
public interface EndorsementCredentialRepository extends JpaRepository<EndorsementCredential, UUID> {

    @Query(value = "SELECT * FROM Certificate where DTYPE='EndorsementCredential'", nativeQuery = true)
    @Override
    List<EndorsementCredential> findAll();
    @Query(value = "SELECT * FROM Certificate where holderSerialNumber = ?1 AND DTYPE = 'EndorsementCredential'", nativeQuery = true)
    EndorsementCredential getEcByHolderSerialNumber(BigInteger holderSerialNumber);
}
