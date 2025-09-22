package hirs.attestationca.persist.service;

import hirs.attestationca.persist.entity.manager.CACredentialRepository;
import hirs.attestationca.persist.entity.userdefined.certificate.CertificateAuthorityCredential;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * A service layer class responsible for encapsulating all business logic related to the Trust Chain
 * Certificates Management Page.
 */
@Log4j2
@Service
public class TrustChainCertificatePageService {
    private final CACredentialRepository caCredentialRepository;

    /**
     * Constructor for the Trust Chain Certificate Page Service.
     *
     * @param caCredentialRepository certificate authority credential repository
     */
    @Autowired
    public TrustChainCertificatePageService(final CACredentialRepository caCredentialRepository) {
        this.caCredentialRepository = caCredentialRepository;
    }

    /**
     * Retrieves the total number of records in the certificate authority (trust chain) repository.
     *
     * @return total number of records in the certificate authority (trust chain) repository.
     */
    public long findTrustChainCertificateRepoCount() {
        return this.caCredentialRepository.findByArchiveFlag(false).size();
    }

    /**
     * Retrieves a page of certificate authority credentials using the provided archive flag and pageable value.
     *
     * @param archiveFlag archive flag
     * @param pageable    pageable
     * @return page of certificate authority credentials
     */
    public Page<CertificateAuthorityCredential> findCACredentialsByArchiveFlag(final boolean archiveFlag,
                                                                               final Pageable pageable) {
        return this.caCredentialRepository.findByArchiveFlag(archiveFlag, pageable);
    }
}
