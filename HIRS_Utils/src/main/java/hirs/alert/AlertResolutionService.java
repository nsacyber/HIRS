package hirs.alert;

import hirs.alert.resolve.AlertResolver;
import hirs.appraiser.Appraiser;
import hirs.appraiser.IMAAppraiser;
import hirs.appraiser.TPMAppraiser;
import hirs.data.persist.Alert;
import hirs.alert.resolve.AlertResolverFactory;
import hirs.data.persist.Device;
import hirs.data.persist.DeviceGroup;
import hirs.data.persist.IMAPolicy;
import hirs.data.persist.baseline.ImaAcceptableRecordBaseline;
import hirs.data.persist.baseline.ImaBaseline;
import hirs.data.persist.baseline.ImaIgnoreSetBaseline;
import hirs.data.persist.baseline.TPMBaseline;
import hirs.data.persist.TPMPolicy;
import hirs.data.persist.baseline.TpmWhiteListBaseline;
import hirs.data.persist.enums.AlertSource;
import hirs.data.persist.enums.AlertType;
import hirs.persist.AppraiserManager;
import hirs.persist.DeviceManager;
import hirs.persist.PolicyManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import hirs.persist.AlertManager;
import hirs.persist.BaselineManager;
import hirs.persist.DeviceHealthManager;
import hirs.persist.ImaBaselineRecordManager;

/**
 * A service to resolve {@link Alert}s that are no longer problematic. Determines the possible
 * actions that can be taken given a specific set of alerts and takes those actions.
 */
@Service
public class AlertResolutionService {

    private static final Logger LOGGER = LogManager.getLogger(AlertResolutionService.class);

    @Autowired
    private DeviceManager deviceManager;

    @Autowired
    private AppraiserManager appraiserManager;

    @Autowired
    private PolicyManager policyManager;

    @Autowired
    private AlertManager alertManager;

    @Autowired
    private DeviceHealthManager deviceHealthManager;

    @Autowired
    private BaselineManager baselineManager;

    @Autowired
    private ImaBaselineRecordManager imaBaselineRecordManager;

    @Autowired
    private AlertResolverFactory alertResolverFactory;

    /**
     * This method will evaluate alerts and provide potentially useful
     * <code>AlertResolutionOption</code>s for resolution.
     *
     * @param alerts alerts to be evaluated for resolution
     * @return
     *      Returns a list of <code>AlertResolutionOption</code>s based on the source,
     *      type, and associated policy of the given alerts
     */
    public List<AlertResolutionOption> getResolutionOptions(final List<Alert> alerts) {

        // find cases where ignoring is the only option, such as nonexistent devices or groups,
        // multiple alert sources, or multiple device groups
        List<AlertResolutionOption> options = getIgnoreOnlyOptions(alerts);
        if (!options.isEmpty()) {
            return options;
        }

        // now the alert sources and the device groups of the referenced devices should all be
        // the same, so take them from the first alert
        DeviceGroup deviceGroup = deviceManager.getDevice(alerts.get(0).getDeviceName())
                .getDeviceGroup();
        AlertSource source = alerts.get(0).getSource();

        // build a list of resolution options specific to the alert source
        LOGGER.debug(String.format("source of alerts is %s", source.toString()));
        switch (source) {
            case IMA_APPRAISER:
                return getImaResolutionOptions(alerts, deviceGroup);
            case TPM_APPRAISER:
                return getTpmResolutionOptions(alerts, deviceGroup);
            // only the default options are supported for all other alerts
            default:
                return getDefaultOptions(alerts);
        }
    }

