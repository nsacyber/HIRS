package hirs.attestationca.service;


import hirs.attestationca.entity.Device;
import hirs.data.persist.DeviceInfoReport;

/**
 * Interface defining methods for registering a device with the system.
 */
public interface DeviceRegister {

    /**
     * Registers a device in to the system using a device info report.
     * If the device doesn't exist, the device is added to the default group.
     * The device info report is stored, and associated with the device
     * @param report the device info report
     * @return the created or updated device
     */
    Device saveOrUpdateDevice(DeviceInfoReport report);

    /**
     * Registers a device with the specified name.
     * @param deviceName the device name
     * @return the created or updated device
     */
    Device saveOrUpdateDevice(String deviceName);
}
