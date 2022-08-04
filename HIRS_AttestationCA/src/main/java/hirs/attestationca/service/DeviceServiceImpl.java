package hirs.attestationca.service;

import hirs.FilteredRecordsList;
import hirs.attestationca.repository.DeviceRepository;
import hirs.data.persist.Device;
import hirs.persist.CriteriaModifier;
import hirs.persist.DBManagerException;
import hirs.persist.DeviceManagerException;
import hirs.persist.OrderedQuery;
import hirs.persist.service.DefaultService;
import hirs.persist.service.DeviceService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A <code>DeviceServiceImpl</code> manages <code>Device</code>s. A
 * <code>DeviceServiceImpl</code> is used to store and manage devices. It has
 * support for the basic create, read, update, and delete methods.
 */
@Service
public class DeviceServiceImpl extends DbServiceImpl<Device> implements DefaultService<Device>,
        DeviceService, OrderedQuery<Device> {

    private static final Logger LOGGER = LogManager.getLogger(DeviceServiceImpl.class);
    @Autowired
    private DeviceRepository deviceRepository;

    /**
     * Default constructor.
     * @param em entity manager for jpa hibernate events
     */
    public DeviceServiceImpl(final EntityManager em) {
    }

    @Override
    public final Device getByName(final String name) {
        LOGGER.debug("Find device by name: {}", name);

        return getRetryTemplate().execute(new RetryCallback<Device,
                DBManagerException>() {
            @Override
            public Device doWithRetry(final RetryContext context)
                    throws DBManagerException {
                return deviceRepository.findByName(name);
            }
        });
    }

    @Override
    public final Device saveDevice(final Device device) throws DeviceManagerException {
        LOGGER.debug("Saving device: {}", device);

        return getRetryTemplate().execute(new RetryCallback<Device,
                DBManagerException>() {
            @Override
            public Device doWithRetry(final RetryContext context)
                    throws DBManagerException {
                return deviceRepository.save(device);
            }
        });
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

        return saveDevice(dbDevice);
    }

    @Override
    public final List<Device> getList() {
        LOGGER.debug("Getting all devices...");

        return getRetryTemplate().execute(new RetryCallback<List<Device>, DBManagerException>() {
            @Override
            public List<Device> doWithRetry(final RetryContext context)
                    throws DBManagerException {
                return deviceRepository.findAll();
            }
        });
    }

    @Override
    public void updateElements(final List<Device> devices) {
        LOGGER.debug("Updating {} devices...", devices.size());

        devices.stream().forEach((device) -> {
            if (device != null) {
                this.updateDevice(device, device.getId());
            }
        });
        deviceRepository.flush();
    }

    @Override
    public final void deleteObjectById(final UUID uuid)
            throws DeviceManagerException {
        LOGGER.debug("Deleting deviceById: {}", uuid);

        getRetryTemplate().execute(new RetryCallback<Void, DBManagerException>() {
            @Override
            public Void doWithRetry(final RetryContext context)
                    throws DBManagerException {
                deviceRepository.deleteById(uuid);
                deviceRepository.flush();
                return null;
            }
        });
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
