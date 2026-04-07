package hirs.attestationca.persist.entity.manager;

import hirs.attestationca.persist.entity.tpm.TPM2ProvisionerState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for managing {@link TPM2ProvisionerState} entities in the database.
 *
 * <p>
 * The {@link TPM2ProvisionerStateRepository} interface extends {@link JpaRepository} to provide basic CRUD operations,
 * including save, find, delete, and query methods. Custom query methods can be defined
 * using Spring Data JPA's query method naming conventions or with the Query annotation.
 * </p>
 */
@Repository
public interface TPM2ProvisionerStateRepository extends JpaRepository<TPM2ProvisionerState, Long> {

    /**
     * Query that retrieves the {@link TPM2ProvisionerState} object using the provided first part of nonce.
     *
     * @param findByFirstPartOfNonce long representation of the first part of nonce
     * @return a {@link TPM2ProvisionerState} object
     */
    TPM2ProvisionerState findByFirstPartOfNonce(Long findByFirstPartOfNonce);
}
