package hirs.attestationca.persist.entity.manager;

import hirs.attestationca.persist.entity.userdefined.info.ComponentInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface for managing {@link ComponentInfo} entities in the database.
 *
 * <p>
 * The {@link ComponentInfoRepository} interface extends {@link JpaRepository} to provide basic CRUD operations,
 * including save, find, delete, and query methods. Custom query methods can be defined
 * using Spring Data JPA's query method naming conventions or with the Query annotation.
 * </p>
 */
public interface ComponentInfoRepository extends JpaRepository<ComponentInfo, UUID> {
    /**
     * Query that retrieves a list of {@link ComponentInfo} objects (device components) by device name.
     *
     * @param deviceName string for the host name
     * @return a list of {@link ComponentInfo} objects (device components)
     */
    List<ComponentInfo> findByDeviceName(String deviceName);
}
