package hirs.persist;

import hirs.FilteredRecordsList;
import hirs.data.persist.BaseReferenceManifest;
import hirs.data.persist.ReferenceDigestRecord;
import hirs.data.persist.ReferenceDigestValue;
import hirs.data.persist.ReferenceManifest;
import hirs.data.persist.SupportReferenceManifest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
            List<ReferenceDigestValue> dbTempList = super.getList(ReferenceDigestValue.class);
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

    /**
     * Returns a list of all <code>Device</code>s that are ordered by a column
     * and direction (ASC, DESC) that is provided by the user.  This method
     * helps support the server-side processing in the JQuery DataTables.
     *
     * @param columnToOrder Column to be ordered
     * @param ascending direction of sort
     * @param firstResult starting point of first result in set
     * @param maxResults total number we want returned for display in table
     * @param search string of criteria to be matched to visible columns
     *
     * @return FilteredRecordsList object with fields for DataTables
     */
    @Override
    public final FilteredRecordsList<ReferenceDigestValue> getOrderedDigestValueList(
            final String columnToOrder,
            final boolean ascending, final int firstResult,
            final int maxResults, final String search) {
        if (columnToOrder == null) {
            LOGGER.debug("null object argument");
            throw new NullPointerException("object");
        }

        //Maps object types and their ability to be searched by Hibernate
        //without modification
        Map<String, Boolean> searchableColumns = new HashMap<>();
        searchableColumns.put("name", true);
        searchableColumns.put("group.name", true);
        searchableColumns.put("last_report_timestamp", false);

        CriteriaModifier modifier = new CriteriaModifier() {
            @Override
            public void modify(final Criteria criteria) {
                criteria.createAlias("valueGroup", "group");
            }
        };

        try {
            LOGGER.debug("Getting baseline list");
            return super.getOrderedList(ReferenceDigestValue.class, columnToOrder, ascending,
                    firstResult,
                    maxResults, search, searchableColumns, modifier);
        } catch (DBManagerException e) {
            throw new AlertManagerException(e);
        }
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
