package hirs.persist;

import java.util.List;

import org.hibernate.SessionFactory;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import hirs.data.persist.alert.AlertMonitor;

/**
 * A <code>DBAlertMonitorManager</code> is a service (extends <code>DBManager</code>) that
 * implements the <code>AlertMonitorManager</code> that stores and retrieves Alert Monitors.
 */
public class DBAlertMonitorManager
        extends DBManager<AlertMonitor> implements AlertMonitorManager {

    private static final Logger LOGGER = LogManager.getLogger(DBAlertMonitorManager.class);

    /**
     * Creates the DBAlertMonitoringManager.
     *
     * @param sessionFactory session factory used to access database connections
     */
    public DBAlertMonitorManager(final SessionFactory sessionFactory) {
        super(AlertMonitor.class, sessionFactory);
    }

    /**
     * Saves the <code>AlertMonitor</code> in the database. This creates a new database
     * session and saves the AlertMonitor. If the <code>AlertMonitor</code> had previously
     * been saved then a <code>AlertMonitorManagerException</code> is thrown.
     *
     * @param monitor AlertMonitor to save
     * @return reference to saved AlertMonitor
     * @throws AlertMonitorManagerException if AlertMonitor has previously been saved or an
 error occurs while trying to save it to the database
     */
    @Override
    public final AlertMonitor saveAlertMonitor(final AlertMonitor monitor)
            throws AlertMonitorManagerException {
        LOGGER.debug("saving Alert Monitor {}", monitor);
        try {
            return super.save(monitor);
        } catch (DBManagerException e) {
            throw new AlertMonitorManagerException(e);
        }
    }

    /**
     * Updates an <code>AlertMonitor</code>. This updates the database entries to reflect the new
     * values that should be set.
     *
     * @param monitor AlertMonitor
     * @throws AlertMonitorManagerException if AlertMonitor has not previously been saved or
 an error occurs while trying to save it to the database
     */
    @Override
    public final void updateAlertMonitor(final AlertMonitor monitor)
            throws AlertMonitorManagerException {
        LOGGER.debug("updating Alert Monitor: {}", monitor);
        try {
            super.update(monitor);
        } catch (DBManagerException e) {
            throw new AlertMonitorManagerException(e);
        }
    }

    /**
     * Returns a list of all <code>AlertMonitor</code>s of type <code>clazz</code>. This
     * searches through the database for this information.
     *
     * @param clazz class type of <code>AlertMonitor</code>s to return (may be null)
     * @return list of <code>AlertMonitor</code> names
     * @throws AlertMonitorManagerException if unable to retrieve the list
     */
    @Override
    public final List<AlertMonitor> getAlertMonitorList(final Class<? extends AlertMonitor> clazz)
            throws AlertMonitorManagerException {
        LOGGER.debug("Getting Alert Monitor list");
        final List<AlertMonitor> monitors;
        try {
            monitors = super.getList(clazz);
        } catch (DBManagerException e) {
            throw new AlertMonitorManagerException(e);
        }
        LOGGER.debug("Got {} Alert Monitors", monitors.size());
        return monitors;
    }

    /**
     * Retrieves the <code>AlertMonitor</code> from the database. This searches the database for an
     * entry whose name matches <code>name</code>. It then reconstructs a <code>AlertMonitor</code>
     * object from the database entry.
     *
     * @param name name of the AlertMonitor
     * @return AlertMonitor if found, otherwise null.
     * @throws AlertMonitorManagerException if unable to search the database or recreate the
     * <code>AlertMonitor</code>
     */
    @Override
    public final AlertMonitor getAlertMonitor(final String name)
            throws AlertMonitorManagerException {
        LOGGER.debug("getting Alert Monitor: {}", name);
        try {
            return super.get(name);
        } catch (DBManagerException e) {
            throw new AlertMonitorManagerException(e);
        }
    }

    /**
     * Deletes the <code>AlertMonitor</code> from the database. This removes all of the database
     * entries that stored information with regards to the this <code>AlertMonitor</code>.
     * Currently, iterates over <code>Policy</code> entries and removes the selected
     * <code>AlertMonitor</code> from them if it exists. This needs to be fixed as this should not
     * be performed without user action. Update is expected soon.
     *
     * @param name name of the <code>AlertMonitor</code> to delete
     * @return true if successfully found and deleted <code>AlertMonitor</code>
     * @throws AlertMonitorManagerException if unable to find the AlertMonitor or delete it
 from the database
     */
    @Override
    public final boolean deleteAlertMonitor(final String name)
            throws AlertMonitorManagerException {
        LOGGER.debug("deleting Alert Monitor: {}", name);
        try {
            return super.delete(name);
        } catch (DBManagerException e) {
            throw new AlertMonitorManagerException(e);
        }
    }
}
