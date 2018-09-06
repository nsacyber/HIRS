package hirs.persist;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Random;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

        Assert.assertEquals(state.getNonce(), nonce);
        Assert.assertEquals(state.getIdentityClaim(), identityClaim);
    }

    /**
     * Test that the constructor throws an {@link IllegalArgumentException} when a null is
     * passed in for the nonce.
     *
     * @throws IOException this will never happen
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public final void testNullNonce() throws IOException {
        Random rand = new Random();
        byte[] nonce = null;
        byte[] identityClaim = new byte[360];
        rand.nextBytes(identityClaim);
        new TPM2ProvisionerState(nonce, identityClaim);
    }

    /**
     * Test that the constructor throws an {@link IllegalArgumentException} when a null is
     * passed in for the identity claim.
     *
     * @throws IOException this will never happen
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public final void testNullIdentityClaim() throws IOException {
        Random rand = new Random();
        byte[] nonce = new byte[32];
        byte[] identityClaim = null;
        rand.nextBytes(nonce);
        new TPM2ProvisionerState(nonce, identityClaim);
    }

    /**
     * Test that the constructor throws an {@link IllegalArgumentException} when a nonce is
     * passed in that is less than 8 bytes.
     * @throws IOException this will never happen
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public final void testNonceToSmall() throws IOException {
        Random rand = new Random();
        byte[] nonce = new byte[7];
        byte[] identityClaim = new byte[360];
        rand.nextBytes(nonce);
        rand.nextBytes(identityClaim);
        new TPM2ProvisionerState(nonce, identityClaim);
    }

    /**
     * Test that {@link TPM2ProvisionerState#getTPM2ProvisionerState(CrudManager, byte[])} works.
     *
     * {@link TPM2ProvisionerState#getTPM2ProvisionerState(CrudManager, byte[])}, null is returned.
     * @throws IOException this will never happen
     */
    @Test
    @SuppressWarnings("unchecked")
    public final void testGetTPM2ProvisionerStateNominal() throws IOException {
        CrudManager<TPM2ProvisionerState> crudManager = mock(CrudManager.class);
        Random rand = new Random();
        byte[] nonce = new byte[32];
        byte[] identityClaim = new byte[360];
        rand.nextBytes(nonce);
        rand.nextBytes(identityClaim);

        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(nonce));
        Long index = dis.readLong();
        dis.close();
        TPM2ProvisionerState value = new TPM2ProvisionerState(nonce, identityClaim);
        when(crudManager.get(index)).thenReturn(value);
        TPM2ProvisionerState tpm2ProvisionerState
                = TPM2ProvisionerState.getTPM2ProvisionerState(crudManager, nonce);
        Assert.assertEquals(tpm2ProvisionerState.getIdentityClaim(), value.getIdentityClaim());
    }

    /**
     * Test that if a null is passed as a nonce to
     * {@link TPM2ProvisionerState#getTPM2ProvisionerState(CrudManager, byte[])}, null is returned.
     * @throws IOException this will never happen
     */
    @Test
    @SuppressWarnings("unchecked")
    public final void testGetTPM2ProvisionerStateNullNonce() throws IOException {
        CrudManager<TPM2ProvisionerState> crudManager = mock(CrudManager.class);
        Random rand = new Random();
        byte[] nonce = new byte[32];
        byte[] identityClaim = new byte[360];
        rand.nextBytes(nonce);
        rand.nextBytes(identityClaim);
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(nonce));
        Long index = dis.readLong();
        dis.close();
        TPM2ProvisionerState value = new TPM2ProvisionerState(nonce, identityClaim);
        when(crudManager.get(index)).thenReturn(value);
        TPM2ProvisionerState tpm2ProvisionerState
                = TPM2ProvisionerState.getTPM2ProvisionerState(crudManager, null);
        Assert.assertNull(tpm2ProvisionerState);

    }

    /**
     * Test that if a nonce that is less than 8 bytes is passed to
     * {@link TPM2ProvisionerState#getTPM2ProvisionerState(CrudManager, byte[])}, null is returned.
     * @throws IOException this will never happen
     */
    @Test
    @SuppressWarnings("unchecked")
    public final void testGetTPM2ProvisionerStateNonceTooSmall() throws IOException {
        CrudManager<TPM2ProvisionerState> crudManager = mock(CrudManager.class);
        Random rand = new Random();
        byte[] nonce = new byte[32];
        byte[] identityClaim = new byte[360];
        rand.nextBytes(nonce);
        rand.nextBytes(identityClaim);
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(nonce));
        Long index = dis.readLong();
        dis.close();
        TPM2ProvisionerState value = new TPM2ProvisionerState(nonce, identityClaim);
        when(crudManager.get(index)).thenReturn(value);
        TPM2ProvisionerState tpm2ProvisionerState =
                TPM2ProvisionerState.getTPM2ProvisionerState(crudManager, new byte[7]);
        Assert.assertNull(tpm2ProvisionerState);

    }

}
