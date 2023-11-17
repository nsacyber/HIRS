package hirs.attestationca.persist.entity.manager;

import hirs.attestationca.persist.entity.userdefined.certificate.CertificateAuthorityCredential;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CACredentialRepository extends JpaRepository<CertificateAuthorityCredential, UUID> {

    List<CertificateAuthorityCredential> findByArchiveFlag(boolean archiveFlag);
    Page<CertificateAuthorityCredential> findByArchiveFlag(boolean archiveFlag, Pageable pageable);
    List<CertificateAuthorityCredential> findBySubject(String subject);
    List<CertificateAuthorityCredential> findBySubjectSorted(String subject);
    CertificateAuthorityCredential findBySubjectKeyIdentifier(byte[] subjectKeyIdentifier);
    CertificateAuthorityCredential findBySubjectKeyIdString(String subjectKeyIdString);
}
