package hirs.attestationca.persist.service;

import hirs.attestationca.persist.entity.manager.SupplyChainValidationSummaryRepository;
import hirs.attestationca.persist.entity.userdefined.SupplyChainValidation;
import hirs.attestationca.persist.entity.userdefined.SupplyChainValidationSummary;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Service class that encapsulates the business logic for displaying Supply Chain Validation Summary data.
 */
@Log4j2
@Service
public class ValidationDetailsPageService {
    private final SupplyChainValidationSummaryRepository supplyChainValidationSummaryRepository;

    public ValidationDetailsPageService(SupplyChainValidationSummaryRepository supplyChainValidationSummaryRepository) {
        this.supplyChainValidationSummaryRepository = supplyChainValidationSummaryRepository;
    }

    public Map<String, UUID> getAssociatedIds(final UUID uuid) {
        HashMap<String, UUID> associatedIds = new HashMap<>();
        SupplyChainValidationSummary supplyChainValidationSummary =
                supplyChainValidationSummaryRepository.findSupplyChainValidationSummaryById(uuid);
        Set<SupplyChainValidation> associatedValidations = supplyChainValidationSummary.getValidations();
        for(SupplyChainValidation validation : associatedValidations) {
           associatedIds.put(validation.getValidationType().toString(), validation.getId());
        }

        return associatedIds;
    }
    //method to query policy settings associated with summary id
}
