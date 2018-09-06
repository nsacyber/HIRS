package hirs.ima;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.codec.binary.Hex;
import org.testng.Assert;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import hirs.data.persist.Digest;
import hirs.data.persist.DigestAlgorithm;
import hirs.data.persist.IMABaselineRecord;
import hirs.data.persist.SimpleImaBaseline;

/**
 * This class contains utility methods and constants that can be used for IMA
 * testing.
 */
public final class IMATestUtil {

    /**
     * Returns the path to a usable IMA CSV file. The file is formatted
     * correctly and contains a few records that can be used for testing. This
     * file is useful because it only contains a few records that makes testing
     * easier.
     */
    public static final String VERIFY_FIELDS_CSV = "/ima/IMAVerifyFields.csv";

    /**
     * Name of the baseline created by {@link #getVerifyBaseline()}.
     */
    public static final String VERIFY_BASELINE_NAME = "VerifyBaseline";

    private static final Logger LOGGER
            = LogManager.getLogger(IMATestUtil.class);

    private IMATestUtil() {
        /* do nothing because utility class should not be instantiated */
    }

    /**
     * Returns the list of expected <code>IMABaselineRecord</code>s when the
     * {@link #VERIFY_FIELDS_CSV} is used.
     *
     * @return expected <code>IMABaselineRecord</code>s
     */
    public static Set<IMABaselineRecord> getExpectedRecords() {
        final String[] paths = {
                "/lib64/xtables/libxt_owner.so",
                "/lib64/xtables/libxt_physdev.so",
                "/lib64/xtables/libipt_SET.so"
        };
        final String[] hashes = {
                "0523143605ad73fe118926ee4cb6206b12b26521",
                "60f7dd9268d894f3a3fa8c9c0c752964aa9a1274",
                "632d128568e560b86302da90d6becb6c2eec090d"
        };
        final Set<IMABaselineRecord> records = new HashSet<>();

        try {
            for (int i = 0; i < paths.length; ++i) {
                final Digest digest = new Digest(
                        DigestAlgorithm.SHA1,
                        Hex.decodeHex(hashes[i].toCharArray())
                );
                records.add(new IMABaselineRecord(paths[i], digest));
            }
        } catch (Exception e) {
            throw new RuntimeException("unexpected exception", e);
        }
        return records;
    }

    /**
     * Asserts that all of the properties are the same for both records.
     *
     * @param baselineRecord
     *            basline record to test
     * @param expected
     *            expected baseline record value
     */
    public static void assertRecordsEqual(
            final IMABaselineRecord baselineRecord,
            final IMABaselineRecord expected) {
        Assert.assertEquals(baselineRecord, expected);
    }

    /**
     * Generates the <code>ImaBaseline</code> from the file
     * {@link #VERIFY_FIELDS_CSV}.
     *
     * @return test IMA baseline
     */
    public static SimpleImaBaseline getVerifyBaseline() {
        LOGGER.debug("generating verify baseline");
        SimpleImaBaseline  baseline = null;
        InputStream in = null;
        try {
            final SimpleImaBaselineGenerator baselineCreator
                    = new SimpleImaBaselineGenerator();
            LOGGER.debug("opening verify fields csv");
            in = IMATestUtil.class
                    .getResourceAsStream(IMATestUtil.VERIFY_FIELDS_CSV);
            baseline = baselineCreator.generateBaselineFromCSVFile(
                    VERIFY_BASELINE_NAME, in);
        } catch (Exception e) {
            LOGGER.error("unexpected exception", e);
            throw new RuntimeException("unexpected exception", e);
        } finally {
            if (in != null) {
                try {
                    LOGGER.debug("closing input stream");
                    in.close();
                } catch (Exception e) {
                    LOGGER.error("error closing istream", e);
                }
            }
        }
        return baseline;
    }

    /**
     * Gets a test URL.
     *
     * @return the test URL
     * @throws MalformedURLException if the URL is malformed
     */
    public static URL getYumRepoURL() throws MalformedURLException {
        return new URL("http://fakerepo.com");
    }
}
