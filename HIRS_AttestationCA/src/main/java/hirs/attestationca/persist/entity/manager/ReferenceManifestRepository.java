package hirs.attestationca.persist.entity.manager;

import hirs.attestationca.persist.entity.userdefined.ReferenceManifest;
import hirs.attestationca.persist.entity.userdefined.rim.BaseReferenceManifest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReferenceManifestRepository extends JpaRepository<ReferenceManifest, UUID> {

    @Query(value = "SELECT * FROM ReferenceManifest WHERE u.hexDecHash = ?1", nativeQuery = true)
    ReferenceManifest findByHash(String rimHash);
    @Query(value = "SELECT * FROM ReferenceManifest WHERE u.hexDecHash = ?1 AND u.rimType = ?2", nativeQuery = true)
    ReferenceManifest findByHash(String rimHash, String rimType);
    @Query(value = "SELECT * FROM ReferenceManifest WHERE u.platformManufacturer = ?1 AND u.platformModel = ?2 AND u.rimType = 'Base'", nativeQuery = true)
    List<BaseReferenceManifest> getBaseByManufacturerModel(String manufacturer, String model);
}
