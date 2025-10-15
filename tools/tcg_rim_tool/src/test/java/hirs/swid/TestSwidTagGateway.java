package hirs.swid;

import hirs.utils.rim.ReferenceManifestValidator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertTrue;

//TODO tests are broken
public class TestSwidTagGateway {
    private static final String ATTRIBUTES_FILE = Objects.requireNonNull(
            TestSwidTagGateway.class.getClassLoader()
                    .getResource("rim_fields.json")).getPath();

    private static final String CA_CHAIN_FILE = Objects.requireNonNull(
            TestSwidTagGateway.class.getClassLoader()
                    .getResource("RimCertChain.pem")).getPath();

    private static final String SUPPORT_RIM_FILE = Objects.requireNonNull(
            TestSwidTagGateway.class.getClassLoader()
                    .getResource("TpmLog.bin")).getPath();

    private static SwidTagGateway gateway;

    private static ReferenceManifestValidator validator;

    private final String defaultOutput = "generated_swidTag.swidtag";

    private final String baseUserCert = "generated_user_cert.swidtag";

    private final String jksKeystoreFile = Objects.requireNonNull(TestSwidTagGateway.class.getClassLoader()
            .getResource("keystore.jks")).getPath();

    private final String signingCertFile = Objects.requireNonNull(TestSwidTagGateway.class.getClassLoader()
            .getResource("RimSignCert.pem")).getPath();

    private final String privateKeyFile = Objects.requireNonNull(TestSwidTagGateway.class.getClassLoader()
            .getResource("privateRimKey.pem")).getPath();

    private final String rfc3852CounterSignatureFile = Objects.requireNonNull(
            TestSwidTagGateway.class.getClassLoader()
                    .getResource("counterSignature.file")).getPath();

    private InputStream expectedFile;

    /**
     * Sets the variables used for the test.
     */
    @BeforeAll
    public static void setUp() {
        gateway = new SwidTagGateway();
        gateway.setRimEventLog(SUPPORT_RIM_FILE);
        gateway.setAttributesFile(ATTRIBUTES_FILE);
        validator = new ReferenceManifestValidator();
        validator.setRimEventLog(SUPPORT_RIM_FILE);
        validator.setTrustStoreFile(CA_CHAN_FILE);
    }

    /**
     * Cleans up after testing.
     * @throws Exception
     */
    @AfterEach
    public void tearDown() throws Exception {
        if (expectedFile != null) {
            expectedFile.close();
        }
    }

    /**
     * This test corresponds to the arguments -c option 1.
     * -c base -l TpmLog.bin -k privateRimKey.pem -p RimSignCert.pem
     * where RimSignCert.pem has the AIA extension.
     */
    @Test
    public void testCreateBaseUserCertNotEmbedded() {
        gateway.setDefaultCredentials(false);
        gateway.setPemCertificateFile(signingCertFile);
        gateway.setPemPrivateKeyFile(privateKeyFile);
        gateway.setEmbeddedCert(false);
        gateway.generateSwidTag(defaultOutput);
        expectedFile = TestSwidTagGateway.class.getClassLoader()
                .getResourceAsStream(baseUserCert);
        assertTrue(compareFileBytesToExpectedFile(defaultOutput));
        validator.setRim(defaultOutput);
        assertTrue(validator.validateRim(signingCertFile));
    }

    /**
     * This test creates the following base RIM with -c option 2.
     * -c base -l TpmLog.bin -k privateRimKey.pem -p RimSignCert.pem -e
     * And then validates it:
     * -v [base RIM] -l TpmLog.bin -t RimCertChain.pem
     */
    @Test
    public void testCreateBaseUserCertEmbedded() {
        gateway.setDefaultCredentials(false);
        gateway.setPemCertificateFile(signingCertFile);
        gateway.setPemPrivateKeyFile(privateKeyFile);
        gateway.setEmbeddedCert(true);
        gateway.generateSwidTag(defaultOutput);
        final String baseUserCertEmbed = "generated_user_cert_embed.swidtag";
        expectedFile = TestSwidTagGateway.class.getClassLoader()
                .getResourceAsStream(baseUserCertEmbed);
        assertTrue(compareFileBytesToExpectedFile(defaultOutput));
        validator.setRim(defaultOutput);
        assertTrue(validator.validateRim(signingCertFile));
    }

