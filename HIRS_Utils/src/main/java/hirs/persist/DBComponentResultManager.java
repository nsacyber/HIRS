package hirs.persist;

import hirs.data.persist.certificate.ComponentResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * This class is used to persist and retrieve
 * {@link hirs.data.persist.certificate.ComponentResult}s into
 * and from the database.
 */
public class DBComponentResultManager extends DBManager<ComponentResult>
        implements ComponentResultManager {

    private static final Logger LOGGER = LogManager.getLogger(DBComponentResultManager.class);

    /**
     * Default Constructor.
     *
     * @param sessionFactory session factory used to access database connections
     */
    public DBComponentResultManager(final SessionFactory sessionFactory) {
        super(ComponentResult.class, sessionFactory);
    }

    @Override
    public ComponentResult saveResult(final ComponentResult componentResult) {
        LOGGER.debug("saving event digest value: {}", componentResult);

        try {
            return save(componentResult);
        } catch (DBManagerException dbMEx) {
            throw new RuntimeException(dbMEx);
        }
    }

    @Override
    public ComponentResult getResult(final ComponentResult componentResult) {
        LOGGER.debug("Getting record for {}", componentResult);
        if (componentResult == null) {
            LOGGER.error("null componentResult argument");
            return null;
        }

        ComponentResult dbRecord;
        Transaction tx = null;
        Session session = getFactory().getCurrentSession();
        try {
            LOGGER.debug("retrieving componentResult from db");
            tx = session.beginTransaction();
            dbRecord = (ComponentResult) session.createCriteria(ComponentResult.class)
                    .add(Restrictions.eq("componentHash",
                            componentResult.getComponentHash()))
                    .add(Restrictions.eq("certificateId",
                            componentResult.getCertificateId()))
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
    public ComponentResult getResultById(final UUID certificateId) {
        LOGGER.debug("Getting record associated with {}", certificateId);
        if (certificateId == null) {
            LOGGER.error("null certificateId argument");
            return null;
        }

        ComponentResult dbRecord;
        Transaction tx = null;
        Session session = getFactory().getCurrentSession();
        try {
            LOGGER.debug("retrieving componentResult from db");
            tx = session.beginTransaction();
            dbRecord = (ComponentResult) session.createCriteria(ComponentResult.class)
                    .add(Restrictions.eq("certificateId",
                            certificateId))
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
    public Set<ComponentResult> getComponentResultList() {
        LOGGER.debug("getting ComponentResult list");

        try {
            final List<ComponentResult> results = super.getList(ComponentResult.class);
            return new HashSet<>(results);
        } catch (DBManagerException e) {
            throw new DeviceManagerException(e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Set<ComponentResult> getComponentResultsByCertificate(final UUID certificateId) {
        LOGGER.debug("Getting record associated with {}", certificateId);
        if (certificateId == null) {
            LOGGER.error("null certificateId argument");
            return null;
        }

        Set<ComponentResult> dbRecord;
        Transaction tx = null;
        Session session = getFactory().getCurrentSession();
        try {
            LOGGER.debug("retrieving componentResult from db");
            tx = session.beginTransaction();
            dbRecord = new HashSet<ComponentResult>(session.createCriteria(ComponentResult.class)
                    .add(Restrictions.eq("certificateId",
                            certificateId)).list());
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
    public boolean deleteResult(final ComponentResult componentResult) {
        boolean result;
        LOGGER.info(String.format("Deleting component result to %s",
                componentResult.getId()));
        try {
            result = super.delete(componentResult);
        } catch (DBManagerException dbMEx) {
            throw new RuntimeException(dbMEx);
        }
        return result;
    }
}
