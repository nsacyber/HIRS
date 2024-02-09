package hirs.attestationca.persist.provision.helper;

import hirs.attestationca.persist.entity.manager.CertificateRepository;
import hirs.attestationca.persist.entity.userdefined.Certificate;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.FileInputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@see CredentialManagementHelper}.
 */
public class CredentialManagementHelperTest {

    @Mock
    private CertificateRepository certificateRepository;

    private static final String EK_HEADER_TRUNCATED
            = "/certificates/nuc-1/ek_cert_7_byte_header_removed.cer";
    private static final String EK_UNTOUCHED
            = "/certificates/nuc-1/ek_cert_untouched.cer";

    /**
     * Setup mocks.
     */
    @BeforeEach
    public void setUp() {
        //certificateRepository = mock(CertificateRepository.class);
        MockitoAnnotations.initMocks(this);
    }

    /**
     * Tests exception generated if providing a null cert repository.
     * @throws IOException if an IO error occurs
     */
    @Test
    public void processNullCertRep() throws IOException {
        // use valid EK byte array
        String path = CredentialManagementHelperTest.class.getResource(EK_UNTOUCHED).getPath();
        byte[] ekBytes = IOUtils.toByteArray(new FileInputStream(path));
        assertThrows(IllegalArgumentException.class, () ->
            CredentialManagementHelper.storeEndorsementCredential(null, ekBytes, "testName"));
    }

    /**
     * Tests exception generated when providing a null EK byte array.
     */
    @Test
    public void processNullEndorsementCredential() {
        assertThrows(IllegalArgumentException.class, () ->
            CredentialManagementHelper.storeEndorsementCredential(certificateRepository, null, "testName"));
    }

    /**
     * Tests exception generated when providing an empty array of bytes as the EK.
     */
    @Test
    public void processEmptyEndorsementCredential() {
        assertThrows(IllegalArgumentException.class, () ->
            CredentialManagementHelper.storeEndorsementCredential(certificateRepository, new byte[0], "testName"));
    }

    /**
     * Tests processing an invalid EK (too small of an array).
     */
    @Test
    public void processInvalidEndorsementCredentialCase1() {
        byte[] ekBytes = new byte[] {1};
        assertThrows(IllegalArgumentException.class, () ->
                CredentialManagementHelper.storeEndorsementCredential(certificateRepository, ekBytes, "testName"));
    }

    /**
     * Tests processing an invalid EK (garbage bytes of a reasonable length).
     */
    @Test
    public void processInvalidEndorsementCredentialCase2() {
        byte[] ekBytes = new byte[] {1, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0};
        assertThrows(IllegalArgumentException.class, () ->
        CredentialManagementHelper.storeEndorsementCredential(certificateRepository, ekBytes, "testName"));
    }

    /**
     * Tests processing a valid EK with the 7 byte header in tact.
     * @throws IOException if an IO error occurs
     */
    @Test
    public void parseUntouchedEndorsementCredential() throws IOException {
        String path = CredentialManagementHelperTest.class.getResource(EK_UNTOUCHED).getPath();
        byte[] ekBytes = IOUtils.toByteArray(new FileInputStream(path));

        CredentialManagementHelper.storeEndorsementCredential(certificateRepository, ekBytes, "testName");
        verify(certificateRepository).save(any(Certificate.class));
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

        CredentialManagementHelper.storeEndorsementCredential(certificateRepository, ekBytes, "testName");
        verify(certificateRepository).save(any(Certificate.class));
    }
}
