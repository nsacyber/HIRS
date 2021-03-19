package hirs.persist;

import hirs.data.persist.ReferenceDigestRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
        LOGGER.debug("saving digest record: {}", referenceDigestRecord);
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
            return null;
        }

        if (referenceDigestRecord.getManufacturer() == null
                || referenceDigestRecord.getModel() == null) {
            LOGGER.error("No reference to get record from db {}", referenceDigestRecord);
            return null;
        }

        ReferenceDigestRecord dbRecord = null;
        Transaction tx = null;
        Session session = getFactory().getCurrentSession();
        try {
            LOGGER.debug("retrieving referenceDigestRecord from db");
            tx = session.beginTransaction();
            dbRecord = (ReferenceDigestRecord) session.createCriteria(ReferenceDigestRecord.class)
                    .add(Restrictions.eq("manufacturer",
                            referenceDigestRecord.getManufacturer())).add(Restrictions.eq("model",
                            referenceDigestRecord.getModel())).uniqueResult();
            tx.commit();
        } catch (Exception ex) {
            final String msg = "unable to retrieve object";
            LOGGER.error(msg, ex);
            if (tx != null) {
                LOGGER.debug("rolling back transaction");
                tx.rollback();
            }
            throw new DBManagerException(msg, ex);
        }
        return dbRecord;
    }

    @Override
    public ReferenceDigestRecord getRecordById(final ReferenceDigestRecord referenceDigestRecord) {
        LOGGER.debug("Getting record for {}", referenceDigestRecord);
        if (referenceDigestRecord == null) {
            LOGGER.error("null referenceDigestRecord argument");
            return null;
        }

        if (referenceDigestRecord.getId() == null) {
            LOGGER.error("No id to get record from db {}", referenceDigestRecord);
            return null;
        }

        ReferenceDigestRecord dbRecord;
        Transaction tx = null;
        Session session = getFactory().getCurrentSession();
        try {
            LOGGER.debug("retrieving referenceDigestRecord from db");
            tx = session.beginTransaction();
            dbRecord = (ReferenceDigestRecord) session.createCriteria(ReferenceDigestRecord.class)
                    .add(Restrictions.eq("id",
                            referenceDigestRecord.getId())).uniqueResult();
            tx.commit();
        } catch (Exception ex) {
            final String msg = "unable to retrieve object";
            LOGGER.error(msg, ex);
            if (tx != null) {
                LOGGER.debug("rolling back transaction");
                tx.rollback();
            }
            throw new DBManagerException(msg, ex);
        }
        return dbRecord;
    }

    @Override
    public ReferenceDigestRecord getRecordBySupportId(final UUID supportId) {
        LOGGER.debug("Getting record for {}", supportId);
        if (supportId == null) {
            LOGGER.error("null supportId argument");
            return null;
        }

        ReferenceDigestRecord dbRecord;
        Transaction tx = null;
        Session session = getFactory().getCurrentSession();
        try {
            LOGGER.debug("retrieving referenceDigestRecord from db");
            tx = session.beginTransaction();
            dbRecord = (ReferenceDigestRecord) session.createCriteria(ReferenceDigestRecord.class)
                    .add(Restrictions.eq("supportRim", supportId)).uniqueResult();
            tx.commit();
        } catch (Exception ex) {
            final String msg = "unable to retrieve object";
            LOGGER.error(msg, ex);
            if (tx != null) {
                LOGGER.debug("rolling back transaction");
                tx.rollback();
            }
            throw new DBManagerException(msg, ex);
        }
        return dbRecord;
    }

    @Override
    public List<ReferenceDigestRecord> getRecordsByManufacturer(
            final ReferenceDigestRecord referenceDigestRecord) {
        LOGGER.debug("Getting records by manufacturer for {}", referenceDigestRecord);
        if (referenceDigestRecord == null) {
            LOGGER.error("null referenceDigestRecord argument");
            throw new NullPointerException("null referenceDigestRecord");
        }
        if (referenceDigestRecord.getManufacturer() == null) {
            LOGGER.error("null referenceDigestRecord manufacturer argument");
            throw new NullPointerException("null referenceDigestRecord manufacturer");
        }

        List<ReferenceDigestRecord> dbRecords = new ArrayList<>();
        String manufacturer = referenceDigestRecord.getManufacturer();
        try {
            List<ReferenceDigestRecord> dbTempList = super.getList(ReferenceDigestRecord.class);
            for (ReferenceDigestRecord rdr : dbTempList) {
                if (rdr.getManufacturer().equals(manufacturer)) {
                    dbRecords.add(rdr);
                }
            }
        } catch (DBManagerException dbMEx) {
            throw new RuntimeException(dbMEx);
        }
        return dbRecords;
    }

    @Override
    public List<ReferenceDigestRecord> getRecordsByModel(
            final ReferenceDigestRecord referenceDigestRecord) {
        LOGGER.debug("Getting records by model for {}", referenceDigestRecord);
        if (referenceDigestRecord == null) {
            LOGGER.error("null referenceDigestRecord argument");
            throw new NullPointerException("null referenceDigestRecord");
        }
        if (referenceDigestRecord.getModel() == null) {
            LOGGER.error("null referenceDigestRecord model argument");
            throw new NullPointerException("null referenceDigestRecord model");
        }

        List<ReferenceDigestRecord> dbRecords = new ArrayList<>();
        String model = referenceDigestRecord.getModel();
        try {
            List<ReferenceDigestRecord> dbTempList = super.getList(ReferenceDigestRecord.class);
            for (ReferenceDigestRecord rdr : dbTempList) {
                if (rdr.getModel().equals(model)) {
                    dbRecords.add(rdr);
                }
            }
        } catch (DBManagerException dbMEx) {
            throw new RuntimeException(dbMEx);
        }
        return dbRecords;
    }

    @Override
    public void updateRecord(final ReferenceDigestRecord referenceDigestRecord) {
        try {
            super.update(referenceDigestRecord);
        } catch (DBManagerException dbMEx) {
            throw new RuntimeException(dbMEx);
        }
    }

    /**
     * Remove a ReferenceDigestRecord from the database.
     *
     * @param referenceDigestRecord the referenceDigestRecord to delete
     * @return true if deletion was successful, false otherwise
     */
    @Override
    public boolean deleteRecord(final ReferenceDigestRecord referenceDigestRecord) {
        boolean result = false;
        LOGGER.info(String.format("Deleting reference to %s/%s",
                referenceDigestRecord.getManufacturer(), referenceDigestRecord.getModel()));
        try {
            result = super.delete(referenceDigestRecord);
        } catch (DBManagerException dbMEx) {
            throw new RuntimeException(dbMEx);
        }
        return result;
    }
}
