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

    /**
     * Query that retrieves a certificate using the provided uuid.
     *
     * @param uuid uuid
     * @return a certificate
     */
    @Query(value = "SELECT * FROM Certificate where id = ?1", nativeQuery = true)
    Certificate getCertificate(UUID uuid);

    /**
     * Query that retrieves a list of certificates using the provided subject and dtype.
     *
     * @param subject subject
     * @param dType   d type
     * @return a list of certificates
     */
    @Query(value = "SELECT * FROM Certificate where subject = ?1 AND DTYPE = ?2", nativeQuery = true)
    List<Certificate> findBySubject(String subject, String dType);

    /**
     * Query that retrieves a sorted list of certificates using the provided subject and dtype.
     *
     * @param subjectSorted
     * @param dType
     * @return a list of sorted certificates
     */
    @Query(value = "SELECT * FROM Certificate where subjectSorted = ?1 AND  DTYPE = ?2", nativeQuery = true)
    List<Certificate> findBySubjectSorted(String subjectSorted, String dType);

    /**
     * Query that retrieves a
     *
     * @param dType
     * @return
     */
    @Query(value = "SELECT * FROM Certificate where DTYPE = ?1", nativeQuery = true)
    List<Certificate> findByType(String dType);

    /**
     * Query that retrieves a
     *
     * @param serialNumber
     * @param dType
     * @return
     */
    @Query(value = "SELECT * FROM Certificate where serialNumber = ?1 AND DTYPE = ?2", nativeQuery = true)
    Certificate findBySerialNumber(BigInteger serialNumber, String dType);

    /**
     * Query that retrieves a
     *
     * @param boardSerialNumber
     * @return
     */
    @Query(value = "SELECT * FROM Certificate where platformSerial = ?1 AND DTYPE = 'PlatformCredential'", nativeQuery = true)
    List<PlatformCredential> byBoardSerialNumber(String boardSerialNumber);

    /**
     * Query that retrieves a
     *
     * @param holderSerialNumber
     * @return
     */
    @Query(value = "SELECT * FROM Certificate where holderSerialNumber = ?1 AND DTYPE = 'PlatformCredential'", nativeQuery = true)
    PlatformCredential getPcByHolderSerialNumber(BigInteger holderSerialNumber);

    /**
     * Query that retrieves a
     *
     * @param holderSerialNumber
     * @return
     */
    @Query(value = "SELECT * FROM Certificate where holderSerialNumber = ?1 AND DTYPE = 'PlatformCredential'", nativeQuery = true)
    List<PlatformCredential> getByHolderSerialNumber(BigInteger holderSerialNumber);

    /**
     * Query that retrieves a
     *
     * @param certificateHash
     * @param dType
     * @return
     */
    @Query(value = "SELECT * FROM Certificate where certificateHash = ?1 AND DTYPE = ?2", nativeQuery = true)
    Certificate findByCertificateHash(int certificateHash, String dType);

    /**
     * Query that retrieves a
     *
     * @param publicKeyModulusHexValue
     * @return
     */
    EndorsementCredential findByPublicKeyModulusHexValue(String publicKeyModulusHexValue);

    /**
     * Query that retrieves a
     *
     * @param deviceId
     * @return
     */
    IssuedAttestationCertificate findByDeviceId(UUID deviceId);

    /**
     * Query that retrieves a
     *
     * @param deviceId
     * @param isLDevID
     * @param sort
     * @return
     */
    List<IssuedAttestationCertificate> findByDeviceIdAndIsLDevID(UUID deviceId, boolean isLDevID, Sort sort);

    /**
     * Query that retrieves a
     *
     * @param certificateHash
     * @return
     */
    Certificate findByCertificateHash(int certificateHash);
}
