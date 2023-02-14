package hirs.attestationca.portal.service;

import hirs.attestationca.portal.entity.manager.SettingsRepository;
import hirs.attestationca.portal.entity.userdefined.SupplyChainSettings;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class SettingsServiceImpl {

    @Autowired(required = false)
    private EntityManager entityManager;

    @Autowired
    private SettingsRepository repository;

    public SupplyChainSettings updateSettings(SupplyChainSettings settings, UUID uuid) {
        if (repository.existsById(uuid)) {
            // already in DB
        }

        return updateSettings(settings);
    }

    public SupplyChainSettings updateSettings(SupplyChainSettings settings) {
        return repository.save(settings);
    }

    public void saveSettings(SupplyChainSettings settings) {
        repository.save(settings);
    }

    public SupplyChainSettings getByName(String name) {
        if (name == null) {
            return null;
        }
        return repository.findByName(name);
    }

//    public Policy getDefaultPolicy(Appraiser appraiser) {
//        return repository.findByAppraiser(appraiser);
//    }
}
