package hirs.attestationca.persist.service;

import hirs.attestationca.persist.entity.manager.PolicyRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 *
 */
@Log4j2
@Service
public class PolicyService {

    private final PolicyRepository policyRepository;

    /**
     * @param policyRepository
     */
    @Autowired
    public PolicyService(PolicyRepository policyRepository) {
        this.policyRepository = policyRepository;
    }
}
