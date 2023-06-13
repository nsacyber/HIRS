package hirs.attestationca.persist.entity.manager;

import hirs.attestationca.persist.entity.userdefined.ReferenceManifest;
import hirs.attestationca.persist.entity.userdefined.rim.BaseReferenceManifest;
import hirs.attestationca.persist.entity.userdefined.rim.EventLogMeasurements;
import hirs.attestationca.persist.entity.userdefined.rim.SupportReferenceManifest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReferenceManifestRepository extends JpaRepository<ReferenceManifest, UUID> {

    @Query(value = "SELECT * FROM ReferenceManifest WHERE hexDecHash = ?1", nativeQuery = true)
    ReferenceManifest findByHash(String rimHash);
    @Query(value = "SELECT * FROM ReferenceManifest WHERE hexDecHash = ?1 AND rimType = ?2", nativeQuery = true)
    ReferenceManifest findByHash(String rimHash, String rimType);
    @Query(value = "SELECT * FROM ReferenceManifest WHERE platformManufacturer = ?1 AND platformModel = ?2 AND rimType = 'Base'", nativeQuery = true)
    List<BaseReferenceManifest> getBaseByManufacturerModel(String manufacturer, String model);
    @Query(value = "SELECT * FROM ReferenceManifest WHERE platformManufacturer = ?1 AND DTYPE = ?2", nativeQuery = true)
    ReferenceManifest getByManufacturer(String manufacturer, String dType);
    @Query(value = "SELECT * FROM ReferenceManifest WHERE platformModel = ?1 AND DTYPE = ?2", nativeQuery = true)
    ReferenceManifest getByModel(String model, String dType);
    @Query(value = "SELECT * FROM ReferenceManifest WHERE DTYPE = 'BaseReferenceManifest'", nativeQuery = true)
    List<BaseReferenceManifest> findAllBaseRims();
    @Query(value = "SELECT * FROM ReferenceManifest WHERE DTYPE = 'SupportReferenceManifest'", nativeQuery = true)
    List<SupportReferenceManifest> findAllSupportRims();
    @Query(value = "SELECT * FROM ReferenceManifest WHERE id = ?1 AND DTYPE = 'BaseReferenceManifest'", nativeQuery = true)
    BaseReferenceManifest getBaseRimEntityById(UUID uuid);
    @Query(value = "SELECT * FROM ReferenceManifest WHERE id = ?1 AND DTYPE = 'SupportReferenceManifest'", nativeQuery = true)
    SupportReferenceManifest getSupportRimEntityById(UUID uuid);
    @Query(value = "SELECT * FROM ReferenceManifest WHERE id = ?1 AND DTYPE = 'EventLogMeasurements'", nativeQuery = true)
    EventLogMeasurements getEventLogRimEntityById(UUID uuid);
    @Query(value = "SELECT * FROM ReferenceManifest WHERE deviceName = ?1 AND DTYPE = 'SupportReferenceManifest'", nativeQuery = true)
    List<SupportReferenceManifest> byDeviceName(String deviceName);
}
