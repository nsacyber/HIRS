package hirs.persist;

import hirs.data.persist.Alert;

import java.util.List;

/**
 * Class for managing the {@link hirs.data.persist.HealthStatus}
 * of a {@link hirs.data.persist.Device}.
 */
public interface DeviceHealthManager {

    /**
     * Updates the health of a device given the name of the device.
     *
     * Finds the latest report for the device. Finds all device state objects for the device to get
     * additional criterion to use to search for alerts. Device health is updated to UNKNOWN if
     * there are no previous reports, UNTRUSTED if there are still relevant alerts given the search
     * criteria, and TRUSTED if there are no relevant alerts.
     *
     * @param deviceName name of device
     */
    void updateHealth(String deviceName);

    /**
     * Updates the health of a device or devices given a list of alerts.
     *
     * Useful for updating health after resolving a list of alerts, this method pulls all the
     * unique device names from the alerts list and calls {@link #updateHealth(String)} for each
     * device once.
     *
     * @param alerts list of alerts
     */
    void updateHealth(List<Alert> alerts);
}
