package hirs.attestationca.persist.entity.manager;

import hirs.attestationca.persist.entity.userdefined.rim.ReferenceDigestValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface for managing {@link ReferenceDigestValue} entities in the database.
 *
 * <p>
 * The {@link ReferenceDigestValueRepository} interface extends {@link JpaRepository} to provide basic CRUD operations,
 * including save, find, delete, and query methods. Custom query methods can be defined
 * using Spring Data JPA's query method naming conventions or with the Query annotation.
 * </p>
 */
@Repository
public interface ReferenceDigestValueRepository extends JpaRepository<ReferenceDigestValue, UUID> {

    /**
     * Query that retrieves a list of reference digest values using the provided model.
     *
     * @param model string representation of the model
     * @return a list of reference digest values
     */
    List<ReferenceDigestValue> findByModel(String model);

    /**
     * Query that retrieves a list of reference digest values using the provided manufacturer.
     *
     * @param manufacturer string representation of the manufacturer
     * @return a list of reference digest values
     */
    List<ReferenceDigestValue> findByManufacturer(String manufacturer);

    /**
     * Query that retrieves a list of reference digest values using the provided associated rim id.
     *
     * @param associatedRimId uuid representation of the associated rim ID
     * @return a list of reference digest values
     */
    List<ReferenceDigestValue> findValuesByBaseRimId(UUID associatedRimId);

    /**
     * Query that retrieves a list of reference digest values using the provided support rim id.
     *
     * @param supportRimId uuid representation of the support rim ID
     * @return a list of reference digest values
     */
    List<ReferenceDigestValue> findBySupportRimId(UUID supportRimId);

    /**
     * Query that retrieves a list of reference digest values using the provided support rim hash.
     *
     * @param supportRimHash a string representation of the support rim hash
     * @return a list of reference digest values
     */
    List<ReferenceDigestValue> findBySupportRimHash(String supportRimHash);

    /**
     * Query that retrieves a list of reference digest values using the provided manufacturer and model.
     *
     * @param manufacturer string representation of the manufacturer
     * @param model        string representation of the model
     * @return a list of reference digest values
     */
    List<ReferenceDigestValue> findByManufacturerAndModel(String manufacturer, String model);
}
