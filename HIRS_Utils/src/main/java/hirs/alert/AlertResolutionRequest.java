package hirs.alert;

import hirs.data.persist.Alert;

import java.util.Collections;
import java.util.List;

/**
 * Describes a resolution for an alert as specified by a user.
 */
public class AlertResolutionRequest {

    private final List<Alert> alerts;
    private final AlertResolutionAction action;
    private final String baselineName;
    private final String reason;

    /**
     * Constructor for AlertResolutionRequest.
     * @param alerts the alerts being resolved
     * @param action the action to take for resolving the alerts
     * @param baselineName the baseline
     * @param reason the reason for taking the action
     */
    public AlertResolutionRequest(final List<Alert> alerts, final AlertResolutionAction action,
            final String baselineName, final String reason) {
        this.alerts = alerts;
        this.action = action;
        this.baselineName = baselineName;
        this.reason = reason;
    }

    /**
     * Gets an unmodifiable List of the alerts.
     * @return an unmodifiable List of the alerts
     */
    public List<Alert> getAlerts() {
        return Collections.unmodifiableList(alerts);
    }

    /**
     * Gets the action being taken for the alerts.
     * @return the action being taken for the alerts
     */
    public AlertResolutionAction getAction() {
        return action;
    }

    /**
     * Gets the name of the baseline.
     * @return the name of the baseline
     */
    public String getBaselineName() {
        return baselineName;
    }

    /**
     * Gets the reason for taking the action.
     * @return the reason for taking the action
     */
    public String getReason() {
        return reason;
    }

}
