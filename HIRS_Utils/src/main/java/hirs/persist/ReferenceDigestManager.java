
package hirs.persist;

import hirs.data.persist.ReferenceDigestRecord;

import java.util.Set;

/**
 * This class facilitates the persistence of {@link hirs.data.persist.ReferenceDigestRecord}s
 * including storage, retrieval, and deletion.
 */
public interface ReferenceDigestManager extends OrderedListQuerier<ReferenceDigestRecord> {

    /**
     * Persists a new Reference Digest.
     *
     * @param referenceDigestRecord the ReferenceDigestRecord
     * @return the persisted ReferenceDigestRecord
     */
    ReferenceDigestRecord save(ReferenceDigestRecord referenceDigestRecord);

    /**
     * Updates an existing ReferenceDigestRecord.
     * @param referenceDigestRecord the Reference Digest update
     */
    void update(ReferenceDigestRecord referenceDigestRecord);

    /**
     * Retrieve Reference Digest according to the given {@link ReferenceDigestSelector}.
     *
     * @param <T> the type of reference digest that will be retrieved
     *  @param referenceDigestSelector a {@link ReferenceDigestSelector} to use for querying
     * @return a Set of matching RIMs, which may be empty
     */
    <T extends ReferenceDigestRecord> Set<T> get(ReferenceDigestSelector referenceDigestSelector);

    /**
     * Delete the given RIM.
     *
     * @param referenceDigestRecord the RIM to delete
     * @return true if the deletion succeeded, false otherwise.
     */
    boolean delete(ReferenceDigestRecord referenceDigestRecord);
}
