package hirs.attestationca.portal.entity.manager;

import hirs.attestationca.portal.entity.userdefined.ReferenceManifest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ReferenceManifestRepository extends JpaRepository<ReferenceManifest, UUID> {
}
