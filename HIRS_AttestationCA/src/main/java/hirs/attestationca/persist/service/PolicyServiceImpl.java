package hirs.attestationca.persist.service;

import hirs.attestationca.persist.entity.manager.PolicyRepository;
import hirs.attestationca.persist.entity.userdefined.PolicySettings;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;

//@Service
public class PolicyServiceImpl extends DefaultDbService<PolicySettings> {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PolicyRepository repository;

    public void saveSettings(PolicySettings settings) {
        repository.save(settings);
    }


//    public Policy getDefaultPolicy(Appraiser appraiser) {
//        return repository.findByAppraiser(appraiser);
//    }
}
