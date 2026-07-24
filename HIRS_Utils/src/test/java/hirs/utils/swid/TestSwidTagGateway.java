package hirs.utils.swid;

import hirs.utils.rim.ReferenceManifestValidator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestSwidTagGateway {

    @TempDir
    private static Path tempDir;

    private static final String ATTRIBUTES_FILE = Objects.requireNonNull(
            TestSwidTagGateway.class.getClassLoader()
                    .getResource("swid/rim_fields.json")).getPath();

    private static final String CA_CHAIN_FILE = Objects.requireNonNull(
            TestSwidTagGateway.class.getClassLoader()
                    .getResource("swid/RimCertChain.pem")).getPath();

    private static final String SUPPORT_RIM_FILE = Objects.requireNonNull(
            TestSwidTagGateway.class.getClassLoader()
                    .getResource("swid/Example.com.BIOS.01.rimel")).getPath();

    private static SwidTagGateway gateway;

    private static ReferenceManifestValidator validator;

    private final String defaultOutput = "generated_swidTag.swidtag";

    private final String jksKeystoreFile = Objects.requireNonNull(TestSwidTagGateway.class.getClassLoader()
            .getResource("swid/keystore.jks")).getPath();

    private final String signingCertFile = Objects.requireNonNull(TestSwidTagGateway.class.getClassLoader()
            .getResource("swid/RimSignCert.pem")).getPath();

    private final String privateKeyFile = Objects.requireNonNull(TestSwidTagGateway.class.getClassLoader()
            .getResource("swid/privateRimKey.pem")).getPath();

    private final String rfc3852CounterSignatureFile = Objects.requireNonNull(
            TestSwidTagGateway.class.getClassLoader()
                    .getResource("swid/counterSignature.file")).getPath();

    /**
     * Sets the variables used for the test.
     */
    @BeforeAll
    public static void setUp() {
        gateway = new SwidTagGateway();
        gateway.setRimEventLog(SUPPORT_RIM_FILE);
        gateway.setAttributesFile(ATTRIBUTES_FILE);
        validator = new ReferenceManifestValidator();
        validator.setTrustStoreFile(CA_CHAIN_FILE);
    }

    /**
     * This test corresponds to the arguments -c option 1.
     * -c base -l TpmLog.bin -k privateRimKey.pem -p RimSignCert.pem
     * where RimSignCert.pem has the AIA extension.
     */
    @Test
    public void testCreateBaseUserCertNotEmbedded() throws IOException {
        Path outputPath = tempDir.resolve(defaultOutput);

        gateway.setDefaultCredentials(false);
        gateway.setPemCertificateFile(signingCertFile);
        gateway.setPemPrivateKeyFile(privateKeyFile);
        gateway.setEmbeddedCert(false);
        gateway.generateSwidTag(outputPath.toString());

        validator.setRim(outputPath.toString());
        assertTrue(validator.validateBaseRim(signingCertFile));
    }

    /**
     * This test creates the following base RIM with -c option 2.
     * -c base -l TpmLog.bin -k privateRimKey.pem -p RimSignCert.pem -e
     * And then validates it:
     * -v [base RIM] -l TpmLog.bin -t RimCertChain.pem
     */
    @Test
    public void testCreateBaseUserCertEmbedded() throws IOException {
        Path outputPath = tempDir.resolve(defaultOutput);

        gateway.setDefaultCredentials(false);
        gateway.setPemCertificateFile(signingCertFile);
        gateway.setPemPrivateKeyFile(privateKeyFile);
        gateway.setEmbeddedCert(true);
        gateway.generateSwidTag(outputPath.toString());

        validator.setRim(outputPath.toString());
        assertTrue(validator.validateBaseRim(signingCertFile));
    }

    /**
     * This test corresponds to the arguments -c option 3.
     * -c base -l TpmLog.bin -d
     */
    @Test
    public void testCreateBaseDefaultCert() throws IOException {
        Path outputPath = tempDir.resolve(defaultOutput);

        gateway.setDefaultCredentials(true);
        gateway.setJksTruststoreFile(jksKeystoreFile);
        gateway.generateSwidTag(outputPath.toString());

        validator.setRim(outputPath.toString());
        assertTrue(validator.validateBaseRim(signingCertFile));
    }

    /**
     * This test corresponds to the arguments -c option 4.
     * -c base -l TpmLog.bin -d --timestamp rfc3339 2023-01-01T00:00:00Z
     */
    @Test
    public void testCreateTimestampRfc3339() throws IOException {
        Path outputPath = tempDir.resolve(defaultOutput);

        gateway.setDefaultCredentials(true);
        gateway.setJksTruststoreFile(jksKeystoreFile);
        gateway.setTimestampFormat("RFC3339");
        gateway.setTimestampArgument("2023-01-01T00:00:00Z");
        gateway.generateSwidTag(outputPath.toString());

        validator.setRim(outputPath.toString());
        assertTrue(validator.validateBaseRim(signingCertFile));
    }

    /**
     * This test corresponds to the arguments for -c option 5.
     * -c base -l TpmLog.bin -d --timestamp rfc3852 countersignature.file
     */
    @Test
    public void testCreateTimestampRfc3852() throws IOException {
        Path outputPath = tempDir.resolve(defaultOutput);

        gateway.setDefaultCredentials(true);
        gateway.setJksTruststoreFile(jksKeystoreFile);
        gateway.setTimestampFormat("RFC3852");
        gateway.setTimestampArgument(rfc3852CounterSignatureFile);
        gateway.generateSwidTag(outputPath.toString());

        validator.setRim(outputPath.toString());
        assertTrue(validator.validateBaseRim(signingCertFile));
    }

    /**
     * This test corresponds to the arguments -v <path>.
     */
    @Test
    public void testValidateSwidtagFile() throws IOException {
        Path outputPath = tempDir.resolve(defaultOutput);

        validator.setRim(outputPath.toString());
        assertTrue(validator.validateBaseRim(signingCertFile));
    }

    @Test
    @Order(Integer.MAX_VALUE)
    public void testValidateSupportRim() throws IOException {
        Path outputPath = tempDir.resolve(defaultOutput);
        Path supportRimPath = Path.of(SUPPORT_RIM_FILE).getParent();

        validator.setRim(outputPath.toString());
        validator.setHasSupportRim(true);
        validator.setSupportRimDirectory(supportRimPath.toString());

        assertTrue(validator.validateBaseRim(signingCertFile));
    }
}
