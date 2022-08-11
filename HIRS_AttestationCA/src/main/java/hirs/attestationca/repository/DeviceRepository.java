package hirs.attestationca.repository;

import hirs.data.persist.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;


/**
 * Setting up for new creation for CRUD operations.
 */
@Repository
public interface DeviceRepository extends JpaRepository<Device, UUID> {

    /**
     * Finds a <code>Device</code>.
     * If the <code>Device</code> is successfully retrieved then a reference to
     * it is returned.
     *
     * @param name the name to search by
     * @return reference to saved Device
     */
    Device findByName(String name);
}
