package hirs.attestationca.persist.service;

import hirs.attestationca.persist.entity.manager.IssuedCertificateRepository;
import hirs.attestationca.persist.entity.userdefined.certificate.IssuedAttestationCertificate;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * A service layer class responsible for encapsulating all business logic related to the Issued Attestation
 * Certificate Page.
 */
@Log4j2
@Service
public class IssuedAttestationCertificatePageService {
    private final IssuedCertificateRepository issuedCertificateRepository;

    /**
     * Constructor for the Issued Certificate Page Service.
     *
     * @param issuedCertificateRepository issued certificate repository
     */
    @Autowired
    public IssuedAttestationCertificatePageService(final IssuedCertificateRepository issuedCertificateRepository) {
        this.issuedCertificateRepository = issuedCertificateRepository;
    }

    /**
     * Retrieves a page of issued attestation certificates using the provided archive flag and pageable value.
     *
     * @param archiveFlag archive flag
     * @param pageable    pageable
     * @return page of issued attestation certificates
     */
    public Page<IssuedAttestationCertificate> findIssuedCertificatesByArchiveFlag(final boolean archiveFlag,
                                                                                  final Pageable pageable) {
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