    /**
     * This test corresponds to the arguments -c option 3.
     * -c base -l TpmLog.bin -d
     */
    @Test
    public void testCreateBaseDefaultCert() {
        gateway.setDefaultCredentials(true);
        gateway.setJksTruststoreFile(jksKeystoreFile);
        gateway.generateSwidTag(defaultOutput);
        final String baseDefaultCert = "generated_default_cert.swidtag";
        expectedFile = TestSwidTagGateway.class.getClassLoader()
                .getResourceAsStream(baseDefaultCert);
        assertTrue(compareFileBytesToExpectedFile(defaultOutput));
        validator.setRim(defaultOutput);
        assertTrue(validator.validateRim(signingCertFile));
    }

    /**
     * This test corresponds to the arguments -c option 4.
     * -c base -l TpmLog.bin -d --timestamp rfc3339 2023-01-01T00:00:00Z
     */
    @Test
    public void testCreateTimestampRfc3339() {
        gateway.setDefaultCredentials(true);
        gateway.setJksTruststoreFile(jksKeystoreFile);
        gateway.setTimestampFormat("RFC3339");
        gateway.setTimestampArgument("2023-01-01T00:00:00Z");
        gateway.generateSwidTag(defaultOutput);
        final String baseRfc3339Timestamp = "generated_timestamp_rfc3339.swidtag";
        expectedFile = TestSwidTagGateway.class.getClassLoader()
                .getResourceAsStream(baseRfc3339Timestamp);
        assertTrue(compareFileBytesToExpectedFile(defaultOutput));
        validator.setRim(defaultOutput);
        assertTrue(validator.validateRim(signingCertFile));
    }

    /**
     * This test corresponds to the arguments for -c option 5.
     * -c base -l TpmLog.bin -d --timestamp rfc3852 countersignature.file
     */
    @Test
    public void testCreateTimestampRfc3852() {
        gateway.setDefaultCredentials(true);
        gateway.setJksTruststoreFile(jksKeystoreFile);
        gateway.setTimestampFormat("RFC3852");
        gateway.setTimestampArgument(rfc3852CounterSignatureFile);
        gateway.generateSwidTag(defaultOutput);
        final String baseRfc3852Timestamp = "generated_timestamp_rfc3852.swidtag";
        expectedFile = TestSwidTagGateway.class.getClassLoader()
                .getResourceAsStream(baseRfc3852Timestamp);
        assertTrue(compareFileBytesToExpectedFile(defaultOutput));
        validator.setRim(defaultOutput);
        assertTrue(validator.validateRim(signingCertFile));
    }

    /**
     * This test corresponds to the arguments -v <path>.
     */
    @Test
    public void testValidateSwidtagFile() {
        final String filepath = Objects.requireNonNull(TestSwidTagGateway.class.getClassLoader()
                .getResource(baseUserCert)).getPath();
        System.out.println("Validating file at " + filepath);
        validator.setRim(defaultOutput);
        assertTrue(validator.validateRim(signingCertFile));
    }

    /**
     * This method compares two files by bytes to determine if they are the same or not.
     *
     * @param file to be compared to the expected value.
     * @return true if they are equal, false if not.
     */
    private boolean compareFileBytesToExpectedFile(final String file) {
        FileInputStream testFile = null;
        try {
            int data;
            testFile = new FileInputStream(file);
            while ((data = testFile.read()) != -1) {
                int expected = expectedFile.read();
                if (data != expected) {
                    System.out.println("Expected: " + expected);
                    System.out.println("Got: " + data);
                    return false;
                }
            }
        } catch (IOException | NullPointerException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (testFile != null) {
                try {
                    testFile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    return false;
                }
            }
            if (expectedFile != null) {
                try {
                    expectedFile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    return false;
                }
            }
        }
        return true;
    }
}
