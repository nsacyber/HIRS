package hirs.attestationca.persist.entity.manager;

import hirs.attestationca.persist.entity.userdefined.Device;
import hirs.attestationca.persist.entity.userdefined.SupplyChainValidationSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.List;

@Repository
public interface SupplyChainValidationSummaryRepository extends JpaRepository<SupplyChainValidationSummary, UUID> {
    SupplyChainValidationSummary findByDevice(Device device);
    List<SupplyChainValidationSummary> findByArchiveFlagFalse();
    Page<SupplyChainValidationSummary> findByArchiveFlagFalse(Pageable pageable);
}
