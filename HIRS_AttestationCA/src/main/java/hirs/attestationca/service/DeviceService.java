package hirs.attestationca.service;

import hirs.data.persist.Device;
import hirs.persist.DeviceManagerException;

import java.util.List;
import java.util.UUID;

/**
 * A <code>DeviceService</code> manages <code>Device</code>s. A
 * <code>DeviceService</code> is used to store and manage devices. It has
 * support for the basic create, read, update, and delete methods.
 */
public interface DeviceService {

    /**
     * Saves the <code>Device</code> in the database. This creates a new
     * database session and saves the device. If the <code>Device</code> had
     * previously been saved then a <code>DeviceManagerException</code> is
     * thrown.
     *
     * @param device
     *            device to save
     * @return reference to saved device
     * @throws hirs.persist.DeviceManagerException
     *             if device has previously been saved or an error occurs
     *             while trying to save it to the database
     */
    Device saveDevice(Device device) throws DeviceManagerException;

    /**
     * Returns a list of all <code>Devices</code>. This searches through
     * the database for this information.
     *
     * @return list of <code>Devices</code>
     * @throws DeviceManagerException
     *             if unable to search the database
     */
    List<Device> getDeviceList();

    /**
     * Updates a <code>Device</code>. This updates the database entries to
     * reflect the new values that should be set.
     *
     * @param device device object to save
     * @param deviceId UUID for the database object
     * @throws DeviceManagerException
     *             if device has not previously been saved or an error occurs
     *             while trying to save it to the database
     * @return a device object
     */
    Device updateDevice(Device device, UUID deviceId) throws DeviceManagerException;

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
    void updateDeviceList(List<Device> deviceList) throws DeviceManagerException;

    /**
     * Deletes the <code>Device</code> from the database. This removes all
     * of the database entries that stored information with regards to the
     * <code>Device</code> with a foreign key relationship.
     *
     * @param deviceId of the device to be deleted
     * @throws DeviceManagerException
     *             if unable to find the device group or delete it from the
     *             database
     */
    void deleteDeviceById(UUID deviceId) throws DeviceManagerException;
}
