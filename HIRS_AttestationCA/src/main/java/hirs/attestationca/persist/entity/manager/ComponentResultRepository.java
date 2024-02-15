package hirs.attestationca.persist.entity.manager;

import hirs.attestationca.persist.entity.userdefined.certificate.ComponentResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigInteger;
import java.util.List;
import java.util.UUID;

@Repository
public interface ComponentResultRepository extends JpaRepository<ComponentResult, UUID> {

    List<ComponentResult> findByCertificateSerialNumber(BigInteger certificateSerialNumber);
    List<ComponentResult> findByCertificateSerialNumberAndMismatched(BigInteger certificateSerialNumber, boolean mismatched);
}
