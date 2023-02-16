package hirs.attestationca.portal.entity.manager;

import hirs.attestationca.portal.entity.userdefined.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DeviceRepository extends JpaRepository<Device, UUID> {
    List<Device> findByName(String deviceName);
}
