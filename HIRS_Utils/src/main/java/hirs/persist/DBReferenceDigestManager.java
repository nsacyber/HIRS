package hirs.persist;

import hirs.data.persist.ReferenceDigestRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.SessionFactory;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This class is used to persist and retrieve {@link hirs.data.persist.ReferenceDigestRecord}s into
 * and from the database.
 */
public class DBReferenceDigestManager extends DBManager<ReferenceDigestRecord>
        implements ReferenceDigestManager {

    private static final Logger LOGGER = LogManager.getLogger(DBReferenceDigestManager.class);

    /**
     * Default Constructor.
     *
     * @param sessionFactory session factory used to access database connections
     */
    public DBReferenceDigestManager(final SessionFactory sessionFactory) {
        super(ReferenceDigestRecord.class, sessionFactory);
    }

    /**
     * This method does not need to be used directly as it is used by
     * {@link ReferenceManifestSelector}'s get* methods. Regardless, it may be
     * used to retrieve ReferenceManifest by other code in this package, given a
     * configured ReferenceManifestSelector.
     *
     * @param referenceDigestSelector a configured
     * {@link ReferenceDigestSelector} to use for querying
     * @return the resulting set of ReferenceManifest, possibly empty
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T extends  ReferenceDigestRecord> Set<T> get(
            final ReferenceDigestSelector referenceDigestSelector) {
        LOGGER.info("Getting the full set of Reference Digest Records.");
        return new HashSet<>(
                (List<T>) getWithCriteria(
                        referenceDigestSelector.getReferenceDigestClass(),
                        Collections.singleton(referenceDigestSelector.getCriterion())
                )
        );
    }

    /**
     * Remove a ReferenceDigestRecord from the database.
     *
     * @param referenceDigestRecord the referenceDigestRecord to delete
     * @return true if deletion was successful, false otherwise
     */
    public boolean deleteReferenceManifest(final ReferenceDigestRecord referenceDigestRecord) {
        LOGGER.info(String.format("Deleting reference to %s/%s",
                referenceDigestRecord.getManufacturer(), referenceDigestRecord.getModel()));
        return delete(referenceDigestRecord);
    }
}
