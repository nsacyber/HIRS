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
     * Query that retrieves a
     *
     * @param archiveFlag
     * @return
     */
    List<IDevIDCertificate> findByArchiveFlag(boolean archiveFlag);

    /**
     * Query that retrieves a
     *
     * @param archiveFlag
     * @param pageable
     * @return
     */
    Page<IDevIDCertificate> findByArchiveFlag(boolean archiveFlag, Pageable pageable);
    /*List<IDevIDCertificate> findBySubject(String subject);
    List<IDevIDCertificate> findBySubjectSorted(String subject);
    List<IDevIDCertificate> findBySubjectAndArchiveFlag(String subject, boolean archiveFlag);
    List<IDevIDCertificate> findBySubjectSortedAndArchiveFlag(String subject, boolean archiveFlag);
    IDevIDCertificate findBySubjectKeyIdentifier(byte[] subjectKeyIdentifier);
    IDevIDCertificate findBySubjectKeyIdStringAndArchiveFlag(String subjectKeyIdString, boolean archiveFlag);
     */
}