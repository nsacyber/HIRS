package hirs.appraiser;

import hirs.data.persist.Digest;
import hirs.data.persist.enums.DigestAlgorithm;
import hirs.data.persist.baseline.IMABaselineRecord;
import hirs.data.persist.baseline.ImaIgnoreSetBaseline;
import hirs.data.persist.baseline.SimpleImaBaseline;
import org.apache.commons.codec.binary.Base64;

/**
 * Utility class for common appraiser test cases.
 */
public final class AppraiserTestUtil {

    /**
     * File path for file that is not known to test report. The test report is
     * retrieved from {@link #getTestReport()}.
     */
    public static final String UNKNOWN_FILE = "ld.so.cache";
    /**
     * File hash for file that is not known to test report. The test report is
     * retrieved from {@link #getTestReport()}.
     */
    public static final String UNKNOWN_HASH = "mDD5gha0OCtZlnlGhPkOA+nVDeU=";
    /**
     * Index of entry into test report for unknown file. The test report is
     * retrieved from {@link #getTestReport()}.
     */
    public static final int UNKNOWN_INDEX = 4;

    private AppraiserTestUtil() {
        /* do nothing */
    }

    /**
     * Returns a small IMA report that can be used for testing.
     *
     * @return test report
     */
    public static IMAReport getTestReport() {
        final IMAReport report = new IMAReport();
        final String[] files = {"boot_aggregate", "/init", "ld-2.12.so",
                UNKNOWN_FILE};
        final String[] hashes = {"GZWGIEab7A6j99LNDyOkhFjjx3M=",
                "MyfXNfq/3Td35On6QLAimH7EKrI=", "ZhRN8cbGioNEcZ5o0wcqYNA25jA=",
                UNKNOWN_HASH};
        for (int i = 0; i < files.length; ++i) {
            final byte[] hash = Base64.decodeBase64(hashes[i]);
            final Digest digest = new Digest(DigestAlgorithm.SHA1, hash);
            report.addRecord(new IMAMeasurementRecord(files[i], digest));
        }
        return report;
    }

    /**
     * Returns a <code>IMAPolicy</code> that can be used for testing. This
     * policy will validate the test report from {@link #getTestReport()} except
     * for the entry at index {@link #UNKOWN_INDEX}.
     *
     * @return test policy
     */
    public static IMAPolicy getTestPolicy() {
        final IMAPolicy policy = new IMAPolicy("Test Policy");
        policy.setWhitelist(getTestWhitelist());
        policy.setRequiredSet(getTestRequiredSet());
        policy.setImaIgnoreSetBaseline(getTestIgnoreSet());
        policy.setFailOnUnknowns(true);
        return policy;
    }

    /**
     * Returns the whitelist part of the test policy.
     *
     * @return test whitelist
     */
    public static SimpleImaBaseline getTestWhitelist() {
        final SimpleImaBaseline whitelist = new SimpleImaBaseline("Test Policy Baseline");

        final String[] whitelistFiles = {"boot_aggregate"};
        final String[] whitelistHashes = {"GZWGIEab7A6j99LNDyOkhFjjx3M="};

        for (int i = 0; i < whitelistFiles.length; ++i) {
            final byte[] hash = Base64.decodeBase64(whitelistHashes[i]);
            final Digest digest = new Digest(DigestAlgorithm.SHA1, hash);
            whitelist.addToBaseline(new IMABaselineRecord(whitelistFiles[i],
                    digest));
        }

        return whitelist;
    }

    /**
     * Returns the required set policy for the test policy.
     *
     * @return test required set
     */
    public static SimpleImaBaseline getTestRequiredSet() {
        final SimpleImaBaseline requiredSet = new SimpleImaBaseline(
                "Test Policy Required Set");

        final String[] requiredFiles = {"/init", "ld-2.12.so"};
        final String[] requiredHashes = {"MyfXNfq/3Td35On6QLAimH7EKrI=",
                "ZhRN8cbGioNEcZ5o0wcqYNA25jA="};

        for (int i = 0; i < requiredFiles.length; ++i) {
            final byte[] hash = Base64.decodeBase64(requiredHashes[i]);
            final Digest digest = new Digest(DigestAlgorithm.SHA1, hash);
            requiredSet.addToBaseline(new IMABaselineRecord(requiredFiles[i],
                    digest));
        }

        return requiredSet;
    }

    /**
     * Returns the ignore set policy for the test policy. There is nothing in
     * this policy.
     *
     * @return ignore set policy
     */
    public static ImaIgnoreSetBaseline getTestIgnoreSet() {
        return new ImaIgnoreSetBaseline("Test Policy Ignore Set");
    }
}
