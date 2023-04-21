package hirs.attestationca.persist.service;

import hirs.attestationca.persist.entity.userdefined.SupplyChainValidation;

import java.util.List;
import java.util.UUID;

public interface SupplyChainValidationService {
    SupplyChainValidation saveSupplyChainValidation(SupplyChainValidation supplyChainValidation);

    List<SupplyChainValidation> fetchSupplyChainValidations();

    SupplyChainValidation updateSupplyChainValidation(SupplyChainValidation supplyChainValidation, UUID scvId);

    void deleteSupplyChainValidation(UUID scvId);
}
