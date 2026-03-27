package hirs.attestationca.persist.entity.manager;

import hirs.attestationca.persist.entity.userdefined.SupplyChainValidationSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface for managing {@link SupplyChainValidationSummary} entities in the database.
 *
 * <p>
 * The {@link SupplyChainValidationSummaryRepository} interface extends {@link JpaRepository} to provide
 * basic CRUD operations, including save, find, delete, and query methods. Custom query methods can be defined
 * using Spring Data JPA's query method naming conventions or with the Query annotation.
 * </p>
 */
@Repository
public interface SupplyChainValidationSummaryRepository extends JpaRepository<SupplyChainValidationSummary, UUID> {

    /**
     * Query that retrieves a page of {@link SupplyChainValidationSummary} objects using the provided pageable value
     * and where the archive flag is false.
     *
     * @param pageable pageable
     * @return a page of {@link SupplyChainValidationSummary} objects
     */
    Page<SupplyChainValidationSummary> findByArchiveFlagFalse(Pageable pageable);


    /**
     * Query that retrieves a list of {@link SupplyChainValidationSummary} objects where the archive flag is false
     * and in order of creation time in descending order.
     *
     * @return a list of {@link SupplyChainValidationSummary} objects
     */
    List<SupplyChainValidationSummary> findByArchiveFlagFalseOrderByCreateTimeDesc();
}
