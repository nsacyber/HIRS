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
     * @param dType   dtype
     * @return a list of certificates
     */
    @Query(value = "SELECT * FROM Certificate where subject = ?1 AND DTYPE = ?2", nativeQuery = true)
    List<Certificate> findBySubject(String subject, String dType);

    /**
     * Query that retrieves a sorted list of certificates using the provided subject and dtype.
     *
     * @param subjectSorted subject
     * @param dType         dtype
     * @return a list of sorted certificates
     */
    @Query(value = "SELECT * FROM Certificate where subjectSorted = ?1 AND  DTYPE = ?2", nativeQuery = true)
    List<Certificate> findBySubjectSorted(String subjectSorted, String dType);

    /**
     * Query that retrieves a list of certificates using the provided dtype.
     *
     * @param dType dtype
     * @return a list of certificates
     */
    @Query(value = "SELECT * FROM Certificate where DTYPE = ?1", nativeQuery = true)
    List<Certificate> findByType(String dType);

    /**
     * Query that retrieves a list of certificates using the provided serial number and dtype.
     *
     * @param serialNumber serial number
     * @param dType        dtype
     * @return a certificate
     */
    @Query(value = "SELECT * FROM Certificate where serialNumber = ?1 AND DTYPE = ?2", nativeQuery = true)
    Certificate findBySerialNumber(BigInteger serialNumber, String dType);

    /**
     * Query that retrieves a list of platform credentials using the provided board serial number
     * and a dtype of "Platform Credential".
     *
     * @param boardSerialNumber board serial number
     * @return a list of platform credentials
     */
    @Query(value = "SELECT * FROM Certificate where platformSerial = ?1 AND DTYPE = 'PlatformCredential'",
            nativeQuery = true)
    List<PlatformCredential> byBoardSerialNumber(String boardSerialNumber);

    /**
     * Query that retrieves a platform credential using the provided holder serial number
     * and a dtype of "Platform Credential".
     *
     * @param holderSerialNumber holder serial number
     * @return platform credential
     */
    @Query(value = "SELECT * FROM Certificate where holderSerialNumber = ?1 AND DTYPE = 'PlatformCredential'",
            nativeQuery = true)
    PlatformCredential getPcByHolderSerialNumber(BigInteger holderSerialNumber);

    /**
     * Query that retrieves a list of platform credentials using the provided holder serial number
     * and a dtype of "Platform Credential".
     *
     * @param holderSerialNumber holder serial numberz
     * @return a list of platform credentials
     */
    @Query(value = "SELECT * FROM Certificate where holderSerialNumber = ?1 AND DTYPE = 'PlatformCredential'",
            nativeQuery = true)
    List<PlatformCredential> getByHolderSerialNumber(BigInteger holderSerialNumber);

    /**
     * Query that retrieves a certificate using the provided certificate hash and dtype.
     *
     * @param certificateHash integer certificate hash
     * @param dType           dtype
     * @return a certificate
     */
    @Query(value = "SELECT * FROM Certificate where certificateHash = ?1 AND DTYPE = ?2", nativeQuery = true)
    Certificate findByCertificateHash(int certificateHash, String dType);

    /**
     * Query that retrieves an endorssement credential using the provided public key modulus hex value.
     *
     * @param publicKeyModulusHexValue public key modulus hex value
     * @return an endorsement credential
     */
    EndorsementCredential findByPublicKeyModulusHexValue(String publicKeyModulusHexValue);

    /**
     * Query that retrieves an issued attestation certificate using the provided device id.
     *
     * @param deviceId uuid representation of the device id
     * @return an issued attestation certificate
     */
    IssuedAttestationCertificate findByDeviceId(UUID deviceId);

    /**
     * Query that retrieves a list of issued attestation certificates using the provided device id,
     * ldevID value and sort value.
     *
     * @param deviceId device id
     * @param ldevID is it a LDevId
     * @param sort     sort
     * @return a list of issued attestation certificates
     */
    List<IssuedAttestationCertificate> findByDeviceIdAndLdevID(UUID deviceId, boolean ldevID, Sort sort);

    /**
     * Query that retrieves a certificates using the provided certificate hash.
     *
     * @param certificateHash integer certificate hash
     * @return a certificate
     */
    Certificate findByCertificateHash(int certificateHash);
}

