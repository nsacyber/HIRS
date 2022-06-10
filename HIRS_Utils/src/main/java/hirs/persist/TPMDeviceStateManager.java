package hirs.persist;

import hirs.data.persist.Device;
import hirs.data.persist.TPMDeviceState;

/**
 * Manages the device state for an TPM appraisal. See {@link TPMDeviceState} for more details.
 *
 * @see TPMDeviceState
 */
public interface TPMDeviceStateManager {

    /**
     * Stores a new <code>TPMDeviceState</code>. This stores a new <code>TPMDeviceState</code> to be
     * managed by the <code>TPMDeviceStateManager</code>. If the <code>TPMDeviceState</code> is
     * successfully saved then a reference to it is returned.
     *
     * @param state
     *            state to save
     * @return reference to saved <code>TPMDeviceState</code>
     * @throws TPMDeviceStateManagerException
     *             if the Policy has previously been saved or unexpected error occurs
     */
    TPMDeviceState saveState(TPMDeviceState state) throws TPMDeviceStateManagerException;

    /**
     * Returns the state associated with the <code>Device</code> or null if not found.
     *
     * @param device
     *            device
     * @return device state for <code>Device</code>
     * @throws TPMDeviceStateManagerException
     *             if any unexpected errors occur while trying to retrieve the state
     */
    TPMDeviceState getState(Device device) throws TPMDeviceStateManagerException;

    /**
     * Updates the state for the <code>Device</code>.
     *
     * @param state
     *            new state for the <code>Device</code>
     * @throws TPMDeviceStateManagerException
     *             if any unexpected errors occur while trying to retrieve the state
     */
    void updateState(TPMDeviceState state) throws TPMDeviceStateManagerException;

    /**
     * Removes the saved state for the <code>Device</code>. If the device state was successfully
     * found and removed then true is returned. If there was no device state currently being managed
     * by this manager then false is returned. If device state is found but unable to be deleted
     * because of unexpected errors then an <code>TPMDeviceStateManagerException</code> is thrown
     *
     * @param device
     *            device whose state is to be removed
     * @return true if successfully found state for device and deleted it, otherwise false
     * @throws TPMDeviceStateManagerException
     *             if any unexpected errors occur while trying to retrieve the state
     */
    boolean deleteState(Device device) throws TPMDeviceStateManagerException;
}
