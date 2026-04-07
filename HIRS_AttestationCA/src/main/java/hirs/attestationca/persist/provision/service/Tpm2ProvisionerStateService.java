package hirs.attestationca.persist.provision.service;

import hirs.attestationca.persist.entity.manager.TPM2ProvisionerStateRepository;
import hirs.attestationca.persist.entity.tpm.TPM2ProvisionerState;
import lombok.extern.log4j.Log4j2;
import org.bouncycastle.util.Arrays;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

/**
 * Service class responsible for handling and processing the TPM2 Provisioner State.
 */
@Service
@Log4j2
public class Tpm2ProvisionerStateService {
    private final TPM2ProvisionerStateRepository tpm2ProvisionerStateRepository;

    /**
     * Constructor.
     *
     * @param tpm2ProvisionerStateRepository TPM2 Provisioner State Repository
     */
    @Autowired
    public Tpm2ProvisionerStateService(final TPM2ProvisionerStateRepository tpm2ProvisionerStateRepository) {
        this.tpm2ProvisionerStateRepository = tpm2ProvisionerStateRepository;
    }

    /**
     * Retrieves the {@link TPM2ProvisionerState} object associated with the nonce.
     *
     * @param nonce the nonce to use as the key for the {@link TPM2ProvisionerState}
     * @return the {@link TPM2ProvisionerState} associated with the nonce; null if a match is not found
     */
    public TPM2ProvisionerState getTPM2ProvisionerState(final byte[] nonce) {
        try (DataInputStream dis = new DataInputStream(new ByteArrayInputStream(nonce))) {
            long firstPartOfNonce = dis.readLong();

            final TPM2ProvisionerState stateFound =
                    tpm2ProvisionerStateRepository.findByFirstPartOfNonce(firstPartOfNonce);

            if (stateFound != null && Arrays.areEqual(stateFound.getNonce(), nonce)) {
                return stateFound;
            }
        } catch (IOException ioEx) {
            log.error(ioEx.getMessage());
        }

        return null;
    }

    /**
     * Deletes the provided {@link TPM2ProvisionerState} object from the database.
     *
     * @param tpm2ProvisionerStateToBeDeleted TPM2 Provisioner State that will be deleted
     */
    public void deleteTPM2ProvisionerState(final TPM2ProvisionerState tpm2ProvisionerStateToBeDeleted) {
        tpm2ProvisionerStateRepository.delete(tpm2ProvisionerStateToBeDeleted);
    }

    /**
     * Save the provided {@link TPM2ProvisionerState} object to the database.
     *
     * @param tpm2ProvisionerStateToBeSaved TPM2 Provisioner State that will be saved
     */
    public void saveTPM2ProvisionerState(final TPM2ProvisionerState tpm2ProvisionerStateToBeSaved) {
        tpm2ProvisionerStateRepository.save(tpm2ProvisionerStateToBeSaved);
    }
}
