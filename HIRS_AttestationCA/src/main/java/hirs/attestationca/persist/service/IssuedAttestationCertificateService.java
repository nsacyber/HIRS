package hirs.attestationca.persist.service;

import hirs.attestationca.persist.entity.manager.IssuedCertificateRepository;
import hirs.attestationca.persist.entity.userdefined.certificate.IssuedAttestationCertificate;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 *
 */
@Log4j2
@Service
public class IssuedAttestationCertificateService {

    private final IssuedCertificateRepository issuedCertificateRepository;

    /**
     * @param issuedCertificateRepository issued certificate repository
     */
    @Autowired
    public IssuedAttestationCertificateService(
            final IssuedCertificateRepository issuedCertificateRepository) {
        this.issuedCertificateRepository = issuedCertificateRepository;
    }

    /**
     * @param archiveFlag archive flag
     * @param pageable    pageable
     * @return
     */
    public Page<IssuedAttestationCertificate> findByArchiveFlag(boolean archiveFlag, Pageable pageable) {
        return this.issuedCertificateRepository.findByArchiveFlag(archiveFlag, pageable);
    }

    /**
     * Retrieves the total number of records in the issued certificate repository.
     *
     * @return total number of records in the issued certificate repository.
     */
    public long findIssuedCertificateRepoCount() {
        return this.issuedCertificateRepository.findByArchiveFlag(false).size();
    }
}
