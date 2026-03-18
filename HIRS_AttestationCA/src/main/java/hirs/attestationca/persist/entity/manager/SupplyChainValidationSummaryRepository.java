package hirs.attestationca.persist.entity.manager;

import hirs.attestationca.persist.entity.userdefined.SupplyChainValidationSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SupplyChainValidationSummaryRepository extends JpaRepository<SupplyChainValidationSummary, UUID> {

    /**
     * Query that retrieves a page of supply chain validation summaries using the provided pageable value
     * and where the archive flag is false, sorted by create time in descending order.
     *
     * @param pageable pageable
     * @return a page of supply chain validation summary
     */
    Page<SupplyChainValidationSummary> findByArchiveFlagFalseOrderByCreateTimeDesc(Pageable pageable);
}
