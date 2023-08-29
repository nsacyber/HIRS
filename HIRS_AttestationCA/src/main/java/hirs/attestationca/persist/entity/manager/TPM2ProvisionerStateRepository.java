package hirs.attestationca.persist.entity.manager;

import hirs.attestationca.persist.entity.tpm.TPM2ProvisionerState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TPM2ProvisionerStateRepository extends JpaRepository<TPM2ProvisionerState, Long> {

    TPM2ProvisionerState findByFirstPartOfNonce(Long findByFirstPartOfNonce);
}
