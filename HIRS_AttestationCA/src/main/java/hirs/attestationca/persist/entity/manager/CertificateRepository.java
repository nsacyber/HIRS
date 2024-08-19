package hirs.attestationca.persist.entity.manager;

import hirs.attestationca.persist.entity.userdefined.Certificate;
import hirs.attestationca.persist.entity.userdefined.certificate.EndorsementCredential;
import hirs.attestationca.persist.entity.userdefined.certificate.IssuedAttestationCertificate;
import hirs.attestationca.persist.entity.userdefined.certificate.PlatformCredential;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigInteger;
import java.util.List;
import java.util.UUID;

@Repository
public interface CertificateRepository extends JpaRepository<Certificate, UUID> {

    @Query(value = "SELECT * FROM Certificate where id = ?1", nativeQuery = true)
    Certificate getCertificate(UUID uuid);
    @Query(value = "SELECT * FROM Certificate where subject = ?1 AND DTYPE = ?2", nativeQuery = true)
    List<Certificate> findBySubject(String subject, String dType);
    @Query(value = "SELECT * FROM Certificate where subjectSorted = ?1 AND  DTYPE = ?2", nativeQuery = true)
    List<Certificate> findBySubjectSorted(String subjectSorted, String dType);
    @Query(value = "SELECT * FROM Certificate where DTYPE = ?1", nativeQuery = true)
    List<Certificate> findByType(String dType);
    @Query(value = "SELECT * FROM Certificate where serialNumber = ?1 AND DTYPE = ?2", nativeQuery = true)
    Certificate findBySerialNumber(BigInteger serialNumber, String dType);
    @Query(value = "SELECT * FROM Certificate where platformSerial = ?1 AND DTYPE = 'PlatformCredential'", nativeQuery = true)
    List<PlatformCredential> byBoardSerialNumber(String boardSerialNumber);
    @Query(value = "SELECT * FROM Certificate where holderSerialNumber = ?1 AND DTYPE = 'PlatformCredential'", nativeQuery = true)
    PlatformCredential getPcByHolderSerialNumber(BigInteger holderSerialNumber);
    @Query(value = "SELECT * FROM Certificate where holderSerialNumber = ?1 AND DTYPE = 'PlatformCredential'", nativeQuery = true)
    List<PlatformCredential> getByHolderSerialNumber(BigInteger holderSerialNumber);
    @Query(value = "SELECT * FROM Certificate where certificateHash = ?1 AND DTYPE = ?2", nativeQuery = true)
    Certificate findByCertificateHash(int certificateHash, String dType);
    EndorsementCredential findByPublicKeyModulusHexValue(String publicKeyModulusHexValue);
    IssuedAttestationCertificate findByDeviceId(UUID deviceId);
    List<IssuedAttestationCertificate> findByDeviceIdAndIsLDevID(UUID deviceId, boolean isLDevID, Sort sort);
    Certificate findByCertificateHash(int certificateHash);
}
