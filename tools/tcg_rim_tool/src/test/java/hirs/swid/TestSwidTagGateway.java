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
	private String inputFile, outputFile, hashType;
	private final String DEFAULT_OUTPUT = "generated_swidTag.swidtag";
	private final String DEFAULT_WITH_CERT = "generated_with_cert.swidtag";
	private final String DEFAULT_NO_CERT = "generated_no_cert.swidtag";
	private InputStream expectedFile;

	@BeforeClass
	public void setUp() throws Exception {
		gateway = new SwidTagGateway();
		inputFile = TestSwidTagGateway.class.getClassLoader().getResource("examplecsv.csv").getFile();
		hashType = "SHA256";
	}

	@AfterClass
	public void tearDown() throws Exception {
		if (expectedFile != null) {
			expectedFile.close();
		}
	}

	/**
	 * Creating a base RIM with default attributes with an X509Certificate element.
	 */
	@Test
	public void testGenerateDefaultWithCert() {
		gateway.setShowCert(true);
		gateway.generateSwidTag();
		expectedFile = (InputStream) TestSwidTagGateway.class.getClassLoader().getResourceAsStream(DEFAULT_WITH_CERT);
		Assert.assertTrue(compareFileBytesToExpectedFile(DEFAULT_OUTPUT));
	}

	/**
	 * Create a base RIM with default attributes without an X509Certificate element.
	 */
	@Test
	public void testGenerateDefaultNoCert() {
		gateway.setShowCert(false);
		gateway.generateSwidTag();
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
	 * Verify expected values of a File element in a Payload element.
	 */
	@Test
	public void testParsePayload() {
	    InputStream is = null;
		outputFile = TestSwidTagGateway.class.getClassLoader().getResource(DEFAULT_WITH_CERT).getPath();
	    try {
    		is = gateway.parsePayload(outputFile);
    		Scanner scanner = new Scanner(is, "UTF-8");
    		String test = "Example.com.iotBase.bin,688e293e3ccb522f6cf8a027c9ade7960f84bd0bf3a0b99812bc1fa498a2db8d";
    		String temp = "";
    		while (scanner.hasNext()) {
    		    temp = scanner.next();
				Assert.assertEquals(temp, test, "temp: " + temp + ", test: " + test);
    		}
	    } catch (IOException e) {
	        Assert.fail("Error parsing test file!");
	    } finally {
	        if (is != null) {
	            try {
	                is.close();
	            } catch (IOException e) {
	            	Assert.fail("Failed to close input stream!");
	            }
	        }
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
