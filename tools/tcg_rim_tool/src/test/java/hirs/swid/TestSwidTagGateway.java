package hirs.swid;

import hirs.utils.rim.ReferenceManifestValidator;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

public class TestSwidTagGateway {
	private SwidTagGateway gateway;
	private ReferenceManifestValidator validator;
	private final String DEFAULT_OUTPUT = "generated_swidTag.swidtag";
	private final String BASE_USER_CERT = "generated_user_cert.swidtag";
	private final String BASE_USER_CERT_EMBED = "generated_user_cert_embed.swidtag";
	private final String BASE_DEFAULT_CERT = "generated_default_cert.swidtag";
	private final String BASE_RFC3339_TIMESTAMP = "generated_timestamp_rfc3339.swidtag";
	private final String BASE_RFC3852_TIMESTAMP = "generated_timestamp_rfc3852.swidtag";
	private final String ATTRIBUTES_FILE = TestSwidTagGateway.class.getClassLoader()
			.getResource("rim_fields.json").getPath();
	private final String JKS_KEYSTORE_FILE = TestSwidTagGateway.class.getClassLoader()
			.getResource("keystore.jks").getPath();
	private final String SIGNING_CERT_FILE = TestSwidTagGateway.class.getClassLoader()
			.getResource("RimSignCert.pem").getPath();
	private final String PRIVATE_KEY_FILE = TestSwidTagGateway.class.getClassLoader()
			.getResource("privateRimKey.pem").getPath();
	private final String CA_CHAIN_FILE = TestSwidTagGateway.class.getClassLoader()
			.getResource("RimCertChain.pem").getPath();
	private final String SUPPORT_RIM_FILE = TestSwidTagGateway.class.getClassLoader()
			.getResource("TpmLog.bin").getPath();
	private final String RFC3852_COUNTERSIGNATURE_FILE = TestSwidTagGateway.class.getClassLoader()
			.getResource("counterSignature.file").getPath();
	private InputStream expectedFile;

	@BeforeClass
	public void setUp() throws Exception {
		gateway = new SwidTagGateway();
		gateway.setRimEventLog(SUPPORT_RIM_FILE);
		gateway.setAttributesFile(ATTRIBUTES_FILE);
		validator = new ReferenceManifestValidator();
		validator.setRimEventLog(SUPPORT_RIM_FILE);
		validator.setTrustStoreFile(CA_CHAIN_FILE);
	}

	@AfterClass
	public void tearDown() throws Exception {
		if (expectedFile != null) {
			expectedFile.close();
		}
	}

	/**
	 * This test corresponds to the arguments:
	 * -c base -l TpmLog.bin -k privateRimKey.pem -p RimSignCert.pem
	 * where RimSignCert.pem has the AIA extension.
	 */
	@Test
	public void testCreateBaseUserCertNotEmbedded() {
		gateway.setDefaultCredentials(false);
		gateway.setPemCertificateFile(SIGNING_CERT_FILE);
		gateway.setPemPrivateKeyFile(PRIVATE_KEY_FILE);
		gateway.setEmbeddedCert(false);
		gateway.generateSwidTag(DEFAULT_OUTPUT);
		expectedFile = TestSwidTagGateway.class.getClassLoader()
				.getResourceAsStream(BASE_USER_CERT);
		Assert.assertTrue(compareFileBytesToExpectedFile(DEFAULT_OUTPUT));
		validator.setRim(DEFAULT_OUTPUT);
		Assert.assertTrue(validator.validateRim(SIGNING_CERT_FILE));
	}

	/**
	 * This test creates the following base RIM:
	 * -c base -l TpmLog.bin -k privateRimKey.pem -p RimSignCert.pem -e
	 * And then validates it:
	 * -v [base RIM] -l TpmLog.bin -t RimCertChain.pem
	 */
	@Test
	public void testCreateBaseUserCertEmbedded() {
		gateway.setDefaultCredentials(false);
		gateway.setPemCertificateFile(SIGNING_CERT_FILE);
		gateway.setPemPrivateKeyFile(PRIVATE_KEY_FILE);
		gateway.setEmbeddedCert(true);
		gateway.generateSwidTag(DEFAULT_OUTPUT);
		expectedFile = TestSwidTagGateway.class.getClassLoader()
				.getResourceAsStream(BASE_USER_CERT_EMBED);
		Assert.assertTrue(compareFileBytesToExpectedFile(DEFAULT_OUTPUT));
		validator.setRim(DEFAULT_OUTPUT);
		Assert.assertTrue(validator.validateRim(SIGNING_CERT_FILE));
	}

