
package hirs.persist;

import hirs.data.persist.ReferenceDigestRecord;

import java.util.List;

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
     * @return status of successful update
     */
    boolean updateRecord(ReferenceDigestRecord referenceDigestRecord);

    /**
     * Delete the given record.
     *
     * @param referenceDigestRecord the digest record delete
     * @return true if the deletion succeeded, false otherwise.
     */
    boolean deleteRecord(ReferenceDigestRecord referenceDigestRecord);
}
