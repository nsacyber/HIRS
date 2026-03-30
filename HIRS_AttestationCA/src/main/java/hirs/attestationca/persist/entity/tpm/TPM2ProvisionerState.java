package hirs.attestationca.persist.entity.tpm;

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
 * This class is for saving the Identity Claim and the Nonce between the two passes of the TPM 2.0 Provisioner.
 */
@Entity
@NoArgsConstructor
@Log4j2
public class TPM2ProvisionerState {
    private static final int MAX_BLOB_SIZE = 16777215;

    @Column(nullable = false)
    private final Date timestamp = new Date();

    @Id
    private Long firstPartOfNonce;

    @Column(nullable = false)
    private byte[] nonce;

    @Lob
    @Column(nullable = false, length = MAX_BLOB_SIZE)
    private byte[] identityClaim;

    /**
     * Constructor.
     *
     * @param nonce         the nonce
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
}
