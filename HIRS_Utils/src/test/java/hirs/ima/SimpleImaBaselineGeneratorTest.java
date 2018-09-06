package hirs.ima;

import hirs.data.persist.IMAReport;
import hirs.data.persist.SimpleImaBaseline;
import hirs.data.persist.DeviceInfoReport;
import hirs.data.persist.Digest;
import hirs.data.persist.DigestAlgorithm;
import hirs.data.persist.ImaBaseline;
import hirs.data.persist.IMABaselineRecord;
import hirs.data.persist.IntegrityReport;
import hirs.data.persist.TPMReport;

import java.io.InputStream;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.codec.binary.Base64;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertEquals;
import org.testng.annotations.Test;

/**
 * <code>CreateIMABaselineTest</code> is a unit test class for the
 * <code>CreateIMABaseline</code> class.
 */
public class SimpleImaBaselineGeneratorTest {

    private static final String DEFAULT_REPORT = "/reports/ima/ima.xml";
    private static final String DEFAULT_INTEGRITY_REPORT = "/reports/integrity/"
            + "integrity_report_with_AIK_certificate.xml";
    private static final String BASELINE_PATH = "/ima/IMATestBaseline.csv";

    /**
     * Tests creation of IMA baseline from .csv file and initialization of an
     * IMA measurement baseline.
     *
     * @throws Exception if error encountered
     */
    @Test
    public final void generateBaselineFromCSVFile() throws Exception {
        try (InputStream in = getClass().getResourceAsStream(BASELINE_PATH)) {
            SimpleImaBaseline baseline = new SimpleImaBaselineGenerator()
                    .generateBaselineFromCSVFile("TestIMABaseline", in);
            assertNotNull(baseline);
        }
    }

    /**
     * Tests creation of IMA baseline from .csv file and initialization of an
     * IMA measurement baseline.
     *
     * @throws Exception if error encountered
     */
    @Test
    public final void updateBaselineFromCSVFile() throws Exception {
        SimpleImaBaseline baseline = new SimpleImaBaseline("TestIMABaseline");
        try (InputStream in = getClass().getResourceAsStream(BASELINE_PATH)) {
            new SimpleImaBaselineGenerator().updateBaselineFromCSVFile(baseline, in);
        }
    }

