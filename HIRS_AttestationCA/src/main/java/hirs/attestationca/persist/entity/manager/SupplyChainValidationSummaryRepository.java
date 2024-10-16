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
     * @param device
     * @return
     */
    SupplyChainValidationSummary findByDevice(Device device);

    /**
     * @return
     */
    List<SupplyChainValidationSummary> findByArchiveFlagFalse();

    /**
     * @param pageable
     * @return
     */
    Page<SupplyChainValidationSummary> findByArchiveFlagFalse(Pageable pageable);
}
