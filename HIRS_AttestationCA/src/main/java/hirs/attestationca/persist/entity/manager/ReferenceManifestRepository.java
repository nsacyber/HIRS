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
     * Query that retrieves a reference manifest using the provided hex/dec hash.
     *
     * @param hexDecHash string representation of the hex dec hash
     * @return a reference manifest
     */
    ReferenceManifest findByHexDecHash(String hexDecHash);

    /**
     * Query that retrieves a reference manifest using the provided base 64 hash.
     *
     * @param base64Hash string representation of the base 64 hash
     * @return a reference manifest
     */
    ReferenceManifest findByBase64Hash(String base64Hash);

    /**
     * Query that retrieves a reference manifest using the provided hex/dec hash and rim type.
     *
     * @param hexDecHash string representation of the hex dec hash
     * @param rimType    string representation of the rim type
     * @return a reference manifest
     */
    ReferenceManifest findByHexDecHashAndRimType(String hexDecHash, String rimType);

    /**
     * Query that retrieves a reference manifest using the provided event log hash and rim type.
     *
     * @param hexDecHash string representation of the event log hash
     * @param rimType    string representation of the rim type
     * @return a reference manifest
     */
    ReferenceManifest findByEventLogHashAndRimType(String hexDecHash, String rimType);

    /**
     * Query that retrieves a list of base reference manifests using the provided manufacturer and model
     * and where the rim type is equal to base.
     *
     * @param manufacturer string representation of platform manufacturer
     * @param model        string representation of platform model
     * @return a list of base reference manifests
     */
    @Query(value = "SELECT * FROM ReferenceManifest WHERE platformManufacturer = ?1 AND platformModel = ?2 "
            + "AND rimType = 'Base'", nativeQuery = true)
    List<BaseReferenceManifest> getBaseByManufacturerModel(String manufacturer, String model);

    /**
     * Query that retrieves a list of base reference manifests using the provided manufacturer and model.
     *
     * @param manufacturer string representation of platform manufacturer
     * @param dType        dtype
     * @return a list of base reference manifests
     */
    @Query(value = "SELECT * FROM ReferenceManifest WHERE platformManufacturer = ?1 AND DTYPE = ?2",
            nativeQuery = true)
    List<BaseReferenceManifest> getByManufacturer(String manufacturer, String dType);

    /**
     * Query that retrieves a reference manifest using the provided model and dtype.
     *
     * @param model string representation of platform model
     * @param dType dtype
     * @return a reference manifest
     */
    @Query(value = "SELECT * FROM ReferenceManifest WHERE platformModel = ?1 AND DTYPE = ?2",
            nativeQuery = true)
    ReferenceManifest getByModel(String model, String dType);

    /**
     * Query that retrieves a list of base reference manifests where the dtype is a base reference manifest.
     *
     * @return a list of base reference manifests
     */
    @Query(value = "SELECT * FROM ReferenceManifest WHERE DTYPE = 'BaseReferenceManifest'",
            nativeQuery = true)
    List<BaseReferenceManifest> findAllBaseRims();

    /**
     * Query that retrieves a list of support reference manifests where the dtype is a
     * support reference manifest.
     *
     * @return a list of support reference manifests
     */
    @Query(value = "SELECT * FROM ReferenceManifest WHERE DTYPE = 'SupportReferenceManifest'",
            nativeQuery = true)
    List<SupportReferenceManifest> findAllSupportRims();

    /**
     * Query that retrieves a base reference manifest using the provided uuid and where the dtype is a
     * base reference manifest.
     *
     * @param uuid uuid
     * @return a base reference manifest
     */
    @Query(value = "SELECT * FROM ReferenceManifest WHERE id = ?1 AND DTYPE = 'BaseReferenceManifest'",
            nativeQuery = true)
    BaseReferenceManifest getBaseRimEntityById(UUID uuid);

    /**
     * Query that retrieves a support reference manifest using the provided uuid and
     * where the dtype is a support reference manifest.
     *
     * @param uuid uuid
     * @return a support reference manifest
     */
    @Query(value = "SELECT * FROM ReferenceManifest WHERE id = ?1 AND DTYPE = 'SupportReferenceManifest'",
            nativeQuery = true)
    SupportReferenceManifest getSupportRimEntityById(UUID uuid);

    /**
     * Query that retrieves event log measurements using the provided uuid and where the dtype is an
     * event log measurement.
     *
     * @param uuid uuid
     * @return event log measurements
     */
    @Query(value = "SELECT * FROM ReferenceManifest WHERE id = ?1 AND DTYPE = 'EventLogMeasurements'",
            nativeQuery = true)
    EventLogMeasurements getEventLogRimEntityById(UUID uuid);

    /**
     * Query that retrieves a list of support reference manifests using the provided device name and where the
     * dtype is a support reference manifest.
     *
     * @param deviceName string representation of the device name
     * @return a list of support reference manifests
     */
    @Query(value = "SELECT * FROM ReferenceManifest WHERE deviceName = ?1 "
            + "AND DTYPE = 'SupportReferenceManifest'", nativeQuery = true)
    List<SupportReferenceManifest> byDeviceName(String deviceName);

    /**
     * Query that retrieves event log measurements using the provided device name and where the dtype is
     * event log measurements.
     *
     * @param deviceName string representation of the device name
     * @return event log measurements
     */
    @Query(value = "SELECT * FROM ReferenceManifest WHERE deviceName = ?1 "
            + "AND DTYPE = 'EventLogMeasurements'", nativeQuery = true)
    EventLogMeasurements byMeasurementDeviceName(String deviceName);

    /**
     * Query that retrieves a list of support reference manifests using the provided manufacturer and platform
     * model and where the rim type is support.
     *
     * @param manufacturer string representation of platform manufacturer
     * @param model        string representation of platform model
     * @return a list of support reference manifests
     */
    @Query(value = "SELECT * FROM ReferenceManifest WHERE platformManufacturer = ?1 AND platformModel = ?2 "
            + "AND rimType = 'Support'", nativeQuery = true)
    List<SupportReferenceManifest> getSupportByManufacturerModel(String manufacturer, String model);

    /**
     * Query that retrieves event log measurements using the provided platform model and where the dtype is
     * event log measurements.
     *
     * @param model string representation of platform model.
     * @return event log measurements
     */
    @Query(value = "SELECT * FROM ReferenceManifest WHERE platformModel = ?1 "
            + "AND DTYPE = 'EventLogMeasurements'", nativeQuery = true)
    EventLogMeasurements getLogByModel(String model);

    /**
     * Query that retrieves a list of reference manifests using the provided device name.
     *
     * @param deviceName string representation of device name
     * @return a list of reference manifests
     */
    List<ReferenceManifest> findByDeviceName(String deviceName);

    /**
     * Query that retrieves a list of reference manifests using the provided archive flag.
     *
     * @param archiveFlag archive flag
     * @return a list of reference manifests
     */
    List<ReferenceManifest> findByArchiveFlag(boolean archiveFlag);

    /**
     * Query that retrieves a page of reference manifests using the provided archive flag and pageable value.
     *
     * @param archiveFlag archive flag
     * @param pageable    pageable
     * @return a page of reference manifests
     */
    Page<ReferenceManifest> findByArchiveFlag(boolean archiveFlag, Pageable pageable);
}
