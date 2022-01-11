package hirs.persist;

import hirs.data.persist.ReferenceDigestRecord;
import hirs.data.persist.ReferenceDigestValue;
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
 * This class is used to persist and retrieve {@link hirs.data.persist.ReferenceDigestValue}s into
 * and from the database.
 */
public class DBReferenceEventManager  extends DBManager<ReferenceDigestValue>
        implements ReferenceEventManager {

    private static final Logger LOGGER = LogManager.getLogger(DBReferenceDigestManager.class);

    /**
     * Default Constructor.
     *
     * @param sessionFactory session factory used to access database connections
     */
    public DBReferenceEventManager(final SessionFactory sessionFactory) {
        super(ReferenceDigestValue.class, sessionFactory);
    }
    @Override
    public ReferenceDigestValue saveValue(final ReferenceDigestValue referenceDigestValue) {
        LOGGER.debug("saving event digest value: {}", referenceDigestValue);
        try {
            return save(referenceDigestValue);
        } catch (DBManagerException dbMEx) {
            throw new RuntimeException(dbMEx);
        }
    }

    @Override
    public ReferenceDigestValue getValue(final ReferenceDigestValue referenceDigestValue) {
        LOGGER.debug("Getting record for {}", referenceDigestValue);
        if (referenceDigestValue == null) {
            LOGGER.error("null referenceDigestValue argument");
            return null;
        }

        if (referenceDigestValue.getSupportRimId() == null
                || referenceDigestValue.getDigestValue() == null
                || referenceDigestValue.getPcrIndex() == -1) {
            LOGGER.error("No reference to get record from db {}", referenceDigestValue);
            return null;
        }

        ReferenceDigestValue dbRecord;
        Transaction tx = null;
        Session session = getFactory().getCurrentSession();
        try {
            LOGGER.debug("retrieving referenceDigestValue from db");
            tx = session.beginTransaction();
            dbRecord = (ReferenceDigestValue) session.createCriteria(ReferenceDigestValue.class)
                    .add(Restrictions.eq("supportRimId",
                            referenceDigestValue.getSupportRimId()))
                    .add(Restrictions.eq("digestValue",
                            referenceDigestValue.getDigestValue()))
                    .add(Restrictions.eq("eventNumber",
                            referenceDigestValue.getPcrIndex()))
                    .uniqueResult();
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
    public ReferenceDigestValue getValueById(final ReferenceDigestValue referenceDigestValue) {
        LOGGER.debug("Getting record for {}", referenceDigestValue);
        if (referenceDigestValue == null) {
            LOGGER.error("null referenceDigestValue argument");
            return null;
        }

        if (referenceDigestValue.getId() == null) {
            LOGGER.error("No reference to get record from db {}", referenceDigestValue);
            return null;
        }

        ReferenceDigestValue dbRecord;
        Transaction tx = null;
        Session session = getFactory().getCurrentSession();
        try {
            LOGGER.debug("retrieving referenceDigestValue from db");
            tx = session.beginTransaction();
            dbRecord = (ReferenceDigestValue) session.createCriteria(ReferenceDigestValue.class)
                    .add(Restrictions.eq("id",
                            referenceDigestValue.getId())).uniqueResult();
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
    public List<ReferenceDigestValue> getValuesByRecordId(
            final ReferenceDigestRecord referenceDigestRecord) {
        LOGGER.debug("Getting digest values for {}", referenceDigestRecord);
        if (referenceDigestRecord == null) {
            LOGGER.error("null referenceDigestRecord argument");
            throw new NullPointerException("null referenceDigestRecord");
        }
        if (referenceDigestRecord.getId() == null) {
            LOGGER.error("null referenceDigestRecord ID argument");
            throw new NullPointerException("null referenceDigestRecord ID");
        }

        List<ReferenceDigestValue> dbDigestValues = new ArrayList<>();
        UUID uuid = referenceDigestRecord.getId();
        try {
            List<ReferenceDigestValue> dbTempList = super.getList(ReferenceDigestValue.class);
            for (ReferenceDigestValue rdv : dbTempList) {
                if (rdv.getSupportRimId().equals(uuid)) {
                    dbDigestValues.add(rdv);
                }
            }
        } catch (DBManagerException dbMEx) {
            throw new RuntimeException(dbMEx);
        }
        return dbDigestValues;
    }

    @Override
    public List<ReferenceDigestValue> getValueByEventType(final String eventType) {
        LOGGER.debug("Getting digest values for event type: {}", eventType);
        if (eventType == null) {
            LOGGER.error("null event type argument");
            throw new NullPointerException("null event type");
        }

        List<ReferenceDigestValue> dbDigestValues = new ArrayList<>();
        try {
            List<ReferenceDigestValue> dbTempList = super.getList(ReferenceDigestValue.class);
            for (ReferenceDigestValue rdv : dbTempList) {
                if (rdv.getEventType().equals(eventType)) {
                    dbDigestValues.add(rdv);
                }
            }
        } catch (DBManagerException dbMEx) {
            throw new RuntimeException(dbMEx);
        }
        return dbDigestValues;
    }

    @Override
    public void updateRecord(final ReferenceDigestValue referenceDigestValue) {
        try {
            super.update(referenceDigestValue);
        } catch (DBManagerException dbMEx) {
            throw new RuntimeException(dbMEx);
        }
    }

    @Override
    public boolean deleteRecord(final ReferenceDigestValue referenceDigestValue) {
        boolean result;
        LOGGER.info(String.format("Deleting reference to %s",
                referenceDigestValue.getId()));
        try {
            result = super.delete(referenceDigestValue);
        } catch (DBManagerException dbMEx) {
            throw new RuntimeException(dbMEx);
        }
        return result;
    }
}
