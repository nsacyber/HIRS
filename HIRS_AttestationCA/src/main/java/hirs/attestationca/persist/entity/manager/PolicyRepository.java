package hirs.attestationca.persist.entity.manager;

import hirs.attestationca.persist.entity.userdefined.PolicySettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository interface for managing the ACA's {@link PolicySettings} in the database.
 *
 * <p>
 * The {@link PolicyRepository} interface extends {@link JpaRepository} to provide basic CRUD operations,
 * including save, find, delete, and query methods. Custom query methods can be defined
 * using Spring Data JPA's query method naming conventions or with the Query annotation.
 * </p>
 */
@Repository
public interface PolicyRepository extends JpaRepository<PolicySettings, UUID> {

    /**
     * Query that retrieves policy settings using the provided name.
     *
     * @param name name
     * @return policy settings
     */
    PolicySettings findByName(String name);
}
