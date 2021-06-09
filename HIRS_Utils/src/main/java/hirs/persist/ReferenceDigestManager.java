
package hirs.persist;

import hirs.data.persist.ReferenceDigestRecord;

import java.util.List;
import java.util.UUID;

/**
 * This class facilitates the persistence of {@link hirs.data.persist.ReferenceDigestRecord}s
 * including storage, retrieval, and deletion.
 */
public interface ReferenceDigestManager {

    /**
     * Persists a new Reference Digest.
     *
     * @param referenceDigestRecord the ReferenceDigestRecord
     * @return the persisted ReferenceDigestRecord
     */
    ReferenceDigestRecord saveRecord(ReferenceDigestRecord referenceDigestRecord);

    /**
     * Persists a new Reference Digest.
     *
     * @param referenceDigestRecord the ReferenceDigestRecord
     * @return the persisted ReferenceDigestRecord
     */
    ReferenceDigestRecord getRecord(ReferenceDigestRecord referenceDigestRecord);

    /**
     * Persists a new Reference Digest.
     *
     * @param manufacturer the string of the manufacturer
     * @param model the string of the model
     * @return the persisted ReferenceDigestRecord
     */
    ReferenceDigestRecord getRecord(String manufacturer, String model);

    /**
     * Persists a new Reference Digest.
     *
     * @param deviceName the string of the network hostname
     * @return the persisted ReferenceDigestRecord list
     */
    List<ReferenceDigestRecord> getRecordsByDeviceName(String deviceName);

    /**
     * Persists a new Reference Digest.
     *
     * @param referenceDigestRecord the ReferenceDigestRecord
     * @return the persisted ReferenceDigestRecord
     */
    ReferenceDigestRecord getRecordById(ReferenceDigestRecord referenceDigestRecord);

    /**
     * Persists a new Reference Digest.
     *
     * @param supportId the support RIM UUID
     * @return the persisted ReferenceDigestRecord
     */
    ReferenceDigestRecord getRecordBySupportId(UUID supportId);

    /**
     * Persists a new Reference Digest.
     *
     * @param referenceDigestRecord the ReferenceDigestRecord
     * @return the persisted ReferenceDigestRecord
     */
    List<ReferenceDigestRecord> getRecordsByManufacturer(
            ReferenceDigestRecord referenceDigestRecord);

    /**
     * Persists a new Reference Digest.
     *
     * @param referenceDigestRecord the ReferenceDigestRecord
     * @return the persisted ReferenceDigestRecord
     */
    List<ReferenceDigestRecord> getRecordsByModel(ReferenceDigestRecord referenceDigestRecord);

    /**
     * Updates an existing ReferenceDigestRecord.
     * @param referenceDigestRecord the Reference Digest update
     */
    void updateRecord(ReferenceDigestRecord referenceDigestRecord);

    /**
     * Delete the given record.
     *
     * @param referenceDigestRecord the digest record delete
     * @return true if the deletion succeeded, false otherwise.
     */
    boolean deleteRecord(ReferenceDigestRecord referenceDigestRecord);
}
