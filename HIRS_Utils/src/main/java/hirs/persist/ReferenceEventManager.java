package hirs.persist;

import hirs.data.persist.ReferenceDigestRecord;
import hirs.data.persist.ReferenceDigestValue;
import hirs.data.persist.ReferenceManifest;

import java.util.List;
import java.util.Set;

/**
 * This class facilitates the persistence of {@link hirs.data.persist.ReferenceDigestValue}s
 * including storage, retrieval, and deletion.
 */
public interface ReferenceEventManager extends OrderedListQuerier<ReferenceDigestValue> {
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
     * Persists a new Reference Digest Value.
     *
     * @param manufacturer the string value to search for
     * @return the persisted ReferenceDigestValue
     */
    List<ReferenceDigestValue> getValueByManufacturer(String manufacturer);

    /**
     * Persists a new Reference Digest.
     *
     * @param model the string value to search for
     * @return the persisted ReferenceDigestValue
     */
    List<ReferenceDigestValue> getValueByModel(String model);

    /**
     * Persists a new Reference Digest.
     *
     * @param manufacturer the string value to search for
     * @param model the string value to search for
     * @return the persisted ReferenceDigestValue
     */
    List<ReferenceDigestValue> getValueByManufacturerModel(String manufacturer, String model);

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
     * @param referenceManifest the referenceManifest
     * @return the persisted list of ReferenceDigestValue
     */
    List<ReferenceDigestValue> getValuesByRimId(ReferenceManifest referenceManifest);

    /**
     * Persists a new Reference Digest value.
     *
     * @param eventType the event type to look for
     * @return the persisted list of ReferenceDigestValue
     */
    List<ReferenceDigestValue> getValueByEventType(String eventType);

    /**
     * Returns a list of all <code>ReferenceDigestValue</code>s that are ordered by a column
     * and direction (ASC, DESC) that is provided by the user.  This method
     * helps support the server-side processing in the JQuery DataTables.
     *
     * @return FilteredRecordsList object with fields for DataTables
     */
    Set<ReferenceDigestValue> getEventList();

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
