package hirs.attestationca.portal.entity.manager;

import hirs.attestationca.portal.entity.userdefined.SupplyChainSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SettingsRepository extends JpaRepository<SupplyChainSettings, UUID> {
    SupplyChainSettings findByName(String name);
}
