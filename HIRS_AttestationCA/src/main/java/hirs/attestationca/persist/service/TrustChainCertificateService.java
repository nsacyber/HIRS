package hirs.attestationca.persist.service;

import hirs.attestationca.persist.entity.manager.CACredentialRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TrustChainCertificateService {

    private final CACredentialRepository caCredentialRepository;

    @Autowired
    public TrustChainCertificateService(CACredentialRepository caCredentialRepository) {
        this.caCredentialRepository = caCredentialRepository;
    }
}
