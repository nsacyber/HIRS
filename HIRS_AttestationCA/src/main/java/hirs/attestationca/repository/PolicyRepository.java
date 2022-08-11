package hirs.attestationca.repository;

import hirs.data.persist.policy.Policy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Setting up for new creation for CRUD operations.
 * @param <T> super type for Policy child type
 */
@Repository
public interface PolicyRepository<T extends Policy> extends JpaRepository<T, UUID> {

    /**
     * Saves the <code>Policy</code> in the database. This creates a new
     * database session and saves the policy.
     *
     * @param policy Policy to save
     * @return reference to saved policy
     */
    T save(T policy);

    /**
     * Updates a <code>Policy</code>. This updates the database entries to
     * reflect the new values that should be set.
     *
     * @param policy Policy object to save
     * @param uuid UUID for the database object
     * @return a Policy object
     */
    T updatePolicy(T policy, UUID uuid);
}
