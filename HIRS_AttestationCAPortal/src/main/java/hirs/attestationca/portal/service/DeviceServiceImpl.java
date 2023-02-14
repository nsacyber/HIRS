package hirs.attestationca.portal.service;

import hirs.attestationca.portal.entity.manager.DeviceRepository;
import hirs.attestationca.portal.entity.userdefined.Device;
import hirs.attestationca.portal.enums.AppraisalStatus;
import hirs.attestationca.portal.enums.HealthStatus;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * https://github.com/darrachequesne/spring-data-jpa-datatables
 */
@Service
public class DeviceServiceImpl {

    @Autowired(required = false)
    private EntityManager entityManager;
    @Autowired
    private DeviceRepository deviceRepository;

    private static List<Device> devices = new ArrayList<>(Arrays.asList(
            new Device("Dell", HealthStatus.TRUSTED,
                    AppraisalStatus.Status.UNKNOWN,
                    Timestamp.valueOf(LocalDateTime.MAX), false, "testing", "resting"),
            new Device("Intel", HealthStatus.UNTRUSTED,
                    AppraisalStatus.Status.FAIL,
                    Timestamp.valueOf(LocalDateTime.MIN), false, "testing", "resting"),
            new Device("Cybex", HealthStatus.UNKNOWN,
                    AppraisalStatus.Status.PASS,
                    Timestamp.valueOf(LocalDateTime.now()), false, "testing", "resting")));

    public List<Device> retrieveDevices() {
        List<Device> devices = new ArrayList<Device>();

        for (Device device : this.devices) {
            devices.add(device);
        }

        return devices;
    }

}
