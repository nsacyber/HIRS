package hirs.attestationca.persist.entity.manager;

import hirs.attestationca.persist.entity.userdefined.ReferenceManifest;
import hirs.attestationca.persist.entity.userdefined.rim.BaseReferenceManifest;
import hirs.attestationca.persist.entity.userdefined.rim.EventLogMeasurements;
import hirs.attestationca.persist.entity.userdefined.rim.SupportReferenceManifest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReferenceManifestRepository extends JpaRepository<ReferenceManifest, UUID> {
    /**
     * Query that retrieves a
     *
     * @param hexDecHash
     * @return
     */
    ReferenceManifest findByHexDecHash(String hexDecHash);

    /**
     * Query that retrieves a
     *
     * @param base64Hash
     * @return
     */
    ReferenceManifest findByBase64Hash(String base64Hash);

    /**
     * @param hexDecHash
     * @param rimType
     * @return
     */
    ReferenceManifest findByHexDecHashAndRimType(String hexDecHash, String rimType);

    /**
     * @param hexDecHash
     * @param rimType
     * @return
     */
    ReferenceManifest findByEventLogHashAndRimType(String hexDecHash, String rimType);

    /**
     * @param manufacturer
     * @param model
     * @return
     */
    @Query(value = "SELECT * FROM ReferenceManifest WHERE platformManufacturer = ?1 AND platformModel = ?2 AND rimType = 'Base'", nativeQuery = true)
    List<BaseReferenceManifest> getBaseByManufacturerModel(String manufacturer, String model);

    /**
     * @param manufacturer
     * @param dType
     * @return
     */
    @Query(value = "SELECT * FROM ReferenceManifest WHERE platformManufacturer = ?1 AND DTYPE = ?2", nativeQuery = true)
    List<BaseReferenceManifest> getByManufacturer(String manufacturer, String dType);

    /**
     * @param model
     * @param dType
     * @return
     */
    @Query(value = "SELECT * FROM ReferenceManifest WHERE platformModel = ?1 AND DTYPE = ?2", nativeQuery = true)
    ReferenceManifest getByModel(String model, String dType);

    /**
     * @return
     */
    @Query(value = "SELECT * FROM ReferenceManifest WHERE DTYPE = 'BaseReferenceManifest'", nativeQuery = true)
    List<BaseReferenceManifest> findAllBaseRims();

    /**
     * Query that retrieves a
     *
     * @return
     */
    @Query(value = "SELECT * FROM ReferenceManifest WHERE DTYPE = 'SupportReferenceManifest'", nativeQuery = true)
    List<SupportReferenceManifest> findAllSupportRims();

    /**
     * Query that retrieves a
     *
     * @param uuid
     * @return
     */
    @Query(value = "SELECT * FROM ReferenceManifest WHERE id = ?1 AND DTYPE = 'BaseReferenceManifest'", nativeQuery = true)
    BaseReferenceManifest getBaseRimEntityById(UUID uuid);

    /**
     * Query that retrieves a
     *
     * @param uuid
     * @return
     */
    @Query(value = "SELECT * FROM ReferenceManifest WHERE id = ?1 AND DTYPE = 'SupportReferenceManifest'", nativeQuery = true)
    SupportReferenceManifest getSupportRimEntityById(UUID uuid);

    /**
     * Query that retrieves a
     *
     * @param uuid
     * @return
     */
    @Query(value = "SELECT * FROM ReferenceManifest WHERE id = ?1 AND DTYPE = 'EventLogMeasurements'", nativeQuery = true)
    EventLogMeasurements getEventLogRimEntityById(UUID uuid);

    /**
     * Query that retrieves a
     *
     * @param deviceName
     * @return
     */
    @Query(value = "SELECT * FROM ReferenceManifest WHERE deviceName = ?1 AND DTYPE = 'SupportReferenceManifest'", nativeQuery = true)
    List<SupportReferenceManifest> byDeviceName(String deviceName);

    /**
     * Query that retrieves a
     *
     * @param deviceName
     * @return
     */
    @Query(value = "SELECT * FROM ReferenceManifest WHERE deviceName = ?1 AND DTYPE = 'EventLogMeasurements'", nativeQuery = true)
    EventLogMeasurements byMeasurementDeviceName(String deviceName);

    /**
     * Query that retrieves a
     *
     * @param manufacturer
     * @param model
     * @return
     */
    @Query(value = "SELECT * FROM ReferenceManifest WHERE platformManufacturer = ?1 AND platformModel = ?2 AND rimType = 'Support'", nativeQuery = true)
    List<SupportReferenceManifest> getSupportByManufacturerModel(String manufacturer, String model);

    /**
     * Query that retrieves a
     *
     * @param model
     * @return
     */
    @Query(value = "SELECT * FROM ReferenceManifest WHERE platformModel = ?1 AND DTYPE = 'EventLogMeasurements'", nativeQuery = true)
    EventLogMeasurements getLogByModel(String model);

    /**
     * Query that retrieves a
     *
     * @param deviceName
     * @return
     */
    List<ReferenceManifest> findByDeviceName(String deviceName);

    /**
     * Query that retrieves a
     *
     * @param archiveFlag
     * @return
     */
    List<ReferenceManifest> findByArchiveFlag(boolean archiveFlag);

    /**
     * Query that retrieves a
     *
     * @param archiveFlag
     * @param pageable
     * @return
     */
    Page<ReferenceManifest> findByArchiveFlag(boolean archiveFlag, Pageable pageable);
}
