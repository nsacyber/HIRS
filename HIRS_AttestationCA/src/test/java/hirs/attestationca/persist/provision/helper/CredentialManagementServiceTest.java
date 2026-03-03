package hirs.attestationca.persist.provision.helper;

import hirs.attestationca.persist.entity.manager.CertificateRepository;
import hirs.attestationca.persist.entity.userdefined.Certificate;
import hirs.attestationca.persist.provision.service.CredentialManagementService;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@see CredentialManagementService}.
 */
public class CredentialManagementServiceTest {

    private static final String EK_HEADER_TRUNCATED = "/certificates/nuc-1/ek_cert_7_byte_header_removed.cer";

    private static final String EK_UNTOUCHED = "/certificates/nuc-1/ek_cert_untouched.cer";

    @InjectMocks
    private CredentialManagementService credentialManagementService;

    @Mock
    private CertificateRepository certificateRepository;

    /**
     * Holds the AutoCloseable instance returned by openMocks.
     */
    private AutoCloseable mocks;

    /**
     * Setup mocks.
     */
    @BeforeEach
    public void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
    }

    /**
     * Tears down the mock instances.
     *
     * @throws Exception if there are any issues closing down mock instances
     */
    @AfterEach
    public void tearDown() throws Exception {
        if (mocks != null) {
            mocks.close();
        }
    }

    /**
     * Tests exception generated when providing a null EK byte array.
     */
    @Test
    public void processNullEndorsementCredential() {
        assertThrows(IllegalArgumentException.class, () ->
                credentialManagementService.storeEndorsementCredential(null,
                        "testName"));
    }

    /**
     * Tests exception generated when providing an empty array of bytes as the EK.
     */
    @Test
    public void processEmptyEndorsementCredential() {
        assertThrows(IllegalArgumentException.class, () ->
                credentialManagementService.storeEndorsementCredential(new byte[0], "testName"));
    }

    /**
     * Tests processing an invalid EK (too small of an array).
     */
    @Test
    public void processInvalidEndorsementCredentialCase1() {
        byte[] ekBytes = new byte[]{1};
        assertThrows(IllegalArgumentException.class, () ->
                credentialManagementService.storeEndorsementCredential(ekBytes, "testName"));
    }

    /**
     * Tests processing an invalid EK (garbage bytes of a reasonable length).
     */
    @Test
    public void processInvalidEndorsementCredentialCase2() {
        byte[] ekBytes = new byte[]{1, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0};
        assertThrows(IllegalArgumentException.class, () ->
                credentialManagementService.storeEndorsementCredential(ekBytes,
                        "testName"));
    }

    /**
     * Tests processing a valid EK with the 7 byte header intact.
     *
     * @throws IOException if an IO error occurs
     */
    @Test
    public void parseUntouchedEndorsementCredential() throws IOException {
        String path = Objects.requireNonNull(CredentialManagementServiceTest.class.getResource(EK_UNTOUCHED)).getPath();
        byte[] ekBytes = IOUtils.toByteArray(new FileInputStream(path));

        credentialManagementService.storeEndorsementCredential(ekBytes, "testName");
        verify(certificateRepository).save(any(Certificate.class));
    }

    /**
     * Tests processing a valid EK with the 7 byte header already stripped.
     *
     * @throws IOException if an IO error occurs
     */
    @Test
    public void parseHeaderTruncatedEndorsementCredential() throws IOException {
        String path = Objects.requireNonNull(CredentialManagementServiceTest.class.getResource(EK_HEADER_TRUNCATED))
                .getPath();
        byte[] ekBytes = IOUtils.toByteArray(new FileInputStream(path));

        credentialManagementService.storeEndorsementCredential(ekBytes, "testName");
        verify(certificateRepository).save(any(Certificate.class));
    }
}
