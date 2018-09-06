package hirs.alert;

import com.google.common.base.Preconditions;
import hirs.data.persist.Alert;
import hirs.data.persist.ReportSummary;
import hirs.data.persist.alert.AlertMonitor;
import hirs.data.persist.alert.AlertServiceConfig;
import hirs.persist.AlertMonitorManager;
import hirs.persist.AlertServiceConfigManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.apache.logging.log4j.LogManager.getLogger;

/**
 * The ManagedAlertService is an abstract class that enables optional HIRS alert notifications. The
 * <code>CompositeAlertService</code> will receive the alert notification from a HIRS component
 * (e.g. IMAAppraiser)and forward the alert to all enabled alert services. Managed Alert Services
 * enable portal configuration settings to create,delete, and configure settings for remote
 * consumers of HIRS alerts.
 */
public abstract class ManagedAlertService implements AlertService {

    private static final Logger LOGGER = getLogger(ManagedAlertService.class);

    /**
     * DB Manager for Alert Monitors.
     */
    @Autowired
    private AlertMonitorManager alertMonitorManager;

    /**
     * DB Manager for Alert Service Configs.
     */
    @Autowired
    private AlertServiceConfigManager alertServiceManager;

    /**
     * Alert Service Configuration for this Alert Service.
     */
    private AlertServiceConfig config;

    /**
     * Alert Monitor List for this Alert Service.
     */
    private List<AlertMonitor> monitors = new ArrayList<>();

    private String name;

    /**
     * Construct a new ManagedAlertService.
     *
     * @param name the name of the alert service
     */
    public ManagedAlertService(final String name) {
        Preconditions.checkNotNull(name, "Name argument cannot be null.");
        this.name = name;
    }

    /**
     * Saves a new alert service manager.
     *
     * @param alertServiceConfigManager the manager to use to persist objects
     */
    public abstract void persist(AlertServiceConfigManager alertServiceConfigManager);

    /**
     * Returns the type of the Managed Alert Service. type is used by DBManager to retrieve an
     * object. name should be set by the class that extends ManagedAlertService within its
     * constructor.
     *
     * @return Type of the Managed Alert Service
     */
    public String getName() {
        return name;
    }

    /**
     * Returns a single configuration for the alert service.
     *
     * @return AlertServiceConfig object
     */
    public AlertServiceConfig getConfig() {
        return config;
    }

    /**
     * Sets the configuration of a Managed Alert Service.
     *
     * @param newConfig Alert Service Configuration
     */
    public final void updateConfig(final AlertServiceConfig newConfig) {
        LOGGER.info("Updating Configuration for Alert Service of type {}", newConfig.getType());
        alertServiceManager.updateAlertServiceConfig(newConfig);
    }

    /**
     * Returns a list of alert monitors for this AlertService. Only monitors with the
     * AlertServiceName matching this service will be returned.
     *
     * @return A list of ManagedAlertMonitors
     */
    public final List<AlertMonitor> getMonitors() {
        return Collections.unmodifiableList(monitors);
    }

    /**
     * Finds and returns an Alert monitor.
     *
     * @param monitorName name of the Alert Monitor
     * @return a AlertMonitor or null if not found
     */
    public final AlertMonitor getMonitor(final String monitorName) {
        LOGGER.debug("Getting Alert Monitor named {}", monitorName);
        return alertMonitorManager.getAlertMonitor(monitorName);
    }

    /**
     * Adds a monitor to the service monitor list. Inserts the name of this alert service to the
     * monitor object
     *
     * @param newMonitor add a monitor to the Alert Services monitor list
     */
    public final void addMonitor(final AlertMonitor newMonitor) {
        LOGGER.info("Adding Alert Monitor named {}", newMonitor.getName());
        newMonitor.setAlertServiceType(name);
        alertMonitorManager.saveAlertMonitor(newMonitor);
    }

