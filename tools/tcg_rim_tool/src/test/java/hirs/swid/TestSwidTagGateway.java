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
	private InputStream expectedFile;
	private static final String TEST_CSV_INPUT = "testCsv.swidtag";
	private static final String TEST_BLANK_SWIDTAG = "generated_swidTag.swidtag";
	
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

	@Test
	public void testGenerateSwidTagStringStringString() {
		outputFile = "testGenerateSwidTagStringStringString.swidtag";
		gateway.generateSwidTag(inputFile, outputFile, hashType);
		expectedFile = (InputStream) TestSwidTagGateway.class.getClassLoader().getResourceAsStream(TEST_CSV_INPUT);
		Assert.assertTrue(compareFileBytesToExpectedFile(outputFile));
	}

	@Test
	public void testGenerateSwidTagFile() {
		outputFile = "testGenerateSwidTagFile.swidtag";
		gateway.generateSwidTag(new File(outputFile));
		expectedFile = (InputStream) TestSwidTagGateway.class.getClassLoader().getResourceAsStream(TEST_BLANK_SWIDTAG);
		Assert.assertTrue(compareFileBytesToExpectedFile(outputFile));
	}

	@Test
	public void testValidateSwidTag() {
	    try {
	        Assert.assertTrue(gateway.validateSwidTag(TestSwidTagGateway.class.getClassLoader().getResource(TEST_BLANK_SWIDTAG).getPath()));
	    } catch (IOException e) {
	        Assert.fail("Invalid swidtag!");
	    }
	}

	@Test
	public void testParsePayload() {
	    InputStream is = null;
	    try {
    		is = gateway.parsePayload(outputFile);
    		Scanner scanner = new Scanner(is, "UTF-8");
    		String test = "PCR0,18382098108101841048";
    		String temp = "";
    		while (scanner.hasNext()) {
    		    temp = scanner.next();
    		}
    		Assert.assertEquals(test, temp);
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
