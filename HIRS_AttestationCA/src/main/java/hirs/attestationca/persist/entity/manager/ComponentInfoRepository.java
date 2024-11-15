package hirs.attestationca.persist.entity.manager;

import hirs.attestationca.persist.entity.userdefined.info.ComponentInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ComponentInfoRepository extends JpaRepository<ComponentInfo, UUID> {
    /**
     * Query that retrieves device components by device name.
     *
     * @param deviceName string for the host name
     * @return a list of device components
     */
    List<ComponentInfo> findByDeviceName(String deviceName);

    /**
     * Query that retrieves device components by device name and
     * the component serial number.
     *
     * @param deviceName      string for the host name
     * @param componentSerial string for the component serial
     * @return a list of device components
     */
    List<ComponentInfo> findByDeviceNameAndComponentSerial(String deviceName, String componentSerial);
}
