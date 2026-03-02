package hirs.attestationca.persist.entity.manager;

import hirs.attestationca.persist.entity.userdefined.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository interface for managing {@link Device} entities in the database.
 *
 * <p>
 * The {@link DeviceRepository} interface extends {@link JpaRepository} to provide basic CRUD operations,
 * including save, find, delete, and query methods. Custom query methods can be defined
 * using Spring Data JPA's query method naming conventions or with the Query annotation.
 * </p>
 */
@Repository
public interface DeviceRepository extends JpaRepository<Device, UUID> {
    /**
     * Query that retrieves a device using the provided device name.
     *
     * @param deviceName device name
     * @return a device
     */
    Device findByName(String deviceName);
}
