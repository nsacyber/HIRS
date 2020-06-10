package hirs.persist;

import hirs.data.persist.Alert;
import hirs.data.persist.Device;
import hirs.data.persist.DeviceState;
import hirs.data.persist.enums.HealthStatus;
import hirs.data.persist.Report;
import hirs.data.persist.ReportSummary;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.apache.logging.log4j.LogManager.getLogger;

/**
 * Implementation class for DeviceHealthManager.
 */
public class DeviceHealthManagerImpl implements DeviceHealthManager {

    private static final Logger LOGGER = getLogger(DeviceHealthManagerImpl.class);

    @Autowired
    private DeviceManager deviceManager;

    @Autowired
    private AlertManager alertManager;

    @Autowired
    @Qualifier(PersistenceConfiguration.DEVICE_STATE_MANAGER_BEAN_NAME)
    private DeviceStateManager deviceStateManager;

    @Autowired
    private ReportSummaryManager reportSummaryManager;

    @Override
    public void updateHealth(final String deviceName) {

        Device device = deviceManager.getDevice(deviceName);

        if (null == device) {
            throw new IllegalArgumentException("No device with name: " + deviceName);
        }

        List<DeviceState> deviceStates = deviceStateManager.getStates(device);
        Report report = null;
        try {
            ReportSummary latestReportSummary =
                    reportSummaryManager.getNewestReport(device.getName());
            // if there's no (latest) report for this device, set the health to Unknown
            if (null == latestReportSummary) {
                LOGGER.warn("No latest report for {}. Setting health status to Unknown", device);
                device.setHealthStatus(HealthStatus.UNKNOWN);
            } else {
                report = latestReportSummary.getReport();
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to get latest report for {}. "
                    + "Setting health status to Unknown", device, e);
            device.setHealthStatus(HealthStatus.UNKNOWN);
        }

        if (null != report) {
            Disjunction disjunction = Restrictions.disjunction();
            boolean hasValidCriterions = false;
            for (DeviceState state : deviceStates) {
                Criterion criterion = state.getDeviceTrustAlertCriterion();
                if (null != criterion) {
                    disjunction.add(criterion);
                    hasValidCriterions = true;
                }
            }
            if (!hasValidCriterions) {
                disjunction = null;
            }

            int alertCount = alertManager.getTrustAlertCount(device, report, disjunction);
            LOGGER.info("Found {} trust alerts for {}", alertCount, device);
            if (alertCount > 0) {
                device.setHealthStatus(HealthStatus.UNTRUSTED);
            } else {
                device.setHealthStatus(HealthStatus.TRUSTED);
            }
        }
        deviceManager.updateDevice(device);
    }

    @Override
    public void updateHealth(final List<Alert> alerts) {
        if (alerts == null) {
            throw new NullPointerException("alert list");
        }
        Set<String> deviceNames = new HashSet<>();
        // get the set of unique device names for these alerts
        for (Alert alert : alerts) {
            String deviceName = alert.getDeviceName();
            if (StringUtils.isNotEmpty(deviceName)) {
                deviceNames.add(deviceName);
            }
        }

        for (String deviceName : deviceNames) {
            updateHealth(deviceName);
        }
    }
}
