package hirs.attestationca.persist.entity.tpm;

import hirs.attestationca.persist.entity.manager.TPM2ProvisionerStateRepository;
import hirs.attestationca.persist.provision.service.Tpm2ProvisionerStateService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * Contains unit tests for {@link TPM2ProvisionerState} and {@link Tpm2ProvisionerStateService}.
 */
public class TPM2ProvisionerStateServiceTest {

    private static final Random RANDOM_GENERATOR = new Random();

    @InjectMocks
    private Tpm2ProvisionerStateService tpm2ProvisionerStateService;

    @Mock
    private TPM2ProvisionerStateRepository tpm2ProvisionerStateRepository;

    private AutoCloseable mocks;

    /**
     * Setups configuration prior to each test method.
     */
    @BeforeEach
    public void setupTests() {
        // Initializes mocks before each test
        mocks = MockitoAnnotations.openMocks(this);
    }

    /**
     * Closes mocks after the completion of each test method.
     *
     * @throws Exception if any issues arise while closing mocks.
     */
    @AfterEach
    public void afterEach() throws Exception {
        if (mocks != null) {
            mocks.close();
        }
    }

    /**
     * Tests that the values passed to the constructor are equal to the values
     * returned by the getters.
     */
    @Test
    public final void testTPM2ProvisionerState() {
        final int nonceSize = 32;
        final int identityClaimSize = 360;
        byte[] nonce = new byte[nonceSize];
        byte[] identityClaim = new byte[identityClaimSize];

        RANDOM_GENERATOR.nextBytes(nonce);
        RANDOM_GENERATOR.nextBytes(identityClaim);

        final TPM2ProvisionerState state = new TPM2ProvisionerState(nonce, identityClaim);

        assertArrayEquals(nonce, state.getNonce());
        assertArrayEquals(identityClaim, state.getIdentityClaim());
    }

    /**
     * Test that the constructor throws an {@link IllegalArgumentException} when a null is
     * passed in for the nonce.
     *
     * @throws IllegalArgumentException if any issues any arise while retrieving the TPM Provisioner State
     */
    @Test
    public final void testNullNonce() throws IllegalArgumentException {
        final int identityClaimSize = 360;
        byte[] identityClaim = new byte[identityClaimSize];

        RANDOM_GENERATOR.nextBytes(identityClaim);
        assertThrows(IllegalArgumentException.class, () -> new TPM2ProvisionerState(null, identityClaim));
    }

    /**
     * Test that the constructor throws an {@link IllegalArgumentException} when a null is
     * passed in for the identity claim.
     *
     * @throws IllegalArgumentException if any issues any arise while retrieving the TPM Provisioner State
     */
    @Test
    public final void testNullIdentityClaim() throws IllegalArgumentException {
        final int nonceSize = 32;
        byte[] nonce = new byte[nonceSize];

        RANDOM_GENERATOR.nextBytes(nonce);

        assertThrows(IllegalArgumentException.class, () -> new TPM2ProvisionerState(nonce, null));
    }

    /**
     * Test that the constructor throws an {@link IllegalArgumentException} when a nonce is
     * passed in that is less than 8 bytes.
     *
     * @throws IllegalArgumentException if any issues any arise while retrieving the TPM Provisioner State
     */
    @Test
    public final void testNonceToSmall() throws IllegalArgumentException {
        final int nonceSize = 7;
        final int identityClaimSize = 360;
        byte[] nonce = new byte[nonceSize];
        byte[] identityClaim = new byte[identityClaimSize];

        RANDOM_GENERATOR.nextBytes(nonce);
        RANDOM_GENERATOR.nextBytes(identityClaim);
        assertThrows(IllegalArgumentException.class, () -> new TPM2ProvisionerState(nonce, identityClaim));
    }

    /**
     * Test the {@link Tpm2ProvisionerStateService#getTPM2ProvisionerState(byte[])} function call.
     *
     * @throws IOException if any issues any arise while retrieving the TPM Provisioner State
     */
    @Test
    public final void testGetTPM2ProvisionerStateNominal() throws IOException {

        final int nonceSize = 32;
        final int identityClaimSize = 360;
        byte[] nonce = new byte[nonceSize];
        byte[] identityClaim = new byte[identityClaimSize];

        RANDOM_GENERATOR.nextBytes(nonce);
        RANDOM_GENERATOR.nextBytes(identityClaim);

        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(nonce));
        Long index = dis.readLong();
        dis.close();

        final TPM2ProvisionerState value = new TPM2ProvisionerState(nonce, identityClaim);
        when(tpm2ProvisionerStateRepository.findByFirstPartOfNonce(index)).thenReturn(value);

        final TPM2ProvisionerState tpm2ProvisionerState = tpm2ProvisionerStateService.getTPM2ProvisionerState(nonce);
        assertNotNull(tpm2ProvisionerState);
        assertArrayEquals(value.getIdentityClaim(), tpm2ProvisionerState.getIdentityClaim());
    }

    /**
     * Test that if a null is passed as a nonce to
     * {@link Tpm2ProvisionerStateService#getTPM2ProvisionerState(byte[])}, null is returned.
     *
     * @throws IOException if any issues any arise while retrieving the TPM Provisioner State
     */
    @Test
    public final void testGetTPM2ProvisionerStateNullNonce() throws IOException {
        final int nonceSize = 32;
        final int identityClaimSize = 360;
        byte[] nonce = new byte[nonceSize];
        byte[] identityClaim = new byte[identityClaimSize];

        RANDOM_GENERATOR.nextBytes(nonce);
        RANDOM_GENERATOR.nextBytes(identityClaim);

        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(nonce));
        Long index = dis.readLong();
        dis.close();

        final TPM2ProvisionerState value = new TPM2ProvisionerState(nonce, identityClaim);
        when(tpm2ProvisionerStateRepository.findByFirstPartOfNonce(index)).thenReturn(value);

        assertThrows(NullPointerException.class, () -> tpm2ProvisionerStateService.getTPM2ProvisionerState(null));
    }

    /**
     * Test that if a nonce that is less than 8 bytes is passed to
     * {@link Tpm2ProvisionerStateService#getTPM2ProvisionerState(byte[])}, null is returned.
     *
     * @throws IOException if any issues any arise while retrieving the TPM Provisioner State
     */
    @Test
    public final void testGetTPM2ProvisionerStateNonceTooSmall() throws IOException {
        final int nonceSize = 32;
        final int identityClaimSize = 360;
        byte[] nonce = new byte[nonceSize];
        byte[] identityClaim = new byte[identityClaimSize];

        RANDOM_GENERATOR.nextBytes(nonce);
        RANDOM_GENERATOR.nextBytes(identityClaim);

        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(nonce));
        Long index = dis.readLong();
        dis.close();

        final int nonce2Size = 7;
        final TPM2ProvisionerState value = new TPM2ProvisionerState(nonce, identityClaim);

        when(tpm2ProvisionerStateRepository.findByFirstPartOfNonce(index)).thenReturn(value);
        final TPM2ProvisionerState tpm2ProvisionerState =
                tpm2ProvisionerStateService.getTPM2ProvisionerState(new byte[nonce2Size]);

        assertNull(tpm2ProvisionerState);
    }
}
