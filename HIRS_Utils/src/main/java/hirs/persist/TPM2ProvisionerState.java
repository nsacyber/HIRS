package hirs.persist;


import hirs.data.persist.ArchivableEntity;
import org.bouncycastle.util.Arrays;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Date;

/**
 * This class is for saving the Identity Claim and the Nonce between the two passes of the
 * TPM 2.0 Provisioner.
 */
@Entity
public class TPM2ProvisionerState extends ArchivableEntity {
    private static final int MAX_BLOB_SIZE = 65535;

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
     * 0-argument constructor for Hibernate use.
     */
    protected TPM2ProvisionerState() {
    }

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
     * @param crudManager the {@link CrudManager} to use when looking for the
     * {@link TPM2ProvisionerState}
     * @param nonce the nonce to use as the key for the {@link TPM2ProvisionerState}
     * @return the {@link TPM2ProvisionerState} associated with the nonce;
     *         null if a match is not found
     */
    public static TPM2ProvisionerState getTPM2ProvisionerState(
            final CrudManager<TPM2ProvisionerState> crudManager,
            final byte[] nonce) {
        try (DataInputStream dis
                     = new DataInputStream(new ByteArrayInputStream(nonce))) {
            long firstPartOfNonce = dis.readLong();
            TPM2ProvisionerState stateFound = crudManager.get(firstPartOfNonce);
            if (Arrays.areEqual(stateFound.getNonce(), nonce)) {
                return stateFound;
            }
        } catch (IOException | NullPointerException e) {
            return null;
        }
        return null;
    }
}
