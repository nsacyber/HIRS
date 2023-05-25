package hirs.attestationca.persist.entity.manager;

import hirs.attestationca.persist.entity.userdefined.PolicySettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PolicyRepository extends JpaRepository<PolicySettings, UUID> {
//    PolicySettings findByName(String name);
}
