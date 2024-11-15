package hirs.attestationca.persist.entity.manager;

import hirs.attestationca.persist.entity.userdefined.certificate.IDevIDCertificate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IDevIDCertificateRepository extends JpaRepository<IDevIDCertificate, UUID> {

    /**
     * Query that retrieves a list of IDevId certificates using the provided archive flag.
     *
     * @param archiveFlag archive flag
     * @return a list of IDevId certificates
     */
    List<IDevIDCertificate> findByArchiveFlag(boolean archiveFlag);

    /**
     * Query that retrieves a page of IDevId certificates using the provided archive flag and pageable value.
     *
     * @param archiveFlag archive flag
     * @param pageable    pageable value
     * @return a page of IDevId certificates
     */
    Page<IDevIDCertificate> findByArchiveFlag(boolean archiveFlag, Pageable pageable);


    //    /**
//     * Query that retrieves a list of IDevId certificates using the provided subject.
//     *
//     * @param subject string representation of the subject
//     * @return a list of IDevId certificates
//     */
//    List<IDevIDCertificate> findBySubject(String subject);
//
//    /**
//     * Query that retrieves a sorted list of IDevId certificates using the provided subject.
//     *
//     * @param subject string representation of the subject
//     * @return a sorted list of IDevId certificates
//     */
//    List<IDevIDCertificate> findBySubjectSorted(String subject);
//
//    /**
//     * Query that retrieves a list of IDevId certificates using the provided subject and archive flag.
//     *
//     * @param subject     string representation of the subject
//     * @param archiveFlag archive flag
//     * @return a list of IDevId certificates
//     */
//    List<IDevIDCertificate> findBySubjectAndArchiveFlag(String subject, boolean archiveFlag);
//
//    /**
//     * Query that retrieves a sorted list of IDevId certificates using the provided subject
//     * and archive flag.
//     *
//     * @param subject     string representation of the subject
//     * @param archiveFlag archive flag
//     * @return a sorted list of IDevId certificates
//     */
//    List<IDevIDCertificate> findBySubjectSortedAndArchiveFlag(String subject, boolean archiveFlag);
//
//    /**
//     * Query that retrieves an IDevId certificate using the provided subject key identifier.
//     *
//     * @param subjectKeyIdentifier byte representation of the subject key identifier
//     * @return an IDevId certificate
//     */
//    IDevIDCertificate findBySubjectKeyIdentifier(byte[] subjectKeyIdentifier);
//
//    /**
//     * Query that retrieves an IDevId certificate using the provided subject key and archive flag.
//     *
//     * @param subjectKeyIdString string representation of the subject key id
//     * @param archiveFlag        archive flag
//     * @return an IDevId certificate
//     */
//      IDevIDCertificate findBySubjectKeyIdStringAndArchiveFlag(String subjectKeyIdString,
//      boolean archiveFlag);
}
