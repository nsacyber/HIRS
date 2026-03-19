package hirs.attestationca.persist.entity.manager;

import hirs.attestationca.persist.entity.userdefined.certificate.CertificateAuthorityCredential;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface for managing {@link CertificateAuthorityCredential} entities in the database.
 *
 * <p>
 * The {@link CACredentialRepository} interface extends {@link JpaRepository} to provide basic CRUD operations,
 * including save, find, delete, and query methods. Custom query methods can be defined
 * using Spring Data JPA's query method naming conventions or with the Query annotation.
 * </p>
 */
@Repository
public interface CACredentialRepository extends JpaRepository<CertificateAuthorityCredential, UUID> {

    /**
     * Query that retrieves a count of {@link CertificateAuthorityCredential} objects in the database filtered by the provided
     * archive flag.
     *
     * @param archiveFlag archive flag
     * @return a count of {@link CertificateAuthorityCredential} objects
     */
    long countByArchiveFlag(boolean archiveFlag);

    /**
     * Query that retrieves a page of {@link CertificateAuthorityCredential} objects filtered by the specified archive flag.
     *
     * @param archiveFlag archive flag
     * @param pageable    pageable
     * @return a page of {@link CertificateAuthorityCredential} objects
     */
    Page<CertificateAuthorityCredential> findByArchiveFlag(boolean archiveFlag, Pageable pageable);

    /**
     * Query that retrieves a list of {@link CertificateAuthorityCredential} objects using the provided subject.
     *
     * @param subject subject
     * @return a list of {@link CertificateAuthorityCredential} objects
     */
    List<CertificateAuthorityCredential> findBySubject(String subject);

    /**
     * Query that retrieves a sorted list of {@link CertificateAuthorityCredential} objects using the provided subject.
     *
     * @param subject subject
     * @return a sorted list of {@link CertificateAuthorityCredential} objects
     */
    List<CertificateAuthorityCredential> findBySubjectSorted(String subject);

    /**
     * Query that retrieves a list of {@link CertificateAuthorityCredential} objects using the provided subject
     * and the provided archive flag.
     *
     * @param subject     subject
     * @param archiveFlag archive flag
     * @return a list of {@link CertificateAuthorityCredential} objects
     */
    List<CertificateAuthorityCredential> findBySubjectAndArchiveFlag(String subject, boolean archiveFlag);

    /**
     * Query that retrieves a sorted list of {@link CertificateAuthorityCredential} objects using the provided subject
     * and the provided archive flag.
     *
     * @param subject     subject
     * @param archiveFlag archive flag
     * @return a sorted list of {@link CertificateAuthorityCredential} objects
     */
    List<CertificateAuthorityCredential> findBySubjectSortedAndArchiveFlag(String subject,
                                                                           boolean archiveFlag);

    /**
     * Query that retrieves a {@link CertificateAuthorityCredential} object using the provided subject key identifier.
     *
     * @param subjectKeyIdentifier byte array representation of the subject key identifier
     * @return a {@link CertificateAuthorityCredential} object
     */
    CertificateAuthorityCredential findBySubjectKeyIdentifier(byte[] subjectKeyIdentifier);

    /**
     * Query that retrieves a {@link CertificateAuthorityCredential} object using the provided subject key identifier
     * and the provided archive flag.
     *
     * @param subjectKeyIdString string representation of the subject key id
     * @param archiveFlag        archive flag
     * @return a {@link CertificateAuthorityCredential} object
     */
    CertificateAuthorityCredential findBySubjectKeyIdStringAndArchiveFlag(String subjectKeyIdString,
                                                                          boolean archiveFlag);
}
