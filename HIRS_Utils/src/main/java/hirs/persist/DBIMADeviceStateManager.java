package hirs.persist;

import org.hibernate.SessionFactory;

import hirs.data.persist.Device;
import hirs.data.persist.IMADeviceState;

/**
 * A <code>DBIMADeviceStateManager</code> manages <code>IMADeviceState</code> objects using a
 * database.
 */
public class DBIMADeviceStateManager extends DBDeviceStateManager implements IMADeviceStateManager {

    /**
     * Creates a new <code>DBIMADeviceStateManager</code>. The optional SessionFactory parameter
     * is used to initialize a session factory to manage all hibernate sessions.
     *
     * @param factory session factory to manage connections to hibernate db
     */
    public DBIMADeviceStateManager(final SessionFactory factory) {
        super(factory);
    }

    /**
     * Saves the <code>IMADeviceState</code> in the database and returns it.
     *
     * @param state
     *            state to save
     * @return <code>IMADeviceState</code> that was saved
     * @throws IMADeviceStateManagerException
     *             if state has previously been saved or an error occurs while trying to save it
     *             to the database
     */
    @Override
    public final IMADeviceState saveState(final IMADeviceState state)
            throws IMADeviceStateManagerException {
        try {
            return (IMADeviceState) super.saveState(state);
        } catch (DeviceStateManagerException e) {
            throw new IMADeviceStateManagerException(e);
        }
    }

    /**
     * Returns the <code>IMADeviceState</code> for a <code>Device</code>.
     *
     * @param device
     *            device
     * @return state
     * @throws IMADeviceStateManagerException
     *             if state has not previously been saved or an error occurs while trying to
     *             retrieve it from the database
     */
    @Override
    public final IMADeviceState getState(final Device device)
            throws IMADeviceStateManagerException {
        try {
            return (IMADeviceState) super.getState(device, IMADeviceState.class);
        } catch (DeviceStateManagerException e) {
            throw new IMADeviceStateManagerException(e);
        }
    }

    /**
     * Updates an <code>IMADeviceState</code>. This updates the database entries to reflect the
     * new values that should be set.
     *
     * @param state
     *            state
     * @throws IMADeviceStateManagerException
     *             if state has not previously been saved or an error occurs while trying to save
     *             it to the database
     */
    @Override
    public final void updateState(final IMADeviceState state)
            throws IMADeviceStateManagerException {
        try {
            super.updateState(state);
        } catch (DeviceStateManagerException e) {
            throw new IMADeviceStateManagerException(e);
        }
    }

    /**
     * Deletes the <code>IMADeviceState</code> from the database.
     *
     * @param device
     *            device whose state is to be remove
     * @return true if successfully found and deleted the
     *         <code>IMADeviceState</code>
     * @throws IMADeviceStateManagerException
     *             if any unexpected errors occur while trying to delete it from the database
     */
    @Override
    public final boolean deleteState(final Device device)
            throws IMADeviceStateManagerException {
        try {
            return super.deleteState(device, IMADeviceState.class);
        } catch (DeviceStateManagerException e) {
            throw new IMADeviceStateManagerException(e);
        }
    }
}
