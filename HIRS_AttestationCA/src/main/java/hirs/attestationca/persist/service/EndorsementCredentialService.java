package hirs.attestationca.persist.service;

import hirs.attestationca.persist.entity.manager.EndorsementCredentialRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EndorsementCredentialService {

    private final EndorsementCredentialRepository endorsementCredentialRepository;

    @Autowired
    public EndorsementCredentialService(
            final EndorsementCredentialRepository endorsementCredentialRepository) {
        this.endorsementCredentialRepository = endorsementCredentialRepository;
    }
}
