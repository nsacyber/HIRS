package hirs.utils.tpm.eventlog;


import hirs.utils.specLookups.TcgAlgRegLookups;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Arrays;


import static hirs.utils.tpm.eventlog.TCGEventLog.PCR_COUNT;


@Slf4j
public class TCGEventLogTest {

    private static final String DEFAULT_EVENT_LOG = "/tcgeventlog/TpmLog.bin";
    private static final String DEFAULT_EXPECTED_PCRS = "/tcgeventlog/TpmLogExpectedPcrs.txt";
    private static final String SHA1_EVENT_LOG = "/tcgeventlog/TpmLogSHA1.bin";
    private static final String SHA1_EXPECTED_PCRS = "/tcgeventlog/TpmLogSHA1ExpectedPcrs.txt";

    /**
     * Initializes a <code>SessionFactory</code>. The factory is used for an in-memory database that
     * is used for testing.
     */
    @BeforeAll
    public static final void setup() {
        log.debug("retrieving session factory");
    }

    /**
     * Closes the <code>SessionFactory</code> from setup.
     */
    @AfterAll
    public static final void tearDown() {
        log.debug("closing session factory");
    }

    /**
     * Tests the processing of a crypto agile event log.
     *
     * @throws IOException              when processing the test fails
     * @throws NoSuchAlgorithmException if an unknown algorithm is encountered.
     * @throws CertificateException     if a certificate fails to parse.
     */
    @Test
    public final void testCryptoAgileTCGEventLog() throws IOException, CertificateException,
            NoSuchAlgorithmException {
        log.debug("Testing the parsing of a Crypto Agile formatted TCG Event Log");


        try {
            // setup
            final InputStream inlog = this.getClass().getResourceAsStream(DEFAULT_EVENT_LOG);
            final InputStream pcrs = this.getClass().getResourceAsStream(DEFAULT_EXPECTED_PCRS);
            final byte[] rawLogBytes = IOUtils.toByteArray(inlog);
            final TCGEventLog evlog = new TCGEventLog(rawLogBytes, false, false, false);
            final String[] pcrFromLog = evlog.getExpectedPCRValues();
            final Object[] pcrObj = IOUtils.readLines(pcrs, "UTF-8").toArray();
            final String[] pcrTxt = Arrays.copyOf(pcrObj, pcrObj.length, String[].class);

            boolean testPass = true;

            // Test 1 get all PCRs
            for (int i = 0; i < PCR_COUNT; i++) {
                if (pcrFromLog[i].compareToIgnoreCase(pcrTxt[i]) != 0) {
                    testPass = false;
                    log.error("\ntestTCGEventLogProcessorParser error with PCR " + i);
                }
            }
            Assertions.assertTrue(testPass);

            // Test 2 get an individual PCR
            final int pcrIndex = 3;
            String pcr3 = evlog.getExpectedPCRValue(pcrIndex);
            Assertions.assertEquals(pcrFromLog[pcrIndex], pcr3);

            // Test 3 check the Algorithm String Identifier used in the log
            String algStr = evlog.getStrongestEvLogHashAlgName();
            Assertions.assertEquals(TcgAlgRegLookups.TcgHashAlg.SHA256.getAlgName(), algStr);

            // Test 4 check the Algorithm # Identifier used in the log
            int id = evlog.getStrongestEvLogHashAlgId();
            Assertions.assertEquals(TcgAlgRegLookups.TcgHashAlg.SHA256.getAlgId(), id);

            log.debug("OK. Parsing of a Crypto Agile Format Success");
        } catch (Throwable throwable) {
            throw throwable;
        }
    }

    /**
     * Tests the processing of a SHA1 formatted Event log.
     *
     * @throws IOException              when processing the test fails
     * @throws NoSuchAlgorithmException if an unknown algorithm is encountered.
     * @throws CertificateException     if a certificate fails to parse.
     */
    @Test
    public final void testSHA1TCGEventLog() throws IOException, CertificateException,
            NoSuchAlgorithmException {
        log.debug("Testing the parsing of a SHA1 formated TCG Event Log");

        try {
            // setup
            final InputStream inlog = this.getClass().getResourceAsStream(SHA1_EVENT_LOG);
            final InputStream pcrs = this.getClass().getResourceAsStream(SHA1_EXPECTED_PCRS);
            final byte[] rawLogBytes = IOUtils.toByteArray(inlog);
            final TCGEventLog evlog = new TCGEventLog(rawLogBytes, false, false, false);
            final String[] pcrFromLog = evlog.getExpectedPCRValues();
            final Object[] pcrObj = IOUtils.readLines(pcrs, "UTF-8").toArray();
            final String[] pcrTxt = Arrays.copyOf(pcrObj, pcrObj.length, String[].class);

            boolean testPass = true;

            // Test 1 get all PCRs
            for (int i = 0; i < PCR_COUNT; i++) {
                if (pcrFromLog[i].compareToIgnoreCase(pcrTxt[i]) != 0) {
                    testPass = false;
                    log.error("\ntestTCGEventLogProcessorParser error with PCR {}", i);
                }
            }
            Assertions.assertTrue(testPass);

            // Test 2 get an individual PCR
            String pcr0 = evlog.getExpectedPCRValue(0);
            Assertions.assertEquals(pcrFromLog[0], pcr0);

            // Test 3 check the Algorithm String Identifier used in the log
            String algStr = evlog.getStrongestEvLogHashAlgName();
            Assertions.assertEquals(TcgAlgRegLookups.TcgHashAlg.SHA1.getAlgName(), algStr);

            // Test 4 check the Algorithm # Identifier used in the log
            int id = evlog.getStrongestEvLogHashAlgId();
            Assertions.assertEquals(TcgAlgRegLookups.TcgHashAlg.SHA1.getAlgId(), id);

            log.debug("OK. Parsing of a SHA1 formatted TCG Event Log Success");
        } catch (Throwable throwable) {
            throw throwable;
        }
    }
}
