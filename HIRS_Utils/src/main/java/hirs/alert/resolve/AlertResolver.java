package hirs.alert.resolve;

import hirs.alert.AlertResolutionAction;
import hirs.alert.AlertResolutionRequest;
import hirs.data.persist.Alert;
import hirs.persist.AlertManager;
import hirs.persist.DeviceHealthManager;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Abstract class providing common functionality for resolving alerts including parameter checking,
 * retrieving Alerts, Alert iteration, recording lists of resolved Alerts and error messages
 * encountered during the Alert resolution process, and ultimately marking Alerts as being
 * successfully resolved.
 */
public abstract class AlertResolver {

    private static final Logger LOGGER = LogManager.getLogger(BaselineAlertResolver.class);

    /**
     * Returns an exception's message or its class name if message is null.
     *
     * @param ex the exception from which to get the message
     * @return the exception's message or its class name if message is null
     */
    protected static String getMessage(final Exception ex) {
        if (StringUtils.isBlank(ex.getMessage())) {
            return ex.getClass().getSimpleName();
        } else {
            return ex.getMessage();
        }
    }

    @Autowired
    private AlertManager alertManager;

    @Autowired
    private DeviceHealthManager deviceHealthManager;

    private AlertResolutionRequest request;
    private boolean hasExecuted = false;
    private final List<Alert> resolved = new ArrayList<>();
    private final List<String> errors = new ArrayList<>();

    /**
     * The default or user-specified explanation for the action taken.
     */
    @SuppressWarnings("checkstyle:visibilitymodifier")
    protected String reason;

    /**
     * Initializes the resolver by setting the resolution request and resetting results, errors, and
     * state.
     *
     * @param request the request specifying the alerts to be resolved and action to be taken.
     * @return this resolver
     */
    final AlertResolver init(final AlertResolutionRequest request) {
        this.request = request;
        hasExecuted = false;
        resolved.clear();
        errors.clear();
        reason = null;
        return this;
    }

    /**
     * Returns this resolver's alert resolution request.
     *
     * @return this resolver's alert resolution request.
     */
    protected AlertResolutionRequest getRequest() {
        return request;
    }

    /**
     * Adds the specified alert to the list of resolved alerts.
     *
     * @param alert the alert that has been resolved successfully
     * @return this resolver
     */
    protected final AlertResolver addResolved(final Alert alert) {
        resolved.add(alert);
        return this;
    }

    /**
     * Returns the list of resolved alerts.
     *
     * @return the list of resolved alerts.
     */
    public final List<Alert> getResolved() {
        return Collections.unmodifiableList(resolved);
    }

    /**
     * Adds the message to the list of error messages.
     *
     * @param error the message to add
     * @return this resolver
     */
    public final AlertResolver addError(final String error) {
        errors.add(error);
        return this;
    }

    /**
     * Adds the exception's message to the list of error messages.
     *
     * @param ex the exception to add
     * @return this resolver
     */
    public final AlertResolver addError(final Exception ex) {
        errors.add(getMessage(ex));
        return this;
    }

    /**
     * Adds the error messages in the collection to the list of error messages.
     *
     * @param errors the error messages to add
     * @return this resolver
     */
    public final AlertResolver addErrors(final Collection<String> errors) {
        this.errors.addAll(errors);
        return this;
    }

    /**
     * Returns true if error messages have been recorded.
     *
     * @return true if error messages have been recorded.
     */
    public final boolean hasErrors() {
        return !errors.isEmpty();
    }

    /**
     * Returns the list of error messages.
     *
     * @return the list of error messages.
     */
    public final List<String> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    /**
     * Returns true if this resolver can perform the specified action.
     *
     * @param action the action to check
     * @return true if this resolver can perform the specified action.
     */
    public final boolean canResolve(final AlertResolutionAction action) {
        if (action == null || action.getAlertResolver() == null) {
            return false;
        } else {
            return action.getAlertResolver().isAssignableFrom(getClass());
        }
    }

    /**
     * Performs the resolution for the specified alerts. Checks if the resolver's current state is
     * valid, does nothing if errors have already been encountered, sets the default reason if not
     * specified by the user, iterates over the specified alerts calling the subclass's
     * resolve(Alert) method, records the successfully resolved alerts, and updates the health
     * status for associated devices
     *
     * @return this resolver
     */
    public final AlertResolver resolve() {

        // don't execute if errors have already been recorded.
        if (!hasErrors()) {

            // only run if initialized
            if (request == null) {
                throw new IllegalStateException("Resolver has not been initialized.");
            }

            // only allow to run once
            if (hasExecuted) {
                throw new IllegalStateException("Resolver has already been executed.");
            }
            hasExecuted = true;

            final AlertResolutionAction action = request.getAction();

            // get reason
            reason = StringUtils.defaultIfBlank(request.getReason(), action.getDefaultReason());

            // catch unexpected errors
            try {

                // allow subclasses to execute code before loop
                if (beforeLoop()) {

                    // process each alert
                    if (action != AlertResolutionAction.NONE) {
                        for (final Alert alert : request.getAlerts()) {
                            if (resolve(alert)) {
                                addResolved(alert);
                            }
                        }
                    }

                    // allow subclasses to execute code after loop
                    if (afterLoop()) {

                        // mark the alerts as resolved
                        alertManager.resolveAlerts(getResolved(), reason);
                        deviceHealthManager.updateHealth(getResolved());

                    }

                }

            } catch (Exception ex) {
                LOGGER.error("Error resolving alerts:", ex);
                addError(ex);
            }

        }

        return this;

    }

    /**
     * Called for each alert by resolve. Must be implemented by subclasses.
     *
     * @param alert the alert to resolve
     * @return true if the resolution was successful
     */
    public abstract boolean resolve(Alert alert);

    /**
     * Optional hook to allow subclasses to execute code before the alerts are iterated.
     *
     * @return true if successful
     */
    protected boolean beforeLoop() {
        return true;
    }

    /**
     * Optional hook to allow subclasses to execute code after the alerts are iterated.
     *
     * @return true if successful
     */
    protected boolean afterLoop() {
        return true;
    }

}
