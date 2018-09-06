package hirs.alert;

import hirs.data.persist.Alert;
import hirs.data.persist.ReportSummary;
import hirs.persist.AlertManager;
import hirs.persist.AlertServiceConfigManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.apache.logging.log4j.Logger;

import static org.apache.logging.log4j.LogManager.getLogger;

import java.util.List;

/**
 * The <code> CompositeAlertService</code> enables the forwarding of alerts. Supports alert
 * forwarding to multiple alert monitoring applications. The CompositeAlertService interacts with
 * the <code>AlertMonitorManager</code> to support Portal based management of the HIRS alert service
 * and subscribers (alert Monitors) of the service. Supports managed and 1 unmanaged alert services.
 * Currently the <code>HibernateAlertService</code> (the database) is the only unmanaged alert
 * service.
 */
@Service
public class CompositeAlertService implements AlertService {

    private static final Logger LOGGER = getLogger(CompositeAlertService.class);

    @Autowired
    private AlertServiceConfigManager alertServiceManager;

    @Autowired
    private List<ManagedAlertService> alertServices;

    @Autowired
    private AlertManager alertManager;

    /**
     * Forwarding service for alerts. Forwards the alert to all enabled Alert Service (Managed and
     * unmanaged).
     *
     * @param alert alert to forward
     */
    public final void alert(final Alert alert) {
        LOGGER.debug("Sending alerts to all Managed Alert Services");

        // persist the alert, must be done prior to using monitors.
        alertManager.saveAlert(alert);

        // iterate through the managed alert services and alert on each that is enabled
        for (ManagedAlertService currentService : alertServices) {
            if (currentService.isEnabled()) {
                currentService.alert(alert);
            }
        }
    }

    /**
     * Sends out an <code>Alert</code> when at least one alert is found after report processing is
     * completed. The alert service configuration will determine if the service will send
     * notification on every alert. This will notify all interested parties of a new alert. This is
     * intended to be called at the end of the Integrity Report Processing when at least one or more
     * alerts have been encountered
     *
     * @param summary the Summary created for the report
     */
    public final void alertSummary(final ReportSummary summary) {
        LOGGER.debug("Sending alert summaries to all Managed Alert Services");
        for (ManagedAlertService currentService : alertServices) {
            if (currentService.isEnabled()) {
                currentService.alertSummary(summary);
            }
        }
    }

}
