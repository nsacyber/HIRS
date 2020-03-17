package hirs.swid;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

public class TestSwidTagGateway {
	private SwidTagGateway gateway;
	private final String DEFAULT_OUTPUT = "generated_swidTag.swidtag";
	private final String DEFAULT_WITH_CERT = "generated_with_cert.swidtag";
	private final String DEFAULT_NO_CERT = "generated_no_cert.swidtag";
	private final String certificateFile = "RimSignCert.pem";
	private final String privateKeyFile = "privateRimKey.pem";
	private InputStream expectedFile;

	@BeforeClass
	public void setUp() throws Exception {
		gateway = new SwidTagGateway();
	}

	@AfterClass
	public void tearDown() throws Exception {
		if (expectedFile != null) {
			expectedFile.close();
		}
	}

	/**
	 * This test corresponds to the arguments:
	 * -c base -k privateRimKey.pem -p RimSignCert.pem
	 */
	@Test
	public void testCreateBaseWithCert() {
		gateway.setDefaultCredentials(false);
		gateway.setPemCertificateFile(certificateFile);
		gateway.setPemPrivateKeyFile(privateKeyFile);
		gateway.generateSwidTag(DEFAULT_OUTPUT);
		expectedFile = (InputStream) TestSwidTagGateway.class.getClassLoader().getResourceAsStream(DEFAULT_WITH_CERT);
		Assert.assertTrue(compareFileBytesToExpectedFile(DEFAULT_OUTPUT));
	}

	/**
	 * This test corresponds to the arguments:
	 * -c base
	 * -c base -a attributes.json
	 */
	@Test
	public void testCreateBaseWithoutCert() {
		gateway.setDefaultCredentials(true);
		gateway.generateSwidTag(DEFAULT_OUTPUT);
		expectedFile = (InputStream) TestSwidTagGateway.class.getClassLoader().getResourceAsStream(DEFAULT_NO_CERT);
		Assert.assertTrue(compareFileBytesToExpectedFile(DEFAULT_OUTPUT));
	}

	/**
	 * Validate a base RIM with default attributes with an X509Certificate element.
	 */
	@Test
	public void testValidateSwidTag() {
	    try {
	        Assert.assertTrue(gateway.validateSwidTag(TestSwidTagGateway.class.getClassLoader().getResource(DEFAULT_WITH_CERT).getPath()));
	    } catch (IOException e) {
	        Assert.fail("Invalid swidtag!");
	    }
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
