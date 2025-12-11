package hirs.attestationca.persist.entity.manager;

import hirs.attestationca.persist.entity.userdefined.rim.ReferenceDigestValue;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReferenceDigestValueRepository extends JpaRepository<ReferenceDigestValue, UUID> {

    /**
     * Retrieves a page of reference digest values sorted by manufacturer in ascending order.
     *
     * @param pageable pageable
     * @return a page of ReferenceDigestValue sorted by manufacturer (ascending)
     */
    Page<ReferenceDigestValue> findAllByOrderByManufacturerAsc(Pageable pageable);

    /**
     * Query that retrieves a page of reference digest values sorted by manufacturer in descending order.
     *
     * @param pageable pageable
     * @return a page of ReferenceDigestValue sorted by manufacturer (descending)
     */
    Page<ReferenceDigestValue> findAllByOrderByManufacturerDesc(Pageable pageable);

    /**
     * Query that retrieves a page of reference digest values sorted by model in ascending order.
     *
     * @param pageable pageable
     * @return a page of reference digest values sorted by model (ascending)
     */
    Page<ReferenceDigestValue> findAllByOrderByModelAsc(Pageable pageable);

    /**
     * Query that retrieves a page of reference digest values sorted by model in descending order.
     *
     * @param pageable pageable
     * @return a page of reference digest values sorted by model (descending)
     */
    Page<ReferenceDigestValue> findAllByOrderByModelDesc(Pageable pageable);

    /**
     * Query that retrieves a page of reference digest values sorted by eventType in ascending order.
     *
     * @param pageable pageable
     * @return a page of reference digest values sorted by eventType (ascending)
     */
    Page<ReferenceDigestValue> findAllByOrderByEventTypeAsc(Pageable pageable);

    /**
     * Query that retrieves a page of reference digest values sorted by eventType in descending order.
     *
     * @param pageable pageable
     * @return a page of reference digest values sorted by eventType (descending)
     */
    Page<ReferenceDigestValue> findAllByOrderByEventTypeDesc(Pageable pageable);

    /**
     * Query that retrieves a page of reference digest values sorted by pcrIndex in ascending order.
     *
     * @param pageable pageable
     * @return a page of reference digest values sorted by pcrIndex (ascending)
     */
    Page<ReferenceDigestValue> findAllByOrderByPcrIndexAsc(Pageable pageable);

    /**
     * Query that retrieves a page of reference digest values sorted by pcrIndex in descending order.
     *
     * @param pageable pageable
     * @return a page of reference digest values sorted by pcrIndex (descending)
     */
    Page<ReferenceDigestValue> findAllByOrderByPcrIndexDesc(Pageable pageable);

    /**
     * Query that retrieves a page of reference digest values sorted by digestValue in ascending order.
     *
     * @param pageable pageable
     * @return a page of reference digest values sorted by digestValue (ascending)
     */
    Page<ReferenceDigestValue> findAllByOrderByDigestValueAsc(Pageable pageable);

    /**
     * Query that retrieves a page of reference digest values sorted by digestValue in descending order.
     *
     * @param pageable pageable
     * @return a page of reference digest values sorted by digestValue (descending)
     */
    Page<ReferenceDigestValue> findAllByOrderByDigestValueDesc(Pageable pageable);

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
