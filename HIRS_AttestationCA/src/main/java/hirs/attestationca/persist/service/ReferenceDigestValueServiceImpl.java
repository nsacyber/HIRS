package hirs.attestationca.persist.service;

import hirs.attestationca.persist.entity.manager.ReferenceDigestValueRepository;
import hirs.attestationca.persist.entity.userdefined.rim.ReferenceDigestValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

@Service
public class ReferenceDigestValueServiceImpl extends DefaultDbService<ReferenceDigestValue> {

    @Autowired
    private ReferenceDigestValueRepository repository;

    public List<ReferenceDigestValue> getValuesByRimId(final UUID baseId) {
        return new LinkedList<>();
    }
}
