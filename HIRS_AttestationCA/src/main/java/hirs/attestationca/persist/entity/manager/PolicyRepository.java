package hirs.attestationca.persist.entity.manager;

import hirs.attestationca.persist.entity.userdefined.PolicySettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PolicyRepository extends JpaRepository<PolicySettings, UUID> {

    /**
     * Query that retrieves policy settings using the provided name.
     *
     * @param name name
     * @return policy settings
     */
    PolicySettings findByName(String name);
}
