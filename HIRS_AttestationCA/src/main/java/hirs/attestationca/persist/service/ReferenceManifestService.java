package hirs.attestationca.persist.service;

import hirs.attestationca.persist.entity.manager.ReferenceDigestValueRepository;
import hirs.attestationca.persist.entity.manager.ReferenceManifestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 *
 */
@Service
public class ReferenceManifestService {

    private final ReferenceManifestRepository referenceManifestRepository;
    private final ReferenceDigestValueRepository referenceDigestValueRepository;

    @Autowired
    public ReferenceManifestService(ReferenceManifestRepository referenceManifestRepository,
                                    ReferenceDigestValueRepository referenceDigestValueRepository) {
        this.referenceManifestRepository = referenceManifestRepository;
        this.referenceDigestValueRepository = referenceDigestValueRepository;
    }
}
