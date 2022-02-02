package hirs.persist;

import hirs.data.persist.BaseReferenceManifest;
import hirs.data.persist.ReferenceDigestRecord;
import hirs.data.persist.ReferenceDigestValue;
import hirs.data.persist.ReferenceManifest;
import hirs.data.persist.SupportReferenceManifest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
    public final Set<ReferenceDigestValue> getEventList() throws DeviceManagerException {
        LOGGER.debug("getting ReferenceDigestValue list");

        try {
            final List<ReferenceDigestValue> events = super.getList(ReferenceDigestValue.class);
            return new HashSet<>(events);
        } catch (DBManagerException e) {
            throw new DeviceManagerException(e);
        }
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
    public List<ReferenceDigestValue> getValueByManufacturer(final String manufacturer) {
        if (manufacturer == null) {
            LOGGER.error("null manufacturer argument");
            throw new NullPointerException("null manufacturer parameter");
        }

        List<ReferenceDigestValue> dbDigestValues = new ArrayList<>();
        try {
            List<ReferenceDigestValue> dbTempList = super.getList(ReferenceDigestValue.class);
            for (ReferenceDigestValue rdv : dbTempList) {
                if (rdv.getManufacturer().equals(manufacturer)) {
                    dbDigestValues.add(rdv);
                }
            }
        } catch (DBManagerException dbMEx) {
            throw new RuntimeException(dbMEx);
        }
        return dbDigestValues;
    }

    @Override
    public List<ReferenceDigestValue> getValueByModel(final String model) {
        if (model == null) {
            LOGGER.error("null model argument");
            throw new NullPointerException("null model parameter");
        }

        List<ReferenceDigestValue> dbDigestValues = new ArrayList<>();
        try {
            List<ReferenceDigestValue> dbTempList = super.getList(ReferenceDigestValue.class);
            for (ReferenceDigestValue rdv : dbTempList) {
                if (rdv.getModel().equals(model)) {
                    dbDigestValues.add(rdv);
                }
            }
        } catch (DBManagerException dbMEx) {
            throw new RuntimeException(dbMEx);
        }
        return dbDigestValues;
    }

    @Override
    public List<ReferenceDigestValue> getValueByManufacturerModel(
            final String manufacturer, final String model) {
        if (model == null) {
            LOGGER.error("null model argument");
            throw new NullPointerException("null model parameter");
        }
        if (manufacturer == null) {
            LOGGER.error("null manufacturer argument");
            throw new NullPointerException("null manufacturer parameter");
        }

        List<ReferenceDigestValue> dbDigestValues = new ArrayList<>();
        try {
            List<ReferenceDigestValue> dbTempList = super.getList(ReferenceDigestValue.class);
            for (ReferenceDigestValue rdv : dbTempList) {
                if (rdv.getManufacturer().equals(manufacturer)
                        && rdv.getModel().equals(model)) {
                    dbDigestValues.add(rdv);
                }
            }
        } catch (DBManagerException dbMEx) {
            throw new RuntimeException(dbMEx);
        }
        return dbDigestValues;
    }

    @Override
    public List<ReferenceDigestValue> getValuesByRecordId(
            final ReferenceDigestRecord referenceDigestRecord) {
        List<ReferenceDigestValue> dbDigestValues = new ArrayList<>(0);

        return dbDigestValues;
    }

    @Override
    public List<ReferenceDigestValue> getValuesByRimId(
            final ReferenceManifest referenceManifest) {
        LOGGER.debug("Getting digest values for {}", referenceManifest);
        if (referenceManifest == null) {
            LOGGER.error("null referenceManifest argument");
            throw new NullPointerException("null referenceManifest");
        }
        if (referenceManifest.getId() == null) {
            LOGGER.error("null referenceManifest ID argument");
            throw new NullPointerException("null referenceManifest ID");
        }

        List<ReferenceDigestValue> dbDigestValues = new ArrayList<>();
        UUID uuid = referenceManifest.getId();
        UUID rdvUuid = UUID.randomUUID();
        try {
            final List<ReferenceDigestValue> dbTempList
                    = super.getList(ReferenceDigestValue.class);
            for (ReferenceDigestValue rdv : dbTempList) {
                if (referenceManifest instanceof BaseReferenceManifest) {
                    rdvUuid = rdv.getBaseRimId();
                } else if (referenceManifest instanceof SupportReferenceManifest) {
                    rdvUuid = rdv.getSupportRimId();
                }
                if (rdvUuid.equals(uuid)) {
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
            final List<ReferenceDigestValue> dbTempList
                    = super.getList(ReferenceDigestValue.class);
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