	/**
	 * This test corresponds to the arguments:
	 * -c base -l TpmLog.bin -d
	 */
	@Test
	public void testCreateBaseDefaultCert() {
		gateway.setDefaultCredentials(true);
		gateway.setJksTruststoreFile(JKS_KEYSTORE_FILE);
		gateway.generateSwidTag(DEFAULT_OUTPUT);
		expectedFile = TestSwidTagGateway.class.getClassLoader()
				.getResourceAsStream(BASE_DEFAULT_CERT);
		Assert.assertTrue(compareFileBytesToExpectedFile(DEFAULT_OUTPUT));
		validator.setRim(DEFAULT_OUTPUT);
		Assert.assertTrue(validator.validateRim(SIGNING_CERT_FILE));
	}

	/**
	 * This test corresponds to the arguments:
	 * -c base -l TpmLog.bin -d --timestamp rfc3339 2023-01-01T00:00:00Z
	 */
	@Test
	public void testCreateTimestampRfc3339() {
		gateway.setDefaultCredentials(true);
		gateway.setJksTruststoreFile(JKS_KEYSTORE_FILE);
		gateway.setTimestampFormat("RFC3339");
		gateway.setTimestampArgument("2023-01-01T00:00:00Z");
		gateway.generateSwidTag(DEFAULT_OUTPUT);
		expectedFile = TestSwidTagGateway.class.getClassLoader()
				.getResourceAsStream(BASE_RFC3339_TIMESTAMP);
		Assert.assertTrue(compareFileBytesToExpectedFile(DEFAULT_OUTPUT));
		validator.setRim(DEFAULT_OUTPUT);
		Assert.assertTrue(validator.validateRim(SIGNING_CERT_FILE));
	}

	/**
	 * This test corresponds to the arguments:
	 * -c base -l TpmLog.bin -d --timestamp rfc3852 countersignature.file
	 */
	@Test
	public void testCreateTimestampRfc3852() {
		gateway.setDefaultCredentials(true);
		gateway.setJksTruststoreFile(JKS_KEYSTORE_FILE);
		gateway.setTimestampFormat("RFC3852");
		gateway.setTimestampArgument(RFC3852_COUNTERSIGNATURE_FILE);
		gateway.generateSwidTag(DEFAULT_OUTPUT);
		expectedFile = TestSwidTagGateway.class.getClassLoader()
				.getResourceAsStream(BASE_RFC3852_TIMESTAMP);
		Assert.assertTrue(compareFileBytesToExpectedFile(DEFAULT_OUTPUT));
		validator.setRim(DEFAULT_OUTPUT);
		Assert.assertTrue(validator.validateRim(SIGNING_CERT_FILE));
	}

	/**
	 * This test corresponds to the arguments:
	 * -v <path>
	 */

	public void testvalidateSwidtagFile() {
		String filepath = TestSwidTagGateway.class.getClassLoader()
				.getResource(BASE_USER_CERT).getPath();
		System.out.println("Validating file at " + filepath);
		validator.setRim(DEFAULT_OUTPUT);
		Assert.assertTrue(validator.validateRim(SIGNING_CERT_FILE));
	}

	/**
	 * This method compares two files by bytes to determine if they are the same or not.
	 * @param file to be compared to the expected value.
	 * @return true if they are equal, false if not.
	 */
	private boolean compareFileBytesToExpectedFile(String file) {
		FileInputStream testFile = null;
		try {
			int data;
			testFile = new FileInputStream(file);
			while ((data = testFile.read()) != -1) {
				int	expected = expectedFile.read();
				if (data != expected) {
					System.out.println("Expected: " + expected);
					System.out.println("Got: " + data);
					return false;
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		} catch (NullPointerException e) {
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