    /**
     * Determine if the given alerts support any actions besides ignoring. This will be the case if
     * one of the following is true:
     * <ul>
     * <li>Any of the alerts has a null device or nonexistent device name</li>
     * <li>Any of the alerts references a device with a null group</li>
     * <li>Any two alerts have different sources</li>
     * <li>Any two alerts have different device groups (and thus different policies)</li>
     * </ul>
     *
     * @param alerts the list of alerts to check
     * @return a list with an ignore option or an empty list if there might be more options
     */
    private List<AlertResolutionOption> getIgnoreOnlyOptions(final List<Alert> alerts) {

        List<AlertResolutionOption> options = new ArrayList<>();
        Device device = null;
        AlertSource sharedSource = null;
        AlertSource currentSource = null;
        DeviceGroup sharedDeviceGroup = null;
        DeviceGroup currentDeviceGroup = null;

        for (Alert alert : alerts) {

            // the device might be null if the report was malformed
            device = deviceManager.getDevice(alert.getDeviceName());
            if (device == null) {
                options.add(new AlertResolutionOption(
                        AlertResolutionAction.IGNORE,
                        "One or more alerts reference a nonexistent device."));
                return options;
            }

            // the device group shouldn't be null, but we should check it
            currentDeviceGroup = device.getDeviceGroup();
            if (currentDeviceGroup == null) {
                options.add(new AlertResolutionOption(
                        AlertResolutionAction.IGNORE,
                        "One or more alerts reference a device with a nonexistent group."));
                return options;
            }

            currentSource = alert.getSource();
            if (currentSource == null) {
                options.add(new AlertResolutionOption(
                        AlertResolutionAction.IGNORE,
                        "One or more alerts is missing an alert source."));
                return options;
            }

            // if this is the first alert in the list
            if (sharedSource == null && sharedDeviceGroup == null) {
                sharedSource = currentSource;
                sharedDeviceGroup = currentDeviceGroup;

            } else {
                if (!currentSource.equals(sharedSource)) {
                    options.add(new AlertResolutionOption(
                            AlertResolutionAction.IGNORE,
                            "Multiple alerts reference different alert sources."));
                    return options;
                }

                if (!currentDeviceGroup.equals(sharedDeviceGroup)) {
                    options.add(new AlertResolutionOption(
                            AlertResolutionAction.IGNORE,
                            "One or more alerts reference devices in different device groups."));
                    return options;
                }
            }
        }

        // an empty options list means the alert list may be actionable
        return options;
    }

    /**
     * Determine resolution options for alerts with IMA Appraiser as the alert source.
     * @param alertList - list of alerts that share a source and device group
     * @return a list of <code>AlertResolutionOption</code>s
     */
    private List<AlertResolutionOption> getImaResolutionOptions(
            final List<Alert> alertList, final DeviceGroup deviceGroup) {

        boolean canAddToBaseline = true;

        AlertType alertType;
        for (Alert alert : alertList) {
            alertType = alert.getType();

            // addToBaseline only helps if each alert would be fixed by adding a record
            if (!alertType.equals(AlertType.WHITELIST_MISMATCH)
                    && !alertType.equals(AlertType.REQUIRED_SET_MISMATCH)
                    && !alertType.equals(AlertType.UNKNOWN_FILE)) {
                LOGGER.debug("cannot add ima record to baseline to resolve alert because alert is"
                        + " type {}", alertType);
                canAddToBaseline = false;
                break;
            }
        }

        List<AlertResolutionOption> options = getDefaultOptions(alertList);

        if (canAddToBaseline) {
            options.add(getAddToImaBaselineOption(deviceGroup));
        }

        return options;
    }

    /**
     * Create an <code>AlertResolutionOption</code> to add to the IMA baselines associated with the
     * given device group.
     *
     * @param deviceGroup to get IMA baselines from
     * @return option including the possible baselines to add to
     */
    private AlertResolutionOption getAddToImaBaselineOption(final DeviceGroup deviceGroup) {

        AlertResolutionOption option = new AlertResolutionOption(
                AlertResolutionAction.ADD_TO_IMA_BASELINE,
                "One or more alerts could be resolved by adding a record to an IMA baseline.");
        Appraiser appraiser = appraiserManager.getAppraiser(IMAAppraiser.NAME);
        IMAPolicy imaPolicy = (IMAPolicy) policyManager.getPolicy(appraiser, deviceGroup);

        List<ImaAcceptableRecordBaseline> whitelists = new ArrayList<>(imaPolicy.getWhitelists());
        List<ImaAcceptableRecordBaseline> requiredSets =
                new ArrayList<>(imaPolicy.getRequiredSets());
        List<ImaIgnoreSetBaseline> ignoreSets = new ArrayList<>(imaPolicy.getIgnoreSets());

        List<String> whitelistNames = new ArrayList<>();
        for (ImaBaseline whitelist : whitelists) {
            whitelistNames.add(whitelist.getName());
        }
        option.setWhitelistNames(whitelistNames);

        List<String> requiredSetNames = new ArrayList<>();
        for (ImaBaseline requiredSet : requiredSets) {
            requiredSetNames.add(requiredSet.getName());
        }
        option.setRequiredSetNames(requiredSetNames);

        List<String> ignoreSetNames = new ArrayList<>();
        for (ImaIgnoreSetBaseline ignoreSet : ignoreSets) {
            ignoreSetNames.add(ignoreSet.getName());
        }
        option.setIgnoreSetNames(ignoreSetNames);

        return option;
    }

