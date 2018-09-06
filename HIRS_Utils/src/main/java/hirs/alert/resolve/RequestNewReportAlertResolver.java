package hirs.alert.resolve;

import hirs.data.persist.Alert;
import hirs.data.persist.Device;
import hirs.data.persist.IntegrityReport;
import hirs.persist.DeviceManager;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Aggregates and deduplicates the devices associated with alerts so that new reports can be
 * requested for them.
 */
@Component
public class RequestNewReportAlertResolver extends AlertResolver {

    private final Map<String, Device> devices = new HashMap<>();

    @Autowired
    private DeviceManager deviceManager;

    /**
     * Aggregates the devices associated with alerts so that new reports can be requested for them.
     *
     * @param alert the alert to be resolved
     * @return true if the device was successfully loaded
     */
    @Override
    public boolean resolve(final Alert alert) {

        String name = alert.getDeviceName();
        if (StringUtils.isBlank(name)) {
            name = ((IntegrityReport) alert.getReport()).getDeviceName();
        }

        if (StringUtils.isEmpty(name)) {
            addError("Could not get device name for alert [" + alert.getId() + "]");
            return false;
        } else if (!devices.containsKey(name)) {
            Device device = deviceManager.getDevice(name);
            if (device == null) {
                addError("Device '" + name + "' for alert id [" + alert.getId()
                        + "] was not found");
                return false;
            }
            devices.put(name, device);
        }

        return true;

    }

    /**
     * Returns a map of the loaded devices indexed by name.
     *
     * @return a map of the loaded devices indexed by name.
     */
    public Map<String, Device> getDevices() {
        return Collections.unmodifiableMap(devices);
    }

}
