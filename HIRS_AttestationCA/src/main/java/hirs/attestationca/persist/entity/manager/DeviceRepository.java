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
     * Query that retrieves a page of all devices using the provided pageable value sorted in descending order by their
     * creation time.
     *
     * @param pageable pageable
     * @return a page of all devices sorted in descending order by their creation time
     */
    Page<Device> findAllByOrderByCreateTimeDesc(Pageable pageable);
}