    /**
     * Determine resolution options for alerts with TPM Appraiser as the alert source.
     * @param alertList - list of alerts that share a source and device group
     * @return a list of <code>AlertResolutionOption</code>s
     */
    private List<AlertResolutionOption> getTpmResolutionOptions(
            final List<Alert> alertList, final DeviceGroup deviceGroup) {

        boolean canEditBaseline = true;

        // should only attempt to add to the baseline if all the alerts are of
        // the type WHITE_LIST_PCR_MISMATCH
        for (Alert alert : alertList) {
            if (!alert.getType().equals(AlertType.WHITE_LIST_PCR_MISMATCH)) {
                canEditBaseline = false;
                break;
            }
        }

        List<AlertResolutionOption> options = getDefaultOptions(alertList);

        if (canEditBaseline) {
            options.add(getAddToTpmBaselineOption(deviceGroup));
            options.add(new AlertResolutionOption(
                    AlertResolutionAction.REMOVE_FROM_TPM_BASELINE,
                    "One or more alerts could be resolved by removing a record from a TPM "
                    + "baseline."));
        }

        return options;
    }

    /**
     * Create an <code>AlertResolutionOption</code> to add to the TPM baselines associated with the
     * given device group.
     *
     * @param deviceGroup to get TPM baselines from
     * @return option including the possible baselines to add to
     */
    private AlertResolutionOption getAddToTpmBaselineOption(final DeviceGroup deviceGroup) {

        AlertResolutionOption option = new AlertResolutionOption(
                AlertResolutionAction.ADD_TO_TPM_BASELINE,
                "One or more alerts could be resolved by adding a record to a TPM baseline.");

        Appraiser appraiser = appraiserManager.getAppraiser(TPMAppraiser.NAME);
        TPMPolicy tpmPolicy = (TPMPolicy) policyManager.getPolicy(appraiser, deviceGroup);

        List<TpmWhiteListBaseline> tpmBaselines
                = new ArrayList<>(tpmPolicy.getTpmWhiteListBaselines());
        List<String> tpmBaselineNames = new ArrayList<>();
        for (TPMBaseline baseline : tpmBaselines) {
            tpmBaselineNames.add(baseline.getName());
        }
        option.setTpmBaselineNames(tpmBaselineNames);

        return option;
    }

    /**
     * Build the list of default options.
     * @return a list of the options available for all alerts
     */
    private List<AlertResolutionOption> getDefaultOptions(final List<Alert> alertList) {
        List<AlertResolutionOption> options = new ArrayList<>();

        LOGGER.debug("adding default alert resolution options for alert source {}",
                alertList.get(0).getSource());
        // ignoring the alert and requesting a new report are always options
        options.add(new AlertResolutionOption(
                AlertResolutionAction.IGNORE,
                "Default alert resolution option"));
        options.add(new AlertResolutionOption(
                AlertResolutionAction.REQUEST_NEW_REPORT,
                "Default alert resolution option"));
        return options;
    }

    /**
     * Checks AlertResolutionRequest parameters, then creates and invokes the appropriate
     * AlertResolver.
     *
     * @param request the AlertResolution request defining the action to be taken
     * @return AlertResolutionResults containing the resolved alerts and errors
     */
    @SuppressWarnings("checkstyle:avoidinlineconditionals")
    public AlertResolver resolve(final AlertResolutionRequest request) {

        // check alerts
        final List<Alert> alerts = request.getAlerts();
        if (alerts == null || alerts.isEmpty()) {
            return alertResolverFactory.getAnonymous("No alerts were provided.");
        }

        // check if valid resolution
        final AlertResolutionAction action = request.getAction();
        List<AlertResolutionOption> options = getResolutionOptions(request.getAlerts());
        for (AlertResolutionOption option : options) {
            if (option.hasAction(action)) {
                LOGGER.info("Resolving " + alerts.size() + " alert(s) by " + action + "...");
                return alertResolverFactory.get(request).resolve();
            }
        }

        // return error
        String msg = action + " is not a valid resolution for the specified alert";
        msg += request.getAlerts().size() == 1 ? "." : "s.";
        return alertResolverFactory.getAnonymous(msg);

    }

}
