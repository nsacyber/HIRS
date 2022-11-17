package hirs.attestationca.service;

import hirs.attestationca.entity.Device;
import hirs.attestationca.entity.DeviceInfoReport;
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

    private DeviceService deviceService;

    private static final Logger LOGGER = LogManager.getLogger(DeviceRegisterImpl.class);

    /**
     * Constructor.
     * @param deviceService the device service
     */
    @Autowired
    public DeviceRegisterImpl(final DeviceService deviceService) {
        this.deviceService = deviceService;
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
        Device savedDevice = deviceService.getByName(deviceName);

        if (savedDevice != null) {
            LOGGER.debug("device found, updating device");
            savedDevice.setDeviceInfo(report);
            deviceService.updateDevice(savedDevice, savedDevice.getId());
            return savedDevice;
        }

        LOGGER.debug("device not found, saving new device");
        Device newDevice = new Device(deviceName, report);
        deviceService.saveDevice(newDevice);
        return newDevice;
    }
}
