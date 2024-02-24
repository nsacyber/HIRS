package hirs.attestationca.persist.entity.manager;

import hirs.attestationca.persist.entity.userdefined.info.ComponentInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ComponentInfoRepository extends JpaRepository<ComponentInfo, UUID> {
    List<ComponentInfo> findByDeviceName(String deviceName);
    List<ComponentInfo> findByDeviceNameAndComponentSerial(String deviceName, String componentSerial);
}
