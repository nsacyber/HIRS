package hirs.persist;

import hirs.data.persist.ReferenceDigestRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.SessionFactory;

import java.util.List;

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

    @Override
    public ReferenceDigestRecord saveRecord(final ReferenceDigestRecord referenceDigestRecord) {
        LOGGER.debug("saving state: {}", referenceDigestRecord);
        try {
            return save(referenceDigestRecord);
        } catch (DBManagerException dbMEx) {
            throw new RuntimeException(dbMEx);
        }
    }

    @Override
    public ReferenceDigestRecord getRecord(final ReferenceDigestRecord referenceDigestRecord) {
        LOGGER.debug("Getting record for {}", referenceDigestRecord);
        if (referenceDigestRecord == null) {
            LOGGER.error("null referenceDigestRecord argument");
            throw new NullPointerException("null referenceDigestRecord");
        }
        return null;
    }

    @Override
    public List<ReferenceDigestRecord> getRecordsByManufacturer(
            final ReferenceDigestRecord referenceDigestRecord) {
        LOGGER.debug("Getting records by manufacturer for {}", referenceDigestRecord);
        if (referenceDigestRecord == null) {
            LOGGER.error("null referenceDigestRecord argument");
            throw new NullPointerException("null referenceDigestRecord");
        }
        return null;
    }

    @Override
    public List<ReferenceDigestRecord> getRecordsByModel(
            final ReferenceDigestRecord referenceDigestRecord) {
        LOGGER.debug("Getting records by model for {}", referenceDigestRecord);
        if (referenceDigestRecord == null) {
            LOGGER.error("null referenceDigestRecord argument");
            throw new NullPointerException("null referenceDigestRecord");
        }
        return null;
    }

    @Override
    public boolean updateRecord(final ReferenceDigestRecord referenceDigestRecord) {
        return false;
    }

    /**
     * Remove a ReferenceDigestRecord from the database.
     *
     * @param referenceDigestRecord the referenceDigestRecord to delete
     * @return true if deletion was successful, false otherwise
     */
    @Override
    public boolean deleteRecord(final ReferenceDigestRecord referenceDigestRecord) {
        LOGGER.info(String.format("Deleting reference to %s/%s",
                referenceDigestRecord.getManufacturer(), referenceDigestRecord.getModel()));
        return delete(referenceDigestRecord);
    }
}
