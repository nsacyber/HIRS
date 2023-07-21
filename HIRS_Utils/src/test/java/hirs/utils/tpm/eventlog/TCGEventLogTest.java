package hirs.utils.tpm.eventlog;


import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Arrays;

import org.apache.commons.io.IOUtils;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class TCGEventLogTest {

    private static final String DEFAULT_EVENT_LOG = "/tcgeventlog/TpmLog.bin";
    private static final String DEFAULT_EXPECTED_PCRS = "/tcgeventlog/TpmLogExpectedPcrs.txt";
    private static final String SHA1_EVENT_LOG = "/tcgeventlog/TpmLogSHA1.bin";
    private static final String SHA1_EXPECTED_PCRS = "/tcgeventlog/TpmLogSHA1ExpectedPcrs.txt";
    private static final Logger LOGGER
            = LogManager.getLogger(TCGEventLogTest.class);

    /**
     * Initializes a <code>SessionFactory</code>. The factory is used for an in-memory database that
     * is used for testing.
     */
    @BeforeAll
    public static final void setup() {
        LOGGER.debug("retrieving session factory");

    }

    /**
     * Closes the <code>SessionFactory</code> from setup.
     */
    @AfterAll
	public static final void tearDown() {
	    LOGGER.debug("closing session factory");
	}

  /**
    * Tests the processing of a crypto agile event log.
    * @throws IOException when processing the test fails
    * @throws NoSuchAlgorithmException if an unknown algorithm is encountered.
    * @throws CertificateException if a certificate fails to parse.
    */
    @Test
    public final void testCryptoAgileTCGEventLog() throws IOException, CertificateException,
                                                                 NoSuchAlgorithmException {
      LOGGER.debug("Testing the parsing of a Crypto Agile formatted TCG Event Log");
      InputStream log, pcrs;
      boolean testPass = true;
      log = this.getClass().getResourceAsStream(DEFAULT_EVENT_LOG);
      byte[] rawLogBytes = IOUtils.toByteArray(log);
      TCGEventLog evlog = new TCGEventLog(rawLogBytes, false, false, false);
      String[] pcrFromLog = evlog.getExpectedPCRValues();
      pcrs = this.getClass().getResourceAsStream(DEFAULT_EXPECTED_PCRS);
      Object[] pcrObj = IOUtils.readLines(pcrs, "UTF-8").toArray();
      String[] pcrTxt = Arrays.copyOf(pcrObj, pcrObj.length, String[].class);
      
      // Test 1 get all PCRs
      for (int i = 0; i < 24; i++) {
          if (pcrFromLog[i].compareToIgnoreCase(pcrTxt[i]) != 0) {
              testPass = false;
              LOGGER.error("\ntestTCGEventLogProcessorParser error with PCR " + i);
          }
      }
      assertTrue(testPass);
      
      // Test 2 get an individual PCR
      String pcr3 = evlog.getExpectedPCRValue(3);
      assertThat(pcrFromLog[3], equalTo(pcr3));

      // Test 3 check the Algorithm String Identifier used in the log
      String algStr = evlog.getEventLogHashAlgorithm();
      assertThat("TPM_ALG_SHA256", equalTo(algStr));

      // Test 4 check the Algorithm # Identifier used in the log
      int id = evlog.getEventLogHashAlgorithmID();
      assertThat(TcgTpmtHa.TPM_ALG_SHA256, equalTo(id));
      
      LOGGER.debug("OK. Parsing of a Crypto Agile Format Success");
    }

    /**
     * Tests the processing of a SHA1 formatted Event log.
     * @throws IOException when processing the test fails
     * @throws NoSuchAlgorithmException if an unknown algorithm is encountered.
     * @throws CertificateException if a certificate fails to parse.
     */
    @Test
    public final void testSHA1TCGEventLog() throws IOException, CertificateException,
                                                            NoSuchAlgorithmException {
      LOGGER.debug("Testing the parsing of a SHA1 formated TCG Event Log");
      InputStream log, pcrs;
      boolean testPass = true;
      log = this.getClass().getResourceAsStream(SHA1_EVENT_LOG);
      byte[] rawLogBytes = IOUtils.toByteArray(log);
      TCGEventLog evlog =  new TCGEventLog(rawLogBytes, false, false, false);
      String[] pcrFromLog = evlog.getExpectedPCRValues();
      pcrs = this.getClass().getResourceAsStream(SHA1_EXPECTED_PCRS);
      Object[] pcrObj = IOUtils.readLines(pcrs, "UTF-8").toArray();
      String[] pcrTxt = Arrays.copyOf(pcrObj, pcrObj.length, String[].class);
      
      // Test 1 get all PCRs
       for (int i = 0; i < 24; i++) {
          if (pcrFromLog[i].compareToIgnoreCase(pcrTxt[i]) != 0) {
             testPass = false;
             LOGGER.error("\ntestTCGEventLogProcessorParser error with PCR " + i);
           }
       }
       assertTrue(testPass);
       
       // Test 2 get an individual PCR
       String pcr0 = evlog.getExpectedPCRValue(0);
       assertThat(pcrFromLog[0], equalTo(pcr0));
       
       // Test 3 check the Algorithm String Identifier used in the log
       String algStr = evlog.getEventLogHashAlgorithm();
       assertThat("TPM_ALG_SHA1", equalTo(algStr));

       // Test 4 check the Algorithm # Identifier used in the log
       int id = evlog.getEventLogHashAlgorithmID();
       assertThat(TcgTpmtHa.TPM_ALG_SHA1, equalTo(id));
       
       LOGGER.debug("OK. Parsing of a SHA1 formatted TCG Event Log Success");
     }
  
}
