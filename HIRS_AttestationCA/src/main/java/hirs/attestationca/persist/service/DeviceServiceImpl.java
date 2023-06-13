package hirs.attestationca.persist.service;

import hirs.attestationca.persist.entity.manager.DeviceRepository;
import hirs.attestationca.persist.entity.userdefined.Device;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * https://github.com/darrachequesne/spring-data-jpa-datatables
 */
@Service
public class DeviceServiceImpl extends DefaultDbService<Device> {

    @Autowired
    private EntityManager entityManager;
    @Autowired
    private DeviceRepository deviceRepository;


    public void saveDevice(Device device) {
        this.deviceRepository.save(device);
    }

    public void saveDevices(List<Device> devices) {
        for (Device device : devices) {
            this.deviceRepository.save(device);
        }
    }
}
