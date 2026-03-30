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

/**
 * Repository interface for managing {@link ReferenceManifest} entities in the database.
 *
 * <p>
 * The {@link ReferenceManifestRepository} interface extends {@link JpaRepository} to provide basic CRUD operations,
 * including save, find, delete, and query methods. Custom query methods can be defined
 * using Spring Data JPA's query method naming conventions or with the Query annotation.
 * </p>
 */
@Repository
public interface ReferenceManifestRepository extends JpaRepository<ReferenceManifest, UUID> {
    /**
     * Query that retrieves a {@link ReferenceManifest} object using the provided hex/dec hash.
     *
     * @param hexDecHash string representation of the hex dec hash
     * @return a {@link ReferenceManifest} object
     */
    ReferenceManifest findByHexDecHash(String hexDecHash);

    /**
     * Query that retrieves a {@link ReferenceManifest} object using the provided base 64 hash.
     *
     * @param base64Hash string representation of the base 64 hash
     * @return a {@link ReferenceManifest} object
     */
    ReferenceManifest findByBase64Hash(String base64Hash);

    /**
     * Query that retrieves a {@link ReferenceManifest} object using the provided hex/dec hash and rim type.
     *
     * @param hexDecHash string representation of the hex dec hash
     * @param rimType    string representation of the rim type
     * @return a {@link ReferenceManifest} object
     */
    ReferenceManifest findByHexDecHashAndRimType(String hexDecHash, String rimType);

    /**
     * Query that retrieves an unarchived {@link ReferenceManifest} object using the provided hex/dec hash and rim type.
     *
     * @param hexDecHash string representation of the hex dec hash
     * @param rimType    string representation of the rim type
     * @return a {@link ReferenceManifest} object
     */
    @Query(value = "SELECT * FROM ReferenceManifest WHERE hexDecHash = ?1 AND rimType = ?2 "
            + "AND archiveFlag is false", nativeQuery = true)
    ReferenceManifest findByHexDecHashAndRimTypeUnarchived(String hexDecHash, String rimType);

    /**
     * Query that retrieves a {@link ReferenceManifest} object using the provided event log hash and rim type.
     *
     * @param hexDecHash string representation of the event log hash
     * @param rimType    string representation of the rim type
     * @return a {@link ReferenceManifest} object
     */
    ReferenceManifest findByEventLogHashAndRimType(String hexDecHash, String rimType);

    /**
     * Query that retrieves a list of {@link BaseReferenceManifest} objects using the provided manufacturer and model
     * and where the rim type is equal to base.
     *
     * @param manufacturer string representation of platform manufacturer
     * @param model        string representation of platform model
     * @return a list of {@link BaseReferenceManifest} objects
     */
    @Query(value = "SELECT * FROM ReferenceManifest WHERE platformManufacturer = ?1 AND platformModel = ?2 "
            + "AND rimType = 'Base'", nativeQuery = true)
    List<BaseReferenceManifest> getBaseByManufacturerModel(String manufacturer, String model);

    /**
     * Query that retrieves a list of {@link BaseReferenceManifest} objects using the provided manufacturer and model.
     *
     * @param manufacturer string representation of platform manufacturer
     * @param dType        dtype
     * @return a list of {@link BaseReferenceManifest} objects
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
     * Query that retrieves a list of {@link BaseReferenceManifest} objects where the dtype is a base
     * reference manifest.
     *
     * @return a list of {@link BaseReferenceManifest} objects
     */
    @Query(value = "SELECT * FROM ReferenceManifest WHERE DTYPE = 'BaseReferenceManifest'",
            nativeQuery = true)
    List<BaseReferenceManifest> findAllBaseRims();

    /**
     * Query that retrieves a list of {@link SupportReferenceManifest} objects where the dtype is a
     * support reference manifest.
     *
     * @return a list of {@link SupportReferenceManifest} objects
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
     * Query that retrieves a list of {@link SupportReferenceManifest} objects using the provided device name and where
     * the dtype is a support reference manifest.
     *
     * @param deviceName string representation of the device name
     * @return a list of {@link SupportReferenceManifest} objects
     */
    @Query(value = "SELECT * FROM ReferenceManifest WHERE deviceName = ?1 "
            + "AND DTYPE = 'SupportReferenceManifest'", nativeQuery = true)
    List<SupportReferenceManifest> getSupportRimsByDeviceName(String deviceName);

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
     * Query that retrieves unarchived event log measurements using the provided device name
     * and where the dtype is event log measurements.
     *
     * @param deviceName string representation of the device name
     * @return event log measurements
     */
    @Query(value = "SELECT * FROM ReferenceManifest WHERE deviceName = ?1 "
            + "AND DTYPE = 'EventLogMeasurements' AND archiveFlag is false", nativeQuery = true)
    EventLogMeasurements byMeasurementDeviceNameUnarchived(String deviceName);

    /**
     * Query that retrieves a list of {@link SupportReferenceManifest} objects using the provided manufacturer and
     * platform model and where the rim type is support.
     *
     * @param manufacturer string representation of platform manufacturer
     * @param model        string representation of platform model
     * @return a list of {@link SupportReferenceManifest} objects
     */
    @Query(value = "SELECT * FROM ReferenceManifest WHERE platformManufacturer = ?1 AND platformModel = ?2 "
            + "AND rimType = 'Support'", nativeQuery = true)
    List<SupportReferenceManifest> getSupportByManufacturerModel(String manufacturer, String model);

    /**
     * Query that retrieves an {@link EventLogMeasurements} object using the provided platform model and where the
     * dtype is event log measurements.
     *
     * @param model string representation of platform model.
     * @return an {@link EventLogMeasurements} object
     */
    @Query(value = "SELECT * FROM ReferenceManifest WHERE platformModel = ?1 "
            + "AND DTYPE = 'EventLogMeasurements'", nativeQuery = true)
    EventLogMeasurements getLogByModel(String model);

    /**
     * Query that retrieves a list of {@link ReferenceManifest} objects using the provided device name.
     *
     * @param deviceName string representation of device name
     * @return a list of {@link ReferenceManifest} objects
     */
    List<ReferenceManifest> findByDeviceName(String deviceName);

    /**
     * Query that retrieves a list of {@link ReferenceManifest} objects using the provided archive flag.
     *
     * @param archiveFlag archive flag
     * @return a list of {@link ReferenceManifest} objects
     */
    List<ReferenceManifest> findByArchiveFlag(boolean archiveFlag);

    /**
     * Query that retrieves a count of {@link ReferenceManifest} objects in the database filtered by the provided
     * archive flag.
     *
     * @param archiveFlag archive flag
     * @return a count of {@link ReferenceManifest} objects
     */
    long countByArchiveFlag(boolean archiveFlag);

    /**
     * Retrieves a paginated list of {@link ReferenceManifest} instances matching the specified subclass types.
     *
     * @param types    the list of {@link ReferenceManifest} subclass types to include
     * @param pageable pageable
     * @return a page of matching {@link ReferenceManifest} instances
     */
    Page<ReferenceManifest> findByClassIn(List<Class<? extends ReferenceManifest>> types, Pageable pageable);
}
