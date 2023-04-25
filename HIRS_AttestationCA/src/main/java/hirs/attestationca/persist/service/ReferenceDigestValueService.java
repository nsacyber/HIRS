package hirs.attestationca.persist.service;

import hirs.attestationca.persist.entity.userdefined.ReferenceManifest;
import hirs.attestationca.persist.entity.userdefined.rim.ReferenceDigestValue;

import java.util.List;
import java.util.UUID;

public interface ReferenceDigestValueService {

    ReferenceDigestValue saveReferenceDigestValue(ReferenceDigestValue referenceDigestValue);

    List<ReferenceDigestValue> fetchDigestValues();

    ReferenceDigestValue updateRefDigestValue(ReferenceDigestValue referenceDigestValue, UUID rdvId);

    List<ReferenceDigestValue> getValuesByRimId(ReferenceManifest baseRim);

    void deleteRefDigestValueById(UUID rdvId);
}
