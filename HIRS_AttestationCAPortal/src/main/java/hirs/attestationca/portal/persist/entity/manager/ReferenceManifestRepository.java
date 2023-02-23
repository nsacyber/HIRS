package hirs.attestationca.portal.persist.entity.manager;

import hirs.attestationca.persist.entity.userdefined.ReferenceManifest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ReferenceManifestRepository extends JpaRepository<ReferenceManifest, UUID> {
}
