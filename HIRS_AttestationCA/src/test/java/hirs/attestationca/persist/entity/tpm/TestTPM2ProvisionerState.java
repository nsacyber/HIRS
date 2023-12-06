package hirs.attestationca.persist.entity.tpm;

import hirs.attestationca.persist.entity.manager.TPM2ProvisionerStateRepository;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Random;

/**
 * Contains unit tests for {@link TPM2ProvisionerState}.
 */
public class TestTPM2ProvisionerState {

    /**
     * Tests that the values passed to the constructor are equal to the values
     * returned by the getters.
     *
     * @throws IOException this will never happen
     */
    @Test
    public final void testTPM2ProvisionerState() throws IOException {
        Random rand = new Random();
        byte[] nonce = new byte[32];
        byte[] identityClaim = new byte[360];
        rand.nextBytes(nonce);
        rand.nextBytes(identityClaim);

        TPM2ProvisionerState state = new TPM2ProvisionerState(nonce, identityClaim);

        assertArrayEquals(state.getNonce(), nonce);
        assertArrayEquals(state.getIdentityClaim(), identityClaim);
    }

    /**
     * Test that the constructor throws an {@link IllegalArgumentException} when a null is
     * passed in for the nonce.
     *
     * @throws IOException this will never happen
     */
    @Test
    public final void testNullNonce() throws IOException {
        Random rand = new Random();
        byte[] nonce = null;
        byte[] identityClaim = new byte[360];
        rand.nextBytes(identityClaim);
        assertThrows(IllegalArgumentException.class, () ->
                new TPM2ProvisionerState(nonce, identityClaim));
    }

    /**
     * Test that the constructor throws an {@link IllegalArgumentException} when a null is
     * passed in for the identity claim.
     *
     * @throws IOException this will never happen
     */
    @Test
    public final void testNullIdentityClaim() throws IOException {
        Random rand = new Random();
        byte[] nonce = new byte[32];
        byte[] identityClaim = null;
        rand.nextBytes(nonce);
        assertThrows(IllegalArgumentException.class, () ->
                new TPM2ProvisionerState(nonce, identityClaim));
    }

    /**
     * Test that the constructor throws an {@link IllegalArgumentException} when a nonce is
     * passed in that is less than 8 bytes.
     *
     * @throws IOException this will never happen
     */
    @Test
    public final void testNonceToSmall() throws IOException {
        Random rand = new Random();
        byte[] nonce = new byte[7];
        byte[] identityClaim = new byte[360];
        rand.nextBytes(nonce);
        rand.nextBytes(identityClaim);
        assertThrows(IllegalArgumentException.class, () ->
                new TPM2ProvisionerState(nonce, identityClaim));
    }


    /**
     * Test that {@link TPM2ProvisionerState#getTPM2ProvisionerState(TPM2ProvisionerStateRepository, byte[])} works.
     * {@link TPM2ProvisionerState#getTPM2ProvisionerState(TPM2ProvisionerStateRepository, byte[])}, null is returned.
     * @throws IOException this will never happen
     */
    @Test
    public final void testGetTPM2ProvisionerStateNominal() throws IOException {
        TPM2ProvisionerStateRepository tpm2ProvisionerStateRepository = mock(TPM2ProvisionerStateRepository.class);
        Random rand = new Random();
        byte[] nonce = new byte[32];
        byte[] identityClaim = new byte[360];
        rand.nextBytes(nonce);
        rand.nextBytes(identityClaim);

        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(nonce));
        Long index = dis.readLong();
        dis.close();
        TPM2ProvisionerState value = new TPM2ProvisionerState(nonce, identityClaim);
        when(tpm2ProvisionerStateRepository.findByFirstPartOfNonce(index)).thenReturn(value);
        TPM2ProvisionerState tpm2ProvisionerState
                = TPM2ProvisionerState.getTPM2ProvisionerState(tpm2ProvisionerStateRepository, nonce);
        assertNotNull(tpm2ProvisionerState);
        assertArrayEquals(tpm2ProvisionerState.getIdentityClaim(), value.getIdentityClaim());
    }

    /**
     * Test that if a null is passed as a nonce to
     * {@link TPM2ProvisionerState#getTPM2ProvisionerState(TPM2ProvisionerStateRepository, byte[])}, null is returned.
     * @throws IOException this will never happen
     */
    @Test
    public final void testGetTPM2ProvisionerStateNullNonce() throws IOException {
        TPM2ProvisionerStateRepository tpm2ProvisionerStateRepository = mock(TPM2ProvisionerStateRepository.class);
        Random rand = new Random();
        byte[] nonce = new byte[32];
        byte[] identityClaim = new byte[360];
        rand.nextBytes(nonce);
        rand.nextBytes(identityClaim);
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(nonce));
        Long index = dis.readLong();
        dis.close();
        TPM2ProvisionerState value = new TPM2ProvisionerState(nonce, identityClaim);
        when(tpm2ProvisionerStateRepository.findByFirstPartOfNonce(index)).thenReturn(value);
        TPM2ProvisionerState tpm2ProvisionerState
                = TPM2ProvisionerState.getTPM2ProvisionerState(tpm2ProvisionerStateRepository, null);
        assertNull(tpm2ProvisionerState);

    }

    /**
     * Test that if a nonce that is less than 8 bytes is passed to
     * {@link TPM2ProvisionerState#getTPM2ProvisionerState(TPM2ProvisionerStateRepository, byte[])}, null is returned.
     * @throws IOException this will never happen
     */
    @Test
    public final void testGetTPM2ProvisionerStateNonceTooSmall() throws IOException {
        TPM2ProvisionerStateRepository tpm2ProvisionerStateRepository = mock(TPM2ProvisionerStateRepository.class);
        Random rand = new Random();
        byte[] nonce = new byte[32];
        byte[] identityClaim = new byte[360];
        rand.nextBytes(nonce);
        rand.nextBytes(identityClaim);
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(nonce));
        Long index = dis.readLong();
        dis.close();
        TPM2ProvisionerState value = new TPM2ProvisionerState(nonce, identityClaim);
        when(tpm2ProvisionerStateRepository.findByFirstPartOfNonce(index)).thenReturn(value);
        TPM2ProvisionerState tpm2ProvisionerState =
                TPM2ProvisionerState.getTPM2ProvisionerState(tpm2ProvisionerStateRepository, new byte[7]);
        assertNull(tpm2ProvisionerState);
    }
}
