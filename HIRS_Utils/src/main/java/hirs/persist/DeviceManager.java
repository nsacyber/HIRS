package hirs.persist;

import hirs.FilteredRecordsList;
import hirs.data.persist.Device;

import java.util.List;
import java.util.Set;

/**
 * A <code>DeviceManager</code> manages <code>Device</code>s. A
 * <code>DeviceManager</code> is used to store and manage devices. It has
 * support for the basic create, read, update, and delete methods.
 */
public interface DeviceManager extends OrderedListQuerier<Device> {

    /**
     * Stores a new <code>Device</code>. This stores a new
     * <code>Device</code> to be managed by the <code>DeviceManager</code>.
     * If the <code>Device</code> is successfully saved then a reference to it
     * is returned.
     *
     * @param device
     *            device to save
     * @return reference to saved device
     * @throws DeviceManagerException
     *             if the device has previously been saved or unexpected error
     *             occurs
     */
    Device saveDevice(Device device) throws DeviceManagerException;

    /**
     * Updates a <code>Device</code>. This updates the <code>Device</code>
     * that is managed so subsequent calls to get this <code>Device</code>
     * will return the values set by the incoming <code>Device</code>.
     *
     * @param device
     *            device
     * @throws DeviceManagerException
     *             if unable to update the device
     */
    void updateDevice(Device device) throws DeviceManagerException;

    /**
     * Updates list of <code>Device</code>s. This updates the database entries
     * to reflect the new values that should be set.  Commonly used when
     * deleting a DeviceGroup.
     *
     * @param deviceList
     *            list of devices that should be updated in single transaction
     * @throws DeviceManagerException
     *             if device has not previously been saved or an error occurs
     *             while trying to save it to the database
     */
    void updateDeviceList(Set<Device> deviceList)
            throws DeviceManagerException;

    /**
     * Returns a list of all <code>Devices</code>. This searches through
     * the database for this information.
     *
     * @return list of <code>Devices</code>
     * @throws DeviceManagerException
     *             if unable to search the database
     */
    Set<Device> getDeviceList() throws DeviceManagerException;

    /**
     * Returns a list of all device names managed by this manager. Every
     * <code>Device</code> must have a name that users can use to reference
     * the <code>Device</code>. This returns a listing of all the
     * <code>Device</code> names.
     *
     * @return list of <code>Device</code> names
     * @throws DeviceManagerException
     *             if unable to create the list
     */
    List<String> getDeviceNameList()
            throws DeviceManagerException;

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
     * @throws DeviceManagerException
     *          if unable to create the list
     */
    FilteredRecordsList<Device> getOrderedDeviceList(
            String columnToOrder, boolean ascending, int firstResult,
            int maxResults, String search)
            throws DeviceManagerException;
    /**
     * Retrieves the <code>Device</code> identified by <code>name</code>. If
     * the <code>Device</code> cannot be found then null is returned.
     *
     * @param name
     *            name of the <code>Device</code>
     * @return <code>Device</code> whose name is <code>name</code> or null if
     *         not found
     * @throws DeviceManagerException
     *             if unable to retrieve the device
     */
    Device getDevice(String name) throws DeviceManagerException;

    /**
     * Used to produce a list of all <code>Device</code>s associated with the Default Group.
     *
     * @return list of Devices that are part of the Default Group
     * @throws DeviceManagerException
     *      if unable to find the device or delete it from the database
     */
    List<Device> getDefaultDevices() throws DeviceManagerException;

    /**
     * Delete the <code>Device</code> identified by <code>name</code>. If
     * the deletion is successful, true is returned. Otherwise, false is
     * returned.
     *
     * @param name of the <code>Device</code> to delete
     * @return boolean indicating outcome of the deletion
     * @throws DeviceManagerException if unable to delete the device group
     */
    boolean deleteDevice(String name) throws DeviceManagerException;

}
