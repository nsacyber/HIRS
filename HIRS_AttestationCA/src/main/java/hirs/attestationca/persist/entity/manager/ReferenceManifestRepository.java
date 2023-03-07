package hirs.attestationca.persist.entity.manager;

import hirs.attestationca.persist.entity.userdefined.ReferenceManifest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ReferenceManifestRepository extends JpaRepository<ReferenceManifest, UUID> {
}
