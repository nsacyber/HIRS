package hirs.persist;

import hirs.data.persist.alert.AlertServiceConfig;

import java.util.List;

/**
 * An <code>AlertServiceManager</code> manages <code>AlertServiceConfig</code>s. An
 * <code>AlertServiceManager</code> is used to store and manage AlertServiceConfigs. It has support
 * for the basic create, read, update, and delete methods.
 */
public interface AlertServiceConfigManager {

    /**
     * Stores a new <code>AlertServiceConfig</code>. This stores a new
     * <code>AlertServiceConfig</code> to be managed by the <code>AlertServiceManager</code>. If the
     * <code>AlertServiceConfig</code> is successfully saved then a reference to it is returned.
     *
     * @param service the AlertServiceConfig to save AlertServiceConfig to save
     * @return reference to saved AlertServiceConfig
     * @throws AlertServiceConfigManagerException if the AlertServiceConfig has previously been
     * saved or unexpected error occurs
     */
    AlertServiceConfig saveAlertServiceConfig(AlertServiceConfig service)
            throws AlertServiceConfigManagerException;

    /**
     * Updates a <code>AlertServiceConfig</code>. This updates the <code>AlertServiceConfig</code>
     * that is managed so subsequent calls to get this <code>AlertServiceConfig</code> will return
     * the values set by the incoming <code>AlertServiceConfig</code>.
     *
     * @param serviceConfig the AlertServiceConfig
     * @throws AlertServiceConfigManagerException if unable to update the AlertServiceConfig
     */
    void updateAlertServiceConfig(AlertServiceConfig serviceConfig)
            throws AlertServiceConfigManagerException;

    /**
     * Returns a list of all AlertServiceConfig names managed by this manager. Every
     * <code>AlertServiceConfig</code> must have a name that users can use to reference the
     * <code>AlertServiceConfig</code>. This returns a listing of all the
     * <code>AlertServiceConfig</code>s.
     * <p>
     * A <code>Class</code> argument may be specified to limit which types of
     * <code>AlertServiceConfig</code>s to return. This argument may be null to return all
     * <code>AlertServiceConfig</code>s.
     *
     * @param clazz class type of <code>AlertServiceConfig</code>s to return (may be null)
     * @return list of <code>AlertServiceConfig</code> names
     * @throws AlertServiceConfigManagerException if unable to create the list
     */
    List<AlertServiceConfig>
            getAlertServiceConfigList(Class<? extends AlertServiceConfig> clazz)
            throws AlertServiceConfigManagerException;

    /**
     * Retrieves the <code>AlertServiceConfig</code> identified by <code>name</code>. If the
     * <code>AlertServiceConfig</code> cannot be found then null is returned.
     *
     * @param type type of the <code>AlertServiceConfig</code>
     * @return <code>AlertServiceConfig</code> whose name is <code>name</code> or null if not found
     * @throws AlertServiceConfigManagerException if unable to retrieve the AlertServiceConfig
     */
    AlertServiceConfig getAlertServiceConfig(String type)
            throws AlertServiceConfigManagerException;

    /**
     * Deletes the <code>AlertServiceConfig</code> identified by <code>name</code>. If the
     * <code>AlertServiceConfig</code> is found and deleted then true is returned, otherwise false.
     *
     * @param type type of the <code>AlertServiceConfig</code> to delete
     * @return true if successfully found and deleted from repo, otherwise false
     * @throws AlertServiceConfigManagerException if unable to delete the AlertServiceConfig for any
     * reason other than not found
     */
    boolean deleteAlertServiceConfig(String type)
            throws AlertServiceConfigManagerException;
}
