package hirs.persist;

import org.hibernate.SessionFactory;

import hirs.data.persist.Device;
import hirs.data.persist.TPMDeviceState;

/**
 * A <code>DBTPMDeviceStateManager</code> manages <code>TPMDeviceState</code> objects using a
 * database.
 */
public class DBTPMDeviceStateManager extends DBDeviceStateManager implements TPMDeviceStateManager {

    /**
     * Creates a new <code>DBTPMDeviceStateManager</code>. The optional SessionFactory parameter is
     * used to initialize a session factory to manage all hibernate sessions.
     *
     * @param factory
     *            session factory to manage connections to hibernate db
     */
    public DBTPMDeviceStateManager(final SessionFactory factory) {
        super(factory);
    }

    /**
     * Saves the <code>TPMDeviceState</code> in the database and returns it.
     *
     * @param state
     *            state to save
     * @return <code>TPMDeviceState</code> that was saved
     * @throws TPMDeviceStateManagerException
     *             if state has previously been saved or an error occurs while trying to save it to
     *             the database
     */
    @Override
    public final TPMDeviceState saveState(final TPMDeviceState state)
            throws TPMDeviceStateManagerException {
        try {
            return (TPMDeviceState) super.saveState(state);
        } catch (DeviceStateManagerException e) {
            throw new TPMDeviceStateManagerException(e);
        }
    }

    /**
     * Returns the <code>TPMDeviceState</code> for a <code>Device</code>.
     *
     * @param device
     *            device
     * @return state
     * @throws TPMDeviceStateManagerException
     *             if state has not previously been saved or an error occurs while trying to
     *             retrieve it from the database
     */
    @Override
    public final TPMDeviceState getState(final Device device)
            throws TPMDeviceStateManagerException {
        try {
            return (TPMDeviceState) super.getState(device, TPMDeviceState.class);
        } catch (DeviceStateManagerException e) {
            throw new TPMDeviceStateManagerException(e);
        }
    }

    /**
     * Updates an <code>TPMDeviceState</code>. This updates the database entries to reflect the new
     * values that should be set.
     *
     * @param state
     *            state
     * @throws TPMDeviceStateManagerException
     *             if state has not previously been saved or an error occurs while trying to save it
     *             to the database
     */
    @Override
    public final void updateState(final TPMDeviceState state)
            throws TPMDeviceStateManagerException {
        try {
            super.updateState(state);
        } catch (DeviceStateManagerException e) {
            throw new TPMDeviceStateManagerException(e);
        }
    }

    /**
     * Deletes the <code>TPMDeviceState</code> from the database.
     *
     * @param device
     *            device whose state is to be remove
     * @return true if successfully found and deleted the <code>TPMDeviceState</code>
     * @throws TPMDeviceStateManagerException
     *             if any unexpected errors occur while trying to delete it from the database
     */
    @Override
    public final boolean deleteState(final Device device) throws TPMDeviceStateManagerException {
        try {
            return super.deleteState(device, TPMDeviceState.class);
        } catch (DeviceStateManagerException e) {
            throw new TPMDeviceStateManagerException(e);
        }
    }

}
