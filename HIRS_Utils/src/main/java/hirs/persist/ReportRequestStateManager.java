package hirs.persist;

import hirs.data.persist.Device;
import hirs.data.persist.ReportRequestState;

import java.util.Collection;

/**
 * A <code>ReportRequestStateManager</code> manages the persistence of {@link ReportRequestState}
 * instances.
 */
public interface ReportRequestStateManager {

    /**
     * Retrieves the associated {@link ReportRequestState} for the given {@link Device}, if one
     * exists.  Creates and returns a new state for the device if none exists.
     *
     * @param device the Device whose state should be retrieved
     * @return the associated ReportRequestState
     */
    ReportRequestState getState(Device device);

    /**
     * Persists the given state.
     *
     * @param state the state to persist
     * @return the persisted copy of the state
     */
    ReportRequestState saveState(ReportRequestState state);

    /**
     * Removes the given {@link ReportRequestState} instance.
     *
     * @param state the ReportRequestState instance to remove
     */
    void deleteState(ReportRequestState state);

    /**
     * Retrieves all {@link ReportRequestState} where the {@link ReportRequestState#getDueDate()}
     * is &le; <code>now</code>.
     *
     * @return non-null listing of late {@link ReportRequestState}.
     */
    Collection<ReportRequestState> getLateDeviceStates();
}
