package hirs.attestationca.persist.entity.manager;

import hirs.attestationca.persist.entity.userdefined.Device;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DeviceRepository extends JpaRepository<Device, UUID> {
    /**
     * Query that retrieves a device using the provided device name.
     *
     * @param deviceName device name
     * @return a device
     */
    Device findByName(String deviceName);

    /**
     * Query that retrieves all devices sorted by name in ascending order.
     *
     * @return a list of devices sorted by device name in ascending order
     */
    Page<Device> findAllByOrderByNameAsc(Pageable pageable);

    /**
     * Query that retrieves all devices sorted by name in descending order.
     *
     * @return a list of devices sorted by device name in descending order
     */
    Page<Device> findAllByOrderByNameDesc(Pageable pageable);
}
