package hirs.attestationca.persist.service;

import hirs.attestationca.persist.entity.manager.PlatformCertificateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PlatformCredentialService {

    private final PlatformCertificateRepository platformCertificateRepository;

    @Autowired
    public PlatformCredentialService(final PlatformCertificateRepository platformCertificateRepository) {
        this.platformCertificateRepository = platformCertificateRepository;
    }
}
