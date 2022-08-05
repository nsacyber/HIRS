package hirs.attestationca.repository;

import hirs.data.persist.policy.Policy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Setting up for new creation for CRUD operations.
 */
@Repository
public interface PolicyRepository<T extends Policy> extends JpaRepository<T, UUID> {
    T save(T policy);

    T updatePolicy(T policy, UUID uuid);
}
