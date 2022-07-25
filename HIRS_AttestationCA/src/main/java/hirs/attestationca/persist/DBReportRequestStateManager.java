package hirs.attestationca.persist;

import hirs.data.persist.Device;
import hirs.data.persist.ReportRequestState;
import hirs.persist.ReportRequestStateManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * This class defines a <code>ReportRequestStateManager</code> that stores ReportRequestStates in a
 * database.
 */
@Service
public class DBReportRequestStateManager extends DBManager<ReportRequestState>
        implements ReportRequestStateManager {
    private static final Logger LOGGER = LogManager.getLogger(DBReportRequestStateManager.class);

    /**
     * Creates a new <code>DBReportRequestStateManager</code> that uses the default database. The
     * default database is used to store all of the <code>ReportRequestState</code>s.
     *
     * @param em entity manager used to access database connections
     */
    public DBReportRequestStateManager(final EntityManager em) {
        super(ReportRequestState.class, em);
    }

    /**
     * Retrieves the state of a device, if the device and state exists.
     *
     * @param device the Device whose state should be retrieved
     * @return the associated ReportRequestState, or null if no associated state was found
     */
    @Override
    public final ReportRequestState getState(final Device device) {
        CriteriaBuilder builder = this.getSession().getCriteriaBuilder();
        Root<ReportRequestState> root = builder.createQuery(ReportRequestState.class)
                .from(ReportRequestState.class);

        Predicate predicate = builder.equal(root.get("device"), device);
        List<ReportRequestState> results = getWithCriteria(Collections.singletonList(predicate));
        if (results.isEmpty()) {
            return null;
        } else {
            LOGGER.debug("Retrieved ReportRequestState: {}", results.get(0));
            return results.get(0);
        }
    }

    /**
     * Return a Collection of all persisted ReportRequestStates in the database.
     *
     * @return the Collection of all persisted ReportRequestStates
     */
    @Override
    public final List<ReportRequestState> getLateDeviceStates() {
        CriteriaBuilder builder = this.getSession().getCriteriaBuilder();
        Root<ReportRequestState> root = builder.createQuery(ReportRequestState.class)
                .from(ReportRequestState.class);

        Predicate predicate = builder.lessThanOrEqualTo(root.get("dueDate"), new Date());
        return getWithCriteria(Collections.singletonList(predicate));
    }

    /**
     * Saves the given state to the database. The associated Device must be saved prior to saving
     * the state.
     *
     * @param state the state to save
     * @return the saved copy of the state
     */
    @Override
    public final ReportRequestState saveState(final ReportRequestState state) {
        if (state.getId() == null) {
            return save(state);
        } else {
            update(state);
            ReportRequestState updatedState = getState(state.getDevice());
            LOGGER.debug("Updated ReportRequestState: {}", updatedState);
            return updatedState;
        }
    }

    /**
     * Deletes the given {@link ReportRequestState} from the database.
     *
     * @param state the ReportRequestState instance to delete
     */
    @Override
    public final void deleteState(final ReportRequestState state) {
        delete(state.toString()); // cyrus-dev
    }
}
