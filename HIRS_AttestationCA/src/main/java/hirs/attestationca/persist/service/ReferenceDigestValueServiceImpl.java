package hirs.attestationca.persist.service;

import hirs.attestationca.persist.entity.manager.ReferenceDigestValueRepository;
import hirs.attestationca.persist.entity.userdefined.ReferenceManifest;
import hirs.attestationca.persist.entity.userdefined.rim.ReferenceDigestValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.datatables.mapping.DataTablesInput;
import org.springframework.data.jpa.datatables.mapping.DataTablesOutput;
import org.springframework.stereotype.Service;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

@Service
public class ReferenceDigestValueServiceImpl extends DefaultDbService<ReferenceDigestValue> implements ReferenceDigestValueService {

    @Autowired
    private ReferenceDigestValueRepository repository;

    @Override
    public ReferenceDigestValue saveReferenceDigestValue(ReferenceDigestValue referenceDigestValue) {
        return repository.save(referenceDigestValue);
    }

    public List<ReferenceDigestValue> findAll() {
        return repository.findAll();
    }

    @Override
    public List<ReferenceDigestValue> fetchDigestValues() {
        return repository.findAll();
    }

    @Override
    public ReferenceDigestValue updateRefDigestValue(ReferenceDigestValue referenceDigestValue, UUID rdvId) {
        return saveReferenceDigestValue(referenceDigestValue);
    }

    public ReferenceDigestValue updateRefDigestValue(ReferenceDigestValue referenceDigestValue) {
        if (referenceDigestValue.getId() != null) {
            return updateRefDigestValue(referenceDigestValue, referenceDigestValue.getId());
        }
        return null;
    }

    public List<ReferenceDigestValue> getValuesByRimId(ReferenceManifest baseRim) {
        List<ReferenceDigestValue> results = new LinkedList<>();
        if (baseRim != null) {
            for (ReferenceDigestValue rdv : repository.findAll()) {
                if (rdv.getBaseRimId() == baseRim.getId()) {
                    results.add(rdv);
                }
            }
        }

        return results;
    }

    @Override
    public void deleteRefDigestValueById(UUID rdvId) {
        repository.getReferenceById(rdvId).archive();
    }
}
