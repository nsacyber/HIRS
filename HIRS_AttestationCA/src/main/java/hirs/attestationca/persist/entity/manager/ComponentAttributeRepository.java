package hirs.attestationca.persist.entity.manager;

import hirs.attestationca.persist.entity.userdefined.certificate.attributes.ComponentAttributeResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface for managing {@link ComponentAttributeResult} entities in the database.
 *
 * <p>
 * The {@link ComponentAttributeRepository} interface extends {@link JpaRepository} to provide basic CRUD operations,
 * including save, find, delete, and query methods. Custom query methods can be defined
 * using Spring Data JPA's query method naming conventions or with the Query annotation.
 * </p>
 */
public interface ComponentAttributeRepository extends JpaRepository<ComponentAttributeResult, UUID> {
    /**
     * Query to look up Attribute Results based on the PlatformCredential's
     * db component id.
     *
     * @param componentId the unique id for the component identifier
     * @return a list of attribute results
     */
    List<ComponentAttributeResult> findByComponentId(UUID componentId);

    /**
     * Query to look up Attribute Results based on the validation id.
     *
     * @param provisionSessionId unique id generated to link supply chain summary
     * @return a list of attribute results
     */
    List<ComponentAttributeResult> findByProvisionSessionId(UUID provisionSessionId);

    /**
     * Query to look up Attribute Results based on the component id and the session id.
     *
     * @param componentId        the unique id for the component identifier
     * @param provisionSessionId unique id generated to link supply chain summary
     * @return a list of attribute results
     */
    List<ComponentAttributeResult> findByComponentIdAndProvisionSessionId(UUID componentId,
                                                                          UUID provisionSessionId);
}
