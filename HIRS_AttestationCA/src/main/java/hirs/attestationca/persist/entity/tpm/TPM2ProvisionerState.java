package hirs.attestationca.persist.entity.tpm;

import hirs.attestationca.persist.entity.manager.TPM2ProvisionerStateRepository;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.bouncycastle.util.Arrays;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Date;

/**
 * This class is for saving the Identity Claim and the Nonce between the two passes of the
 * TPM 2.0 Provisioner.
 */
@Log4j2
@NoArgsConstructor
@Entity
public class TPM2ProvisionerState {
    private static final int MAX_BLOB_SIZE = 16777215;

    @Id
    private Long firstPartOfNonce;

    @Column(nullable = false)
    private byte[] nonce;

    @Lob
    @Column(nullable = false, length = MAX_BLOB_SIZE)
    private byte[] identityClaim;

    @Column(nullable = false)
    private Date timestamp = new Date();

    /**
     * Constructor.
     *
     * @param nonce the nonce
     * @param identityClaim the identity claim
     */
    public TPM2ProvisionerState(final byte[] nonce, final byte[] identityClaim) {
        if (nonce == null) {
            throw new IllegalArgumentException("Nonce should not be null");
        }

        if (identityClaim == null) {
            throw new IllegalArgumentException("Identity Claim should not be null");
        }

        if (nonce.length < Long.BYTES) {
            throw new IllegalArgumentException(
                    String.format("Nonce must be larger than 8 bytes. (Received %d.)",
                            nonce.length));
        }

        this.nonce = Arrays.clone(nonce);
        this.identityClaim = Arrays.clone(identityClaim);

        try (DataInputStream dis = new DataInputStream(new ByteArrayInputStream(nonce))) {
            firstPartOfNonce = dis.readLong();
        } catch (IOException e) {
            // This would only happen if there were not enough bytes; that is handled above.
            throw new RuntimeException(e);
        }
    }

    /**
     * Get the nonce.
     *
     * @return the nonce
     */
    public byte[] getNonce() {
        return Arrays.clone(nonce);
    }

    /**
     * Get the identity claim.
     *
     * @return the identity claim
     */
    public byte[] getIdentityClaim() {
        return Arrays.clone(identityClaim);
    }

    /**
     * Convenience method for finding the {@link TPM2ProvisionerState} associated with the nonce.
     *
     * @param tpm2ProvisionerStateRepository the {@link TPM2ProvisionerStateRepository} to use when looking for the
     * {@link TPM2ProvisionerState}
     * @param nonce the nonce to use as the key for the {@link TPM2ProvisionerState}
     * @return the {@link TPM2ProvisionerState} associated with the nonce;
     *         null if a match is not found
     */
    public static TPM2ProvisionerState getTPM2ProvisionerState(
            final TPM2ProvisionerStateRepository tpm2ProvisionerStateRepository,
            final byte[] nonce) {
        try (DataInputStream dis
                     = new DataInputStream(new ByteArrayInputStream(nonce))) {
            long firstPartOfNonce = dis.readLong();
            TPM2ProvisionerState stateFound = tpm2ProvisionerStateRepository
                    .findByFirstPartOfNonce(firstPartOfNonce);
            if (stateFound != null && Arrays.areEqual(stateFound.getNonce(), nonce)) {
                return stateFound;
            }
        } catch (IOException ioEx) {
            log.error(ioEx.getMessage());
            return null;
        }
        return null;
    }
}
