package hirs.persist;

import java.util.List;

import org.hibernate.SessionFactory;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import hirs.data.persist.alert.AlertServiceConfig;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;

/**
 * A <code>DBAlertServiceManager</code> is a service (extends <code>DBManager</code>) that
 * implements the <code>AlertServiceConfigManager</code> that stores and retrieves Alert Services.
 */
public class DBAlertServiceManager
        extends DBManager<AlertServiceConfig> implements AlertServiceConfigManager {

    private static final Logger LOGGER = LogManager.getLogger(DBAlertServiceManager.class);

    /**
     * Creates a new <code>DBReportSummaryManager</code>. The optional SessionFactory parameter is
     * used to manage sessions with a hibernate db.
     *
     * @param factory a hibernate session factory
     */
    public DBAlertServiceManager(final SessionFactory factory) {
        super(AlertServiceConfig.class, factory);
    }

    /**
     * Saves the <code>AlertServiceConfig</code> in the database. This creates a new database
     * session and saves the AlertServiceConfig. If the <code>AlertServiceConfig</code> had
     * previously been saved then a <code>AlertServiceManagerException</code> is thrown.
     *
     * @param serviceConfig AlertServiceConfig to save
     * @return reference to saved AlertServiceConfig
     * @throws AlertServiceConfigManagerException if baseline has previously been saved or an error
     * occurs while trying to save it to the database
     */
    @Override
    public final AlertServiceConfig saveAlertServiceConfig(final AlertServiceConfig serviceConfig)
            throws AlertServiceConfigManagerException {
        LOGGER.debug("saving Alert Monitor {}", serviceConfig);
        try {
            return super.save(serviceConfig);
        } catch (DBManagerException e) {
            throw new AlertServiceConfigManagerException(e);
        }
    }

    /**
     * Updates a <code>AlertServiceConfig</code>. This updates the database entries to reflect the
     * new values that should be set.
     *
     * @param serviceConfig baseline
     * @throws AlertServiceConfigManagerException if baseline has not previously been saved or an
     * error occurs while trying to save it to the database
     */
    @Override
    public final void updateAlertServiceConfig(final AlertServiceConfig serviceConfig)
            throws AlertServiceConfigManagerException {
        LOGGER.debug("updating Alert Monitor: {}", serviceConfig);
        try {
            super.update(serviceConfig);
        } catch (DBManagerException e) {
            throw new AlertServiceConfigManagerException(e);
        }
    }

    /**
     * Returns a list of all <code>AlertServiceConfig</code>s of type <code>clazz</code>. This
     * searches through the database for this information.
     *
     * @param clazz class type of <code>AlertServiceConfig</code>s to return (may be null)
     * @return list of <code>AlertServiceConfig</code> names
     * @throws AlertServiceConfigManagerException if unable to retrieve the list
     */
    @Override
    public final List<AlertServiceConfig> getAlertServiceConfigList(
            final Class<? extends AlertServiceConfig> clazz)
            throws AlertServiceConfigManagerException {
        LOGGER.debug("Getting Alert Service Config list");
        final List<AlertServiceConfig> serviceConfigs;
        try {
            serviceConfigs = super.getList(clazz);
        } catch (DBManagerException e) {
            throw new BaselineManagerException(e);
        }
        LOGGER.debug("Got {} Alert Monitors", serviceConfigs.size());
        return serviceConfigs;
    }

    /**
     * Retrieves the <code>AlertServiceConfig</code> from the database. This searches the database
     * for an entry whose name matches <code>name</code>. It then reconstructs a
     * <code>AlertServiceConfig</code> object from the database entry.
     *
     * @param type type of the AlertServiceConfig
     * @return baseline if found, otherwise null.
     * @throws AlertServiceConfigManagerException if unable to search the database or recreate the
     * <code>AlertServiceConfig</code>
     */
    @Override
    public final AlertServiceConfig getAlertServiceConfig(final String type)
            throws AlertServiceConfigManagerException {
        if (type == null) {
            LOGGER.debug("null name argument");
            return null;
        }
        LOGGER.debug("getting Alert Monitor: {}", type);

        AlertServiceConfig ret = null;
        Transaction tx = null;
        Session session = getFactory().getCurrentSession();
        try {
            LOGGER.debug("retrieving AlertServiceConfig from db");
            tx = session.beginTransaction();
            ret = (AlertServiceConfig) session.createCriteria(AlertServiceConfig.class)
                    .add(Restrictions.eq("type", type)).uniqueResult();
            tx.commit();
        } catch (Exception e) {
            final String msg = "unable to retrieve object";
            LOGGER.error(msg, e);
            if (tx != null) {
                LOGGER.debug("rolling back transaction");
                tx.rollback();
            }
            return null;
        }
        return ret;
    }

    /**
     * Deletes the <code>AlertServiceConfig</code> from the database. This removes all of the
     * database entries that stored information with regards to the this
     * <code>AlertServiceConfig</code>. Currently, iterates over <code>Policy</code> entries and
     * removes the selected <code>AlertServiceConfig</code> from them if it exists. This needs to be
     * fixed as this should not be performed without user action. Update is expected soon.
     *
     * @param type type of the <code>AlertServiceConfig</code> to delete
     * @return true if successfully found and deleted <code>AlertServiceConfig</code>
     * @throws AlertServiceConfigManagerException if unable to find the baseline or delete it from
     * the database
     */
    @Override
    public final boolean deleteAlertServiceConfig(final String type)
            throws AlertServiceConfigManagerException {
        if (type == null) {
            LOGGER.debug("null name argument");
            return false;
        }
        LOGGER.debug("deleting Alert Monitor: {}", type);

        boolean deleted = false;
        AlertServiceConfig object = null;
        Transaction tx = null;
        Session session = getFactory().getCurrentSession();
        try {
            LOGGER.debug("retrieving object from db");
            tx = session.beginTransaction();
            object = (AlertServiceConfig) session.createCriteria(AlertServiceConfig.class)
                    .add(Restrictions.eq("type", type)).uniqueResult();
            if (object != null) {
                LOGGER.debug("found object, deleting it");
                session.delete(object);
                deleted = true;
            }
            tx.commit();
        } catch (Exception e) {
            final String msg = "unable to retrieve object";
            LOGGER.error(msg, e);
            if (tx != null) {
                LOGGER.debug("rolling back transaction");
                tx.rollback();
            }
            return false;
        }
        return deleted;
    }
}
