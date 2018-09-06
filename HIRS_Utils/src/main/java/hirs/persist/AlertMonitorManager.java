package hirs.persist;

import hirs.data.persist.alert.AlertMonitor;

import java.util.List;

/**
 * An <code>AlertMonitorManager</code> manages <code>AlertMonitor</code>. An
 * <code>AlertMonitorManager</code> is used to store and manage AlertMonitors.
 * It has support for the basic create, read, update, and delete methods.
 */
public interface AlertMonitorManager {

    /**
     * Stores a new <code>AlertMonitor</code>. This stores a new
     * <code>AlertMonitor</code> to be managed by the
     * <code>AlertMonitorManager</code>. If the <code>AlertMonitor</code> is
     * successfully saved then a reference to it is returned.
     *
     * @param monitor
     *            the AlertMonitor to save AlertMonitor to save
     * @return reference to saved AlertMonitor
     * @throws AlertMonitorManagerException
     *             if the AlertMonitor has previously been saved or unexpected
     *             error occurs
     */
    AlertMonitor saveAlertMonitor(AlertMonitor monitor)
            throws AlertMonitorManagerException;

    /**
     * Updates a <code>AlertMonitor</code>. This updates the
     * <code>AlertMonitor</code> that is managed so subsequent calls to get this
     * <code>AlertMonitor</code> will return the values set by the incoming
     * <code>AlertMonitor</code>.
     *
     * @param alertMonitor
     *            AlertMonitor
     * @throws AlertMonitorManagerException
     *             if unable to update the AlertMonitor
     */
    void updateAlertMonitor(AlertMonitor alertMonitor)
            throws AlertMonitorManagerException;

    /**
     * Returns a list of all AlertMonitor names managed by this manager. Every
     * <code>AlertMonitor</code> must have a name that users can use to
     * reference the <code>AlertMonitor</code>. This returns a listing of all
     * the <code>AlertMonitor</code>s.
     * <p>
     * A <code>Class</code> argument may be specified to limit which types of
     * <code>AlertMonitor</code>s to return. This argument may be null to return
     * all <code>AlertMonitor</code>s.
     *
     * @param clazz class type of <code>AlertMonitor</code>s to return (may be null)
     * @return list of <code>AlertMonitor</code> names
     * @throws AlertMonitorManagerException
     *             if unable to create the list
     */
    List<AlertMonitor> getAlertMonitorList(Class<? extends AlertMonitor> clazz)
            throws AlertMonitorManagerException;

    /**
     * Retrieves the <code>AlertMonitor</code> identified by <code>name</code>.
     * If the <code>AlertMonitor</code> cannot be found then null is returned.
     *
     * @param name
     *            name of the <code>AlertMonitor</code>
     * @return <code>AlertMonitor</code> whose name is <code>name</code> or null
     *         if not found
     * @throws AlertMonitorManagerException
     *             if unable to retrieve the AlertMonitor
     */
    AlertMonitor getAlertMonitor(String name)
            throws AlertMonitorManagerException;

    /**
     * Deletes the <code>AlertMonitor</code> identified by <code>name</code>. If
     * the <code>AlertMonitor</code> is found and deleted then true is returned,
     * otherwise false.
     *
     * @param name
     *            name of the <code>AlertMonitor</code> to delete
     * @return true if successfully found and deleted from repo, otherwise false
     * @throws AlertMonitorManagerException
     *             if unable to delete the AlertMonitor for any reason other
     *             than not found
     */
    boolean deleteAlertMonitor(String name) throws AlertMonitorManagerException;
}
