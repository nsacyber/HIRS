package hirs.alert;

import hirs.alert.resolve.AddToIMABaselineAlertResolver;
import hirs.alert.resolve.AddToTPMBaselineAlertResolver;
import hirs.alert.resolve.AlertResolver;
import hirs.alert.resolve.IgnoreAlertResolver;
import hirs.alert.resolve.RemoveFromIMABaselineAlertResolver;
import hirs.alert.resolve.RemoveFromTPMBaselineAlertResolver;
import hirs.alert.resolve.RequestNewReportAlertResolver;
import hirs.data.persist.baseline.Baseline;
import hirs.data.persist.baseline.SimpleImaBaseline;
import hirs.data.persist.baseline.TpmWhiteListBaseline;

/**
 * Specifies actions that can be taken to resolve an Alert.
 */
public enum AlertResolutionAction {

    /**
     * Take no action.
     */
    NONE("No action taken"),
    /**
     * Ignore the given alert or alerts.
     */
    IGNORE("Ignored Once", IgnoreAlertResolver.class),
    /**
     * Add the referenced IMA record to a baseline.
     */
    ADD_TO_IMA_BASELINE("Added to IMA Baseline", AddToIMABaselineAlertResolver.class,
            SimpleImaBaseline.class),
    /**
     * Remove the referenced IMA record from any baselines it can be found in.
     */
    REMOVE_FROM_IMA_BASELINE("Removed from IMA Baseline", RemoveFromIMABaselineAlertResolver.class,
            SimpleImaBaseline.class),
    /**
     * Add the referenced TPM record to a baseline.
     */
    ADD_TO_TPM_BASELINE("Added to TPM baseline", AddToTPMBaselineAlertResolver.class,
            TpmWhiteListBaseline.class),
    /**
     * Remove the referenced TPM record from any baselines it can be found in.
     */
    REMOVE_FROM_TPM_BASELINE("Removed from TPM Baseline", RemoveFromTPMBaselineAlertResolver.class,
            TpmWhiteListBaseline.class),
    /**
     * Request a new report.
     */
    REQUEST_NEW_REPORT("Requested new report.", RequestNewReportAlertResolver.class);

    private final String defaultReason;
    private final Class<? extends AlertResolver> alertResolver;
    private final Class<? extends Baseline> baselineType;

    /**
     * Construct a new AlertResolutionAction with a reason the alert has been resolved, the class
     * that can perform the resolution, and the class of baseline operated on by this resolution.
     *
     * @param defaultReason the reason an alert can be considered resolved as a result of this
     *                      action
     * @param alertResolver the class that will perform the resolution action
     * @param baselineType the type of baseline operated on by this resolution action
     */
    AlertResolutionAction(final String defaultReason,
            final Class<? extends AlertResolver> alertResolver,
            final Class<? extends Baseline> baselineType) {
        this.defaultReason = defaultReason;
        this.alertResolver = alertResolver;
        this.baselineType = baselineType;
    }

    /**
     * Construct a new AlertResolutionAction with a reason the alert has been resolved and the class
     * that can perform the resolution.
     *
     * @param defaultReason the reason an alert can be considered resolved as a result of this
     *                      action
     * @param alertResolver the class that will perform the resolution action
     */
    AlertResolutionAction(final String defaultReason,
            final Class<? extends AlertResolver> alertResolver) {
        this.defaultReason = defaultReason;
        this.baselineType = null;
        this.alertResolver = alertResolver;
    }

    /**
     * Construct a new AlertResolutionAction with a reason the alert has been resolved.
     *
     * @param defaultReason the reason an alert can be considered resolved as a result of this
     *                      action
     */
    AlertResolutionAction(final String defaultReason) {
        this.defaultReason = defaultReason;
        this.baselineType = null;
        this.alertResolver = null;
    }

    /**
     * Returns a string containing a generic reason for the resolution if not provided by the user.
     *
     * @return string containing a generic reason for the resolution
     */
    public String getDefaultReason() {
        return defaultReason;
    }

    /**
     * Returns the AlertResolver class for the action.
     *
     * @return the AlertResolver class for the action
     */
    public Class<? extends AlertResolver> getAlertResolver() {
        return alertResolver;
    }

    /**
     * Returns the appropriate {@link Baseline} class for the action or null if the action does not
     * involve a baseline.
     *
     * @return the appropriate {@link Baseline} class for the action
     */
    public Class<? extends Baseline> getBaselineType() {
        return baselineType;
    }

    /**
     * Returns true if the resolution modifies a Baseline.
     *
     * @return true if the resolution modifies a Baseline
     */
    public boolean isBaselineResolution() {
        return baselineType != null;
    }

    /**
     * Returns true if the resolution can resolve the specified Baseline.
     *
     * @param baseline the Baseline to test
     * @return true if the resolution can resolve the specified Baseline.
     */
    public boolean canResolve(final Baseline baseline) {
        if (baseline == null || baselineType == null) {
            return false;
        } else {
            return baselineType.isAssignableFrom(baseline.getClass());
        }
    }

}
