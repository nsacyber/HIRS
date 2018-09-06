package hirs.persist;

import hirs.FilteredRecordsList;
import java.util.Set;

import hirs.data.persist.DeviceGroup;
import hirs.data.persist.Policy;

/**
 * A DeviceGroupManager manages DeviceGroups. It is used to store and manage
 * device groups. It has support for the basic create, read, update, and delete
 * methods.
 */
public interface DeviceGroupManager {

    /**
     * Stores a new <code>DeviceGroup</code> to be managed by the
     * <code>DeviceGroupManager</code>. If the <code>DeviceGroup</code> is
     * successfully saved, then a reference to it is returned.
     *
     * @param deviceGroup
     *            device group to save
     * @return reference to the saved device group
     * @throws DeviceGroupManagerException
     *             if the device group has been previously saved or an
     *             unexpected error occurs
     */
    DeviceGroup saveDeviceGroup(DeviceGroup deviceGroup)
            throws DeviceGroupManagerException;

    /**
     * Updates a <code>DeviceGroup</code> that is managed so subsequent calls to
     * get this <code>DeviceGroup</code> will return the values set by the
     * incoming <code>DeviceGroup</code>.
     *
     * @param deviceGroup
     *            device group to be updated
     * @throws DeviceGroupManagerException
     *             if unable to update the device group
     */
    void updateDeviceGroup(DeviceGroup deviceGroup)
            throws DeviceGroupManagerException;

    /**
     * Returns a set of all device groups managed by this manager. Every
     * <code>DeviceGroup</code> must have a name that users can use to reference
     * the <code>DeviceGroup</code>.
     *
     * @return a set containing the device groups
     * @throws DeviceGroupManagerException
     *             if unable to create set
     */
    Set<DeviceGroup> getDeviceGroupSet() throws DeviceGroupManagerException;

    /**
     * Retrieves the <code>DeviceGroup</code> identified by <code>name</code>.
     * If the <code>DeviceGroup</code> cannot be found, then null is returned.
     *
     * @param name
     *            name of the <code>DeviceGroup</code>
     * @return <code>DeviceGroup</code> or null if not found
     * @throws DeviceGroupManagerException
     *             if unable to retrieve the device group
     */
    DeviceGroup getDeviceGroup(String name) throws DeviceGroupManagerException;

    /**
     * Checks whether or not a {@link Policy} is currently associated with
     * a group.  The only instance at this time makes a determination whether
     * or not the provided Policy is safe for deletion.
     *
     * @param policy
     *      {@link Policy} that has been selected for deletion.
     * @return
     *      whether or not the provided policy is the member of a group
     * @throws DeviceGroupManagerException
     *             if policy is null or unable to return query {@link Policy}
     */
    Set<DeviceGroup> getGroupsAssignedToPolicy(Policy policy)
            throws DeviceGroupManagerException;

    /**
     * Delete the <code>DeviceGroup</code> identified by <code>name</code>. If
     * the deletion is successful, true is returned. Otherwise, false is
     * returned.
     *
     * @param name
     *            name of the <code>DeviceGroup</code> to delete
     * @return boolean indicating outcome of the deletion
     * @throws DeviceGroupManagerException
     *             if unable to delete the device group
     */
    boolean deleteDeviceGroup(String name) throws DeviceGroupManagerException;

    /**
     * Returns a list of all <code>DeviceGroup</code>s that are ordered by a column
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
     * @throws DeviceGroupManagerException
     *          if unable to create the list
     */
    FilteredRecordsList<DeviceGroup> getOrderedDeviceGroupList(
            String columnToOrder, boolean ascending, int firstResult,
            int maxResults, String search)
            throws DeviceGroupManagerException;
}
