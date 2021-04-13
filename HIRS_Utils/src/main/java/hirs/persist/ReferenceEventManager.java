package hirs.persist;

import hirs.data.persist.ReferenceDigestRecord;
import hirs.data.persist.ReferenceDigestValue;

import java.util.List;

/**
 * This class facilitates the persistence of {@link hirs.data.persist.ReferenceDigestValue}s
 * including storage, retrieval, and deletion.
 */
public interface ReferenceEventManager {
    /**
     * Persists a new Reference Digest value.
     *
     * @param referenceDigestValue the ReferenceDigestValue
     * @return the persisted ReferenceDigestValue
     */
    ReferenceDigestValue saveValue(ReferenceDigestValue referenceDigestValue);

    /**
     * Persists a new Reference Digest value.
     *
     * @param referenceDigestValue the ReferenceDigestValue
     * @return the persisted ReferenceDigestValue
     */
    ReferenceDigestValue getValue(ReferenceDigestValue referenceDigestValue);

    /**
     * Persists a new Reference Digest value.
     *
     * @param referenceDigestValue the ReferenceDigestValue
     * @return the persisted ReferenceDigestValue
     */
    ReferenceDigestValue getValueById(ReferenceDigestValue referenceDigestValue);

    /**
     * Persists a new Reference Digest value.
     *
     * @param referenceDigestRecord the ReferenceDigestRecord
     * @return the persisted list of ReferenceDigestValue
     */
    List<ReferenceDigestValue> getValuesByRecordId(ReferenceDigestRecord referenceDigestRecord);

    /**
     * Persists a new Reference Digest value.
     *
     * @param eventType the event type to look for
     * @return the persisted list of ReferenceDigestValue
     */
    List<ReferenceDigestValue> getValueByEventType(String eventType);

    /**
     * Updates an existing ReferenceDigestRecord.
     * @param referenceDigestValue the Reference Event update
     */
    void updateRecord(ReferenceDigestValue referenceDigestValue);

    /**
     * Delete the given value.
     *
     * @param referenceDigestValue the digest record delete
     * @return true if the deletion succeeded, false otherwise.
     */
    boolean deleteRecord(ReferenceDigestValue referenceDigestValue);
}
