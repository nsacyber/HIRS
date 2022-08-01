package hirs.attestationca.service;

import hirs.FilteredRecordsList;
import hirs.attestationca.repository.DeviceRepository;
import hirs.data.persist.Device;
import hirs.persist.CriteriaModifier;
import hirs.persist.DBManagerException;
import hirs.persist.DeviceManagerException;
import hirs.persist.OrderedQuery;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A <code>DeviceServiceImpl</code> manages <code>Device</code>s. A
 * <code>DeviceServiceImpl</code> is used to store and manage devices. It has
 * support for the basic create, read, update, and delete methods.
 */
@Service
public class DeviceServiceImpl implements DeviceService, OrderedQuery<Device> {

    private static final Logger LOGGER = LogManager.getLogger();
    @Autowired
    private DeviceRepository deviceRepository;

    @Override
    public final Device saveDevice(final Device device) throws DeviceManagerException {
        LOGGER.debug("Saving device: {}", device);
        return deviceRepository.save(device);
    }

    @Override
    public final List<Device> getDeviceList() {
        LOGGER.debug("Getting all devices...");
        return deviceRepository.findAll();
    }

    @Override
    public final Device updateDevice(final Device device, final UUID deviceId)
            throws DeviceManagerException {
        LOGGER.debug("Updating device: {}", device);
        Device dbDevice;

        if (deviceId == null) {
            LOGGER.debug("Device not found: {}", device);
            dbDevice = device;
        } else {
            // will not return null, throws and exception
            dbDevice = deviceRepository.getReferenceById(deviceId);

            // run through things that aren't equal and update

        }

        deviceRepository.save(dbDevice);

        return dbDevice;
    }

    @Override
    public final void updateDeviceList(final List<Device> deviceList)
            throws DeviceManagerException {
        LOGGER.debug("Updating {} devices...", deviceList.size());

        deviceList.stream().forEach((device) -> {
            if (device != null) {
                this.updateDevice(device, device.getId());
            }
        });

    }

    @Override
    public final void deleteDeviceById(final UUID deviceId)
            throws DeviceManagerException {
        LOGGER.debug("Deleting deviceById: {}", deviceId);
        deviceRepository.deleteById(deviceId);
    }

    @Override
    public FilteredRecordsList getOrderedList(
            final Class<Device> clazz, final String columnToOrder,
            final boolean ascending, final int firstResult, final int maxResults,
            final String search, final Map<String, Boolean> searchableColumns)
            throws DBManagerException {
        return null;
    }

    @Override
    public FilteredRecordsList<Device> getOrderedList(
            final Class<Device> clazz, final String columnToOrder,
            final boolean ascending, final int firstResult, final int maxResults,
            final String search, final Map<String, Boolean> searchableColumns,
            final CriteriaModifier criteriaModifier)
            throws DBManagerException {
        return null;
    }
}
