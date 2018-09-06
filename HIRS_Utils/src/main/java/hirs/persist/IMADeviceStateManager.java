package hirs.persist;

import hirs.data.persist.Device;
import hirs.data.persist.IMADeviceState;


/**
 * Manages the device state for an IMA appraisal. See {@link IMADeviceState} for more details.
 *
 * @see IMADeviceState
 */
public interface IMADeviceStateManager {

    /**
     * Stores a new <code>IMADeviceState</code>. This stores a new <code>IMADeviceState</code> to be
     * managed by the <code>IMADeviceStateManager</code>. If the <code>IMADeviceState</code> is
     * successfully saved then a reference to it is returned.
     *
     * @param state
     *            state to save
     * @return reference to saved <code>IMADeviceState</code>
     * @throws IMADeviceStateManagerException
     *             if the Policy has previously been saved or unexpected error occurs
     */
    IMADeviceState saveState(IMADeviceState state) throws IMADeviceStateManagerException;

    /**
     * Returns the state associated with the <code>Device</code> or null if not found.
     *
     * @param device
     *            device
     * @return device state for <code>Device</code>
     * @throws IMADeviceStateManagerException
     *             if any unexpected errors occur while trying to retrieve the state
     */
    IMADeviceState getState(Device device)  throws IMADeviceStateManagerException;

    /**
     * Updates the state for the <code>Device</code>.
     *
     * @param state
     *            new state for the <code>Device</code>
     * @throws IMADeviceStateManagerException
     *             if any unexpected errors occur while trying to retrieve the state
     */
    void updateState(IMADeviceState state) throws IMADeviceStateManagerException;

    /**
     * Removes the saved state for the <code>Device</code>. If the device state was successfully
     * found and removed then true is returned. If there was no device state currently being
     * managed by this manager then false is returned. If device state is found but unable to be
     * deleted because of unexpected errors then an <code>IMADeviceStateManagerException</code> is
     * thrown
     *
     * @param device
     *            device whose state is to be removed
     * @return true if successfully found state for device and deleted it, otherwise false
     * @throws IMADeviceStateManagerException
     *             if any unexpected errors occur while trying to retrieve the state
     */
    boolean deleteState(Device device) throws IMADeviceStateManagerException;
}
