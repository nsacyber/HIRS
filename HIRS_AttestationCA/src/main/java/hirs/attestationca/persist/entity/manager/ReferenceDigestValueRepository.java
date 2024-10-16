package hirs.attestationca.persist.entity.manager;

import hirs.attestationca.persist.entity.userdefined.rim.ReferenceDigestValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReferenceDigestValueRepository extends JpaRepository<ReferenceDigestValue, UUID> {

    /**
     * Query that retrieves a
     *
     * @param model
     * @return
     */
    List<ReferenceDigestValue> findByModel(String model);

    /**
     * Query that retrieves a
     *
     * @param manufacturer
     * @return
     */
    List<ReferenceDigestValue> findByManufacturer(String manufacturer);

    /**
     * Query that retrieves a
     *
     * @param associatedRimId
     * @return
     */
    List<ReferenceDigestValue> findValuesByBaseRimId(UUID associatedRimId);

    /**
     * Query that retrieves a
     *
     * @param supportRimId
     * @return
     */
    List<ReferenceDigestValue> findBySupportRimId(UUID supportRimId);

    /**
     * Query that retrieves a
     *
     * @param supportRimHash
     * @return
     */
    List<ReferenceDigestValue> findBySupportRimHash(String supportRimHash);

    /**
     * Query that retrieves a
     *
     * @param manufacturer
     * @param model
     * @return
     */
    List<ReferenceDigestValue> findByManufacturerAndModel(String manufacturer, String model);
}
