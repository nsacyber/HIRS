package hirs.attestationca;

import hirs.data.persist.certificate.Certificate;
import hirs.persist.CertificateManager;
import org.apache.commons.io.IOUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.FileInputStream;
import java.io.IOException;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@see CredentialManagementHelper}.
 */
public class CredentialManagementHelperTest {

    private CertificateManager certMan;

    private static final String EK_HEADER_TRUNCATED
            = "/certificates/nuc-1/ek_cert_7_byte_header_removed.cer";
    private static final String EK_UNTOUCHED
            = "/certificates/nuc-1/ek_cert_untouched.cer";

    /**
     * Setup mocks.
     */
    @BeforeMethod
    public void setUp() {
        certMan = mock(CertificateManager.class);
    }

    /**
     * Tests exception generated if providing a null cert manager.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void processNullCertMan() {
        CredentialManagementHelper.storeEndorsementCredential(null, null);
    }

    /**
     * Tests exception generated when providing a null EK byte array.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void processNullEndorsementCredential() {
        CredentialManagementHelper.storeEndorsementCredential(certMan, null);
    }

    /**
     * Tests exception generated when providing an empty array of bytes as the EK.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void processEmptyEndorsementCredential() {
        CredentialManagementHelper.storeEndorsementCredential(certMan, new byte[0]);
    }

    /**
     * Tests processing an invalid EK (too small of an array).
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void processInvalidEndorsementCredentialCase1() {
        byte[] ekBytes = new byte[] {1};
                CredentialManagementHelper.storeEndorsementCredential(certMan, ekBytes);
    }

    /**
     * Tests processing an invalid EK (garbage bytes of a reasonable length).
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void processInvalidEndorsementCredentialCase2() {
        byte[] ekBytes = new byte[] {1, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0};
        CredentialManagementHelper.storeEndorsementCredential(certMan, ekBytes);
    }

    /**
     * Tests processing a valid EK with the 7 byte header in tact.
     * @throws IOException if an IO error occurs
     */
    @Test
    public void parseUntouchedEndorsementCredential() throws IOException {
        String path = CredentialManagementHelperTest.class.getResource(EK_UNTOUCHED).getPath();
        byte[] ekBytes = IOUtils.toByteArray(new FileInputStream(path));

        CredentialManagementHelper.storeEndorsementCredential(certMan, ekBytes);
        verify(certMan).saveCertificate(any(Certificate.class));
    }

    /**
     * Tests processing a valid EK with the 7 byte header already stripped.
     * @throws IOException if an IO error occurs
     */
    @Test
    public void parseHeaderTruncatedEndorsementCredential() throws IOException {
        String path = CredentialManagementHelperTest.class.getResource(EK_HEADER_TRUNCATED)
                .getPath();
        byte[] ekBytes = IOUtils.toByteArray(new FileInputStream(path));

        CredentialManagementHelper.storeEndorsementCredential(certMan, ekBytes);
        verify(certMan).saveCertificate(any(Certificate.class));
    }
}
