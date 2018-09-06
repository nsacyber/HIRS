package hirs.data.service;

import hirs.data.persist.Device;
import hirs.data.persist.DeviceGroup;
import hirs.data.persist.DeviceInfoReport;
import hirs.persist.DeviceGroupManager;
import hirs.persist.DeviceManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service handling registering of a device to the default group.
 * Saves or updates a Device in the system, and its DeviceInfoReport, if
 * provided. Newly created devices are assigned to the default group.
 *
 * @see Device
 * @see DeviceInfoReport
 */
@Service
public class DeviceRegisterImpl implements DeviceRegister {

    private DeviceManager deviceManager;
    private DeviceGroupManager deviceGroupManager;

    private static final Logger LOGGER = LogManager.getLogger(DeviceRegisterImpl.class);

    /**
     * Constructor.
     * @param deviceManager the device manager
     * @param deviceGroupManager the device group manager
     */
    @Autowired
    public DeviceRegisterImpl(final DeviceManager deviceManager,
                              final DeviceGroupManager deviceGroupManager) {
        this.deviceManager = deviceManager;
        this.deviceGroupManager = deviceGroupManager;
    }

    @Override
    public Device saveOrUpdateDevice(final DeviceInfoReport report) {
        String deviceName = report.getNetworkInfo().getHostname();
        return registerDeviceToManager(deviceName, report);
    }

    @Override
    public Device saveOrUpdateDevice(final String deviceName) {
        return registerDeviceToManager(deviceName, null);
    }

    private Device registerDeviceToManager(final String deviceName, final DeviceInfoReport report) {
        Device savedDevice = deviceManager.getDevice(deviceName);
        if (savedDevice == null) {
            LOGGER.debug("device not found, saving new device");
            Device newDevice = new Device(deviceName, report);
            DeviceGroup group = deviceGroupManager.getDeviceGroup(DeviceGroup.DEFAULT_GROUP);
            newDevice.setDeviceGroup(group);
            deviceManager.saveDevice(newDevice);
        } else {
            LOGGER.debug("device found, updating device");
            savedDevice.setDeviceInfo(report);
            deviceManager.updateDevice(savedDevice);
        }
        return deviceManager.getDevice(deviceName);
    }
}