    /**
     * Tests creation of IMA baseline from .csv file and initialization of an
     * IMA measurement baseline when processing file paths surrounded by double
     * quotes.
     *
     * @throws Exception if error encountered
     */
    @Test
    public final void generateBaselineQuotes() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/ima/IMATestBaselineQuotes.csv")) {
            ImaBaseline baseline = new SimpleImaBaselineGenerator().generateBaselineFromCSVFile(
                    "TestIMABaselineQuotes", in);
            assertNotNull(baseline);
        }
    }

    /**
     * Tests creation of IMA baseline from .csv file and initialization of an
     * IMA measurement baseline when processing file paths surrounded by double
     * quotes.
     *
     * @throws Exception if error encountered
     */
    @Test
    public final void updateBaselineQuotes() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/ima/IMATestBaselineQuotes.csv")) {
            SimpleImaBaseline baseline = new SimpleImaBaseline("TestIMABaselineQuotes");
            new SimpleImaBaselineGenerator().updateBaselineFromCSVFile(baseline, in);
        }
    }

    /**
     * Tests creation of IMA baseline from .csv file and initialization of an
     * IMA measurement baseline when processing data that contains escape
     * characters.
     *
     * @throws Exception if error encountered
     */
    @Test
    public final void generateBaselineEscape() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/ima/IMATestEscape.csv")) {
            ImaBaseline baseline = new SimpleImaBaselineGenerator().generateBaselineFromCSVFile(
                    "TestIMABaselineEscapeChars", in);
            assertNotNull(baseline);
        }
    }

    /**
     * Tests creation of IMA baseline from .csv file and initialization of an
     * IMA measurement baseline when processing data that contains escape
     * characters.
     *
     * @throws Exception if error encountered
     */
    @Test
    public final void updateBaselineEscape() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/ima/IMATestEscape.csv")) {
            SimpleImaBaseline baseline = new SimpleImaBaseline("TestIMABaselineEscapeChars");
            new SimpleImaBaselineGenerator().updateBaselineFromCSVFile(baseline, in);
        }
    }

    /**
     * Tests generateWhiteListBaselineFromCSVFile() throws NullPointerException with
     * null baseline name.
     *
     * @throws Exception if error encountered
     */
    @Test(expectedExceptions = NullPointerException.class)
    public final void generateBaselineFromCSVFileNullBaselineName() throws Exception {
        try (InputStream in = getClass().getResourceAsStream(BASELINE_PATH)) {
            new SimpleImaBaselineGenerator().generateBaselineFromCSVFile(null, in);
        }
    }

    /**
     * Tests updateBaselineFromCSVFile() throws NullPointerException with
     * null baseline.
     *
     * @throws Exception if error encountered
     */
    @Test(expectedExceptions = NullPointerException.class)
    public final void updateBaselineFromCSVFileNullBaseline() throws Exception {
        try (InputStream in = getClass().getResourceAsStream(BASELINE_PATH)) {
            new SimpleImaBaselineGenerator().updateBaselineFromCSVFile(null, in);
        }
    }

    /**
     * Tests generateWhiteListBaselineFromCSVFile() throws NullPointerException with
     * null input stream.
     *
     * @throws Exception if error encountered
     */
    @Test(expectedExceptions = NullPointerException.class)
    public final void generateBaselineFromCSVFileNullInputStream() throws Exception {
        new SimpleImaBaselineGenerator().generateBaselineFromCSVFile("TestIMABaseline", null);
    }

    /**
     * Tests updateBaselineFromCSVFile() throws NullPointerException with
     * null input stream.
     *
     * @throws Exception if error encountered
     */
    @Test(expectedExceptions = NullPointerException.class)
    public final void updateBaselineFromCSVFileNullInputStream() throws Exception {
        new SimpleImaBaselineGenerator().updateBaselineFromCSVFile(
                new SimpleImaBaseline("testname"), null);
    }

    /**
     * Tests generateWhiteListBaselineFromCSVFile() throws CreateIMABaselineException if
     * .csv file contains no records.
     *
     * @throws Exception if error encountered
     */
    @Test(expectedExceptions = IMABaselineGeneratorException.class)
    public final void generateBaselineFromCSVFileContainsNoRecords() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/ima/IMAEmptyFile.csv")) {
            new SimpleImaBaselineGenerator().generateBaselineFromCSVFile("TestIMABaseline", in);
        }
    }

    /**
     * Tests updateBaselineFromCSVFile() throws CreateIMABaselineException if
     * .csv file contains no records.
     *
     * @throws Exception if error encountered
     */
    @Test(expectedExceptions = IMABaselineGeneratorException.class)
    public final void updateBaselineFromCSVFileContainsNoRecords() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/ima/IMAEmptyFile.csv")) {
            new SimpleImaBaselineGenerator().updateBaselineFromCSVFile(
                    new SimpleImaBaseline("TestIMABaseline"), in);
        }
    }

    /**
     * Tests generateWhiteListBaselineFromCSVFile() throws CreateIMABaselineException if
     * .csv file contains a record with a comma in the file path.
     *
     * @throws Exception if error encountered
     */
    @Test
    public final void generateBaselineFromCSVFileContainsCommas() throws Exception {
        try (InputStream in =
                     getClass().getResourceAsStream("/ima/IMAExtraCommaTestBaseline.csv")) {
            SimpleImaBaseline baseline =
                    new SimpleImaBaselineGenerator().generateBaselineFromCSVFile(
                    "TestIMABaseline", in
                    );

            assertEquals(1, baseline.getBaselineRecords().size());

            for (IMABaselineRecord record : baseline.getBaselineRecords()) {
                assertEquals(record.getPath(),
                        "\\usr\\\"share\"\\i18n\\charmaps\\ISO_8859-1,GL.gz");
            }
        }
    }

    /**
     * Tests updateBaselineFromCSVFile() throws CreateIMABaselineException if
     * .csv file contains a record with a comma in the file path.
     *
     * @throws Exception if error encountered
     */
    @Test
    public final void updateBaselineFromCSVFileContainsCommas() throws Exception {
        try (InputStream in =
                     getClass().getResourceAsStream("/ima/IMAExtraCommaTestBaseline.csv")) {
            SimpleImaBaseline baseline = new SimpleImaBaseline("TestIMABaseline");
            new SimpleImaBaselineGenerator().updateBaselineFromCSVFile(baseline, in);

            assertEquals(1, baseline.getBaselineRecords().size());

            for (IMABaselineRecord record : baseline.getBaselineRecords()) {
                assertEquals(record.getPath(),
                        "\\usr\\\"share\"\\i18n\\charmaps\\ISO_8859-1,GL.gz");
            }
        }
    }

    /**
     * Tests generateWhiteListBaselineFromCSVFile() handles measurement entries that
     * have more than 2 fields (2 fields per record are expected).
     *
     * @throws Exception if error encountered
     */
    @Test(expectedExceptions = IMABaselineGeneratorException.class)
    public final void generateBaselineFromCSVFileContainsAdditionalFields() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/ima/IMATestAdditionalFields.csv")) {
            new SimpleImaBaselineGenerator().generateBaselineFromCSVFile("IMATestBaseline", in);
        }
    }

    /**
     * Tests updateBaselineFromCSVFile() handles measurement entries that
     * have more than 2 fields (2 fields per record are expected).
     *
     * @throws Exception if error encountered
     */
    @Test(expectedExceptions = IMABaselineGeneratorException.class)
    public final void updateBaselineFromCSVFileContainsAdditionalFields() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/ima/IMATestAdditionalFields.csv")) {
            new SimpleImaBaselineGenerator().updateBaselineFromCSVFile(
                    new SimpleImaBaseline("IMATestBaseline"), in);
        }
    }

    /**
     * Tests generateWhiteListBaselineFromCSVFile throws CreateIMABaselineException if
     * .csv file contains records that do not adhere to the record format. In
     * this case the ima record does not meet the minimum number of required
     * fields.
     *
     * @throws Exception if error encountered
     */
    @Test(expectedExceptions = IMABaselineGeneratorException.class)
    public final void generateBaselineFromCSVFileContainsInvalidRecords() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/ima/IMAInvalidRecords.csv")) {
            new SimpleImaBaselineGenerator().generateBaselineFromCSVFile("IMATestBaseline", in);
        }
    }

    /**
     * Tests updateBaselineFromCSVFile throws CreateIMABaselineException if
     * .csv file contains records that do not adhere to the record format. In
     * this case the ima record does not meet the minimum number of required
     * fields.
     *
     * @throws Exception if error encountered
     */
    @Test(expectedExceptions = IMABaselineGeneratorException.class)
    public final void updateBaselineFromCSVFileContainsInvalidRecords() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/ima/IMAInvalidRecords.csv")) {
            new SimpleImaBaselineGenerator().updateBaselineFromCSVFile(
                    new SimpleImaBaseline("IMATestBaseline"), in);
        }
    }

    /**
     * Tests creation of IMA baseline from .csv file and verifies that all
     * baseline properties were created correctly.
     *
     * @throws Exception if error encountered
     */
    @Test
    public final void generateBaselineFromCSVFileVerifyProperties() throws Exception {
        try (InputStream in = getClass().getResourceAsStream(IMATestUtil.VERIFY_FIELDS_CSV)) {
            SimpleImaBaseline baseline =
                    new SimpleImaBaselineGenerator().generateBaselineFromCSVFile(
                    "TestIMABaseline", in
                    );
            assertEquals(baseline.getName(), "TestIMABaseline");
            Set<IMABaselineRecord> expectedRecords = IMATestUtil.getExpectedRecords();
            Set<IMABaselineRecord> baselineRecords = baseline.getBaselineRecords();
            assertEquals(baselineRecords, expectedRecords);
        }
    }

    /**
     * Tests creation of IMA baseline from .csv file and verifies that all
     * baseline properties were created correctly.
     *
     * @throws Exception if error encountered
     */
    @Test
    public final void updateBaselineFromCSVFileVerifyProperties() throws Exception {
        try (InputStream in = getClass().getResourceAsStream(IMATestUtil.VERIFY_FIELDS_CSV)) {
            SimpleImaBaseline baseline = new SimpleImaBaseline("TestIMABaseline");
            new SimpleImaBaselineGenerator().updateBaselineFromCSVFile(baseline, in);
            assertEquals(baseline.getName(), "TestIMABaseline");
            Set<IMABaselineRecord> expectedRecords = IMATestUtil.getExpectedRecords();
            Set<IMABaselineRecord> baselineRecords = baseline.getBaselineRecords();
            assertEquals(baselineRecords, expectedRecords);
        }
    }

    /**
     * Tests generation of IMAbaseline instance from an IMA report. The test
     * relies on sample IMA report stored in XML file.
     *
     * @throws Exception
     *             if error encountered
     */
    @Test
    public final void generateBaselineFromIMAReport() throws Exception {
        final String[] hashes = {"GZWGIEab7A6j99LNDyOkhFjjx3M=",
                "MyfXNfq/3Td35On6QLAimH7EKrI=", "CfTKAHwf04wUN9ILMcNwPOts/GU=",
                "ZhRN8cbGioNEcZ5o0wcqYNA25jA=", "mDD5gha0OCtZlnlGhPkOA+nVDeU=",
                "CDeQlrufdO4mjEZTFBU/CMXnd04=", "+xpZzZ/2f9TAu1KUMe6T2RDExUQ=",
                "st0DA3u1ki0nrDQYAUPTZek3iMY=", "OEvB5B7ffdmYBiNpx7DPRAiNXOc=",
                "YPNEp9caN2O6LQN70ZAU/l7K6ac=" };
        final String[] paths = {"boot_aggregate", "/init", "/init",
                "ld-2.12.so", "ld.so.cache", "libc-2.12.so", "dracut-lib.sh",
                "/bin/mknod", "libselinux.so.1", "libdl-2.12.so" };
        final IMABaselineRecord[] expected = new IMABaselineRecord[hashes.length];
        for (int i = 0; i < hashes.length; ++i) {
            final Digest digest = new Digest(DigestAlgorithm.SHA1,
                    Base64.decodeBase64(hashes[i]));
            expected[i] = new IMABaselineRecord(paths[i], digest);
        }
        final IMAReport report = getDefaultIMAReport();
        final SimpleImaBaselineGenerator generator = new SimpleImaBaselineGenerator();
        final String name = "testIMABaselineFromIMAReport";
        final SimpleImaBaseline baseline = generator.generateBaselineFromIMAReport(name, report);
        assertNotNull(baseline);
        Set<IMABaselineRecord> records = baseline.getBaselineRecords();
        assertEquals(expected.length, records.size());
        for (IMABaselineRecord record : expected) {
            assertTrue(records.contains(record));
        }
    }

    /**
     * Tests generateBaselineFromIMAReport() throws NullPointerException with
     * null IntegrityReport parameter.
     *
     * @throws Exception if error encountered
     */
    @Test(expectedExceptions = NullPointerException.class)
    public final void generateBaselineFromIMAReportNullIMAReport() throws Exception {
        SimpleImaBaselineGenerator simpleImaBaselineGenerator = new SimpleImaBaselineGenerator();
        simpleImaBaselineGenerator.generateBaselineFromIMAReport(
                "testIMABaselineFromIntegrityReport", null);
    }

    /**
     * Tests generateBaselineFromIMAReport() throws NullPointerException
     * with null baselineName parameter.
     *
     * @throws Exception if error encountered
     */
    @Test(expectedExceptions = NullPointerException.class)
    public final void generateBaselineFromIMAReportNullBaselineName() throws Exception {
        final IMAReport report = getDefaultIMAReport();
        final SimpleImaBaselineGenerator generator = new SimpleImaBaselineGenerator();
        generator.generateBaselineFromIMAReport(null, report);
    }

    /**
     * Tests generation of IMAbaseline instance from an integrity report. The
     * test relies on sample IMA report stored in XML file.
     *
     * @throws Exception if error encountered
     */
    public final void generateBaselineFromIntegrityReport() throws Exception {
        final String[] hashes = {"n9/LWPw76vtjr48Z9kWB9fws32c=",
                "2jmj7l5rSw0yVb/vlWAYkK/YBwk=", "2jmj7l5rSw0yVb/vlWAYkK/YBwk=",
                "2jmj7l5rSw0yVb/vlWAYkK/YBwk=", "2jmj7l5rSw0yVb/vlWAYkK/YBwk=",
                "AAAAAAAAAAAAAAAAAAAAAAAAAAA=", "2jmj7l5rSw0yVb/vlWAYkK/YBwk=",
                "2jmj7l5rSw0yVb/vlWAYkK/YBwk=", "2jmj7l5rSw0yVb/vlWAYkK/YBwk=",
                "2jmj7l5rSw0yVb/vlWAYkK/YBwk=" };
        final String[] paths = {"boot_aggregate",
                "/sys/fs/cgroup/systemd/release_agent",
                "/sys/fs/cgroup/systemd/notify_on_release",
                "/sys/fs/cgroup/systemd/system.slice/kmod-static-nodes.service/"
                + "control/cgroup.procs",
                "/sys/fs/cgroup/systemd/system.slice/kmod-static-nodes.service/"
                + "cgroup.procs",
                "/sys/fs/cgroup/systemd/system.slice/kmod-static-nodes.service/"
                + "cgroup.procs",
                "/sys/fs/cgroup/systemd/system.slice/systemd-sysctl.service/cgr"
                + "oup.procs1",
                "/sys/fs/cgroup/systemd/system.slice/dracut-cmdline.service/cgr"
                + "oup.procs",
                "/sys/fs/cgroup/systemd/system.slice/cgroup.procs",
                "/sys/fs/cgroup/systemd/system.slice/-.mount/cgroup.procs" };
        final IMABaselineRecord[] expected = new IMABaselineRecord[hashes.length];
        for (int i = 0; i < hashes.length; ++i) {
            final Digest digest = new Digest(DigestAlgorithm.SHA1,
                    Base64.decodeBase64(hashes[i]));
            expected[i] = new IMABaselineRecord(paths[i], digest);
        }

        IntegrityReport report = getIntegrityReport(DEFAULT_INTEGRITY_REPORT);
        final SimpleImaBaselineGenerator generator = new SimpleImaBaselineGenerator();
        final String name = "testIMABaselineFromIntegrityReport";
        final SimpleImaBaseline baseline =
                generator.generateBaselineFromIntegrityReport(name, report);
        assertNotNull(baseline);
        Set<IMABaselineRecord> records = baseline.getBaselineRecords();
        assertEquals(expected.length, records.size());
        for (IMABaselineRecord record : expected) {
            assertTrue(records.contains(record));
        }
    }

    /**
     * Tests generateBaselineFromIntegrityReport() throws NullPointerException
     * with null IntegrityReport parameter.
     *
     * @throws Exception if error encountered
     */
    @Test(expectedExceptions = NullPointerException.class)
    public final void generateBaselineFromIntegrityReportNullIntegrityReport() throws Exception {
        SimpleImaBaselineGenerator simpleImaBaselineGenerator = new SimpleImaBaselineGenerator();
        simpleImaBaselineGenerator.generateBaselineFromIntegrityReport("testname", null);
    }

    /**
     * Tests generateBaselineFromIntegrityReport() throws NullPointerException
     * with null baselineName parameter.
     *
     * @throws Exception if error encountered
     */
    @Test(expectedExceptions = NullPointerException.class)
    public final void generateBaselineFromIntegrityReportNullBaselineName() throws Exception {
        final IntegrityReport report = getIntegrityReport(DEFAULT_INTEGRITY_REPORT);
        final SimpleImaBaselineGenerator generator = new SimpleImaBaselineGenerator();
        generator.generateBaselineFromIntegrityReport(null, report);
    }

    private IMAReport getDefaultIMAReport() throws Exception {
        try (InputStream istream = getClass().getResourceAsStream(DEFAULT_REPORT)) {
            JAXBContext requestContext = JAXBContext.newInstance(IMAReport.class);
            Unmarshaller reportUnmarshaller = requestContext.createUnmarshaller();
            return (IMAReport) reportUnmarshaller.unmarshal(istream);
        }
    }

    private IntegrityReport getIntegrityReport(final String path) throws Exception {
        try (InputStream istream = getClass().getResourceAsStream(path)) {
            JAXBContext context =
                    JAXBContext.newInstance(IntegrityReport.class,
                            DeviceInfoReport.class,  TPMReport.class,
                            IMAReport.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            return (IntegrityReport) unmarshaller.unmarshal(istream);
        }
    }
}

