package hirs.attestationca.persist.service;

import hirs.attestationca.persist.OrderedListQuerier;
import hirs.attestationca.persist.entity.userdefined.ReferenceManifest;
import hirs.attestationca.persist.service.selector.ReferenceManifestSelector;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface ReferenceManifestService<T extends ReferenceManifest> extends OrderedListQuerier<T> {

    ReferenceManifest saveReferenceManifest(ReferenceManifest referenceManifest);

    List<ReferenceManifest> fetchReferenceManifests();
//    DataTablesOutput<ReferenceManifest> fetchReferenceManifests(DataTablesInput input);

    ReferenceManifest updateReferenceManifest(ReferenceManifest referenceManifest, UUID rimId);

    void deleteReferenceManifestById(UUID rimId);

    <T extends  ReferenceManifest> Set<T> get(ReferenceManifestSelector referenceManifestSelector);
}