    /**
     * Deletes the specified monitor from the alert services monitor list.
     *
     * @param monitorToDelete Alert Monitor to remove
     */
    public final void deleteMonitor(final AlertMonitor monitorToDelete) {
        LOGGER.info("Deleting Alert Monitor named {}", monitorToDelete.getName());
        alertMonitorManager.deleteAlertMonitor(monitorToDelete.getName());
    }

    /**
     * Forwards an alert to a managed alert service.
     *
     * @param alert alert to forward
     */
    @Override
    public void alert(final Alert alert) {
        //check if portal updated properties since last time
        reloadProperties();
        // Only process alert if this alert service is enabled.
        if (isEnabled()) {
            for (AlertMonitor currentMonitor : monitors) {
                if (currentMonitor.isMonitorEnabled()
                        && currentMonitor.isIndividualAlertEnabled()) {
                    sendAlert(currentMonitor, alert);
                }
            }
        }
    }

    /**
     * Returns true if the configuration is not null and is enabled; false otherwise.
     *
     * @return true if the configuration is not null and is enabled; false otherwise
     */
    public boolean isEnabled() {
        return config != null && config.isEnabled();
    }

    /**
     * Sends out an <code>Alert</code> when at least one alert is found after report processing is
     * completed. The alert service configuration will determine if the service will send
     * notification on every alert. This will notify all interested parties of a new alert.
     *
     * @param summary summary ReportSummary
     */
    public void alertSummary(final ReportSummary summary) {
        List<AlertMonitor> monitorList = alertMonitorManager
                .getAlertMonitorList(AlertMonitor.class);
        for (AlertMonitor currentMonitor : monitorList) {
            if (currentMonitor.isMonitorEnabled()
                    && currentMonitor.isAlertOnSummaryEnabled()) {
                sendAlertSummary(currentMonitor, summary);
            }
        }
    }

    /**
     * Sends an alert to a single alert monitor.
     *
     * @param monitor an individual alert monitor to send the alert to
     * @param alert   the individual alert to send to the remote monitor
     */
    protected abstract void sendAlert(AlertMonitor monitor, Alert alert);

    /**
     * Sends an alert summary to a single alert monitor.
     *
     * @param monitor an individual alert monitor to send the alert to
     * @param summary a summary for for the report
     */
    protected abstract void sendAlertSummary(AlertMonitor monitor, ReportSummary summary);

    /**
     * Reloads <code>AlertService</code> configuration properties. This is intended to be called
     * after the HIRS Portal updates a configuration file. This will force the service to update its
     * properties.
     */
    @PostConstruct
    public final void reloadProperties() {
        config = loadConfig();
        monitors = loadMonitors();
    }

    /**
     * Returns a list of alert monitors for this AlertService. Only monitors with the
     * AlertServiceName matching this service will be returned.
     *
     * @return A list of ManagedAlertMonitors
     */
    private List<AlertMonitor> loadMonitors() {
        LOGGER.debug("Getting Alert Monitor list");
        List<AlertMonitor> alertMonitors =
                alertMonitorManager.getAlertMonitorList(AlertMonitor.class);
        List<AlertMonitor> filteredMonitors = new ArrayList<>();

        // screen all monitors and make sure they were intended for this alert service
        for (AlertMonitor monitor : alertMonitors) {
            if (monitor.getAlertServiceType().equals(getName())) {
                filteredMonitors.add(monitor);
            }
        }
        return filteredMonitors;
    }

    /**
     * Returns a single configuration for the alert service.
     *
     * @return AlertServiceConfig object
     */
    private AlertServiceConfig loadConfig() {
        List<AlertServiceConfig> configurations =
                alertServiceManager.getAlertServiceConfigList(AlertServiceConfig.class);

        AlertServiceConfig configuration = null;
        for (AlertServiceConfig conf : configurations) {
            if (conf.getType().equals(getName())) {
                configuration = conf;
            }
        }
        return configuration;
    }
}
