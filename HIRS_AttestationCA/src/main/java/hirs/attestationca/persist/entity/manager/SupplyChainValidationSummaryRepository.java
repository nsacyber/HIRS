package hirs.attestationca.persist.entity.manager;

import hirs.attestationca.persist.entity.userdefined.Device;
import hirs.attestationca.persist.entity.userdefined.SupplyChainValidationSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SupplyChainValidationSummaryRepository
        extends JpaRepository<SupplyChainValidationSummary, UUID> {

    /**
     * Query that retrieves a supply chain validation summary using the provided device.
     *
     * @param device device
     * @return a supply chain validation summary
     */
    SupplyChainValidationSummary findByDevice(Device device);

    /**
     * Query that retrieves a list of supply chain validation summaries where the archive flag is false.
     *
     * @return a list of supply chain validation summary
     */
    List<SupplyChainValidationSummary> findByArchiveFlagFalse();

    /**
     * Query that retrieves a page of supply chain validation summaries using the provided pageable value
     * and where the archive flag is false.
     *
     * @param pageable pageable
     * @return a page of supply chain validation summary
     */
    Page<SupplyChainValidationSummary> findByArchiveFlagFalse(Pageable pageable);
}
