package hirs.persist;

import hirs.data.persist.Device;
import hirs.data.persist.DeviceState;

import java.util.List;

/**
 * Manages the device state for an appraisal. See {@link DeviceState} for more details.
 *
 * @see DeviceState
 */
public interface DeviceStateManager {

    /**
     * Stores a new <code>DeviceState</code>. This stores a new <code>DeviceState</code> to be
     * managed by the <code>DeviceStateManager</code>. If the <code>DeviceState</code> is
     * successfully saved then a reference to it is returned.
     *
     * @param state
     *            state to save
     * @return reference to saved <code>DeviceState</code>
     * @throws DeviceStateManagerException
     *             if the Policy has previously been saved or unexpected error occurs
     */
    DeviceState saveState(DeviceState state) throws DeviceStateManagerException;

    /**
     * Returns the state associated with the <code>Device</code> or null if not found.
     *
     * @param device
     *            device
     * @param clazz
     *            Class to specify which type of <code>DeviceState</code> to retrieve
     * @return device state for <code>Device</code>
     * @throws DeviceStateManagerException
     *             if any unexpected errors occur while trying to retrieve the state
     */
    DeviceState getState(Device device, Class<? extends DeviceState> clazz)
            throws DeviceStateManagerException;

    /**
     * Returns a <code>List</code> of the <code>DeviceStates</code> associated with the
     * <code>Device</code>.  If there are no states are associated, an empty list is returned.
     *
     * @param device
     *            device
     * @return list of device states
     * @throws DeviceStateManagerException
     *             if any unexpected errors occur while trying to retrieve the states
     */
    List<DeviceState> getStates(Device device) throws DeviceStateManagerException;

    /**
     * Updates the state for the <code>Device</code>.
     *
     * @param state
     *            new state for the <code>Device</code>
     * @throws DeviceStateManagerException
     *             if any unexpected errors occur while trying to retrieve the state
     */
    void updateState(DeviceState state) throws DeviceStateManagerException;

    /**
     * Removes the saved state for the <code>Device</code>. If the device state was successfully
     * found and removed then true is returned. If there was no device state currently being managed
     * by this manager then false is returned. If device state is found but unable to be deleted
     * because of unexpected errors then an <code>DeviceStateManagerException</code> is thrown
     *
     * @param device
     *            device whose state is to be removed
     * @param clazz
     *            Class to specify which type of <code>DeviceState</code> to retrieve
     * @return true if successfully found state for device and deleted it, otherwise false
     * @throws DeviceStateManagerException
     *             if any unexpected errors occur while trying to retrieve the state
     */
    boolean deleteState(Device device, Class<? extends DeviceState> clazz)
            throws DeviceStateManagerException;

}
