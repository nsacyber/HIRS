package hirs.tpm;

import hirs.data.persist.baseline.TPMBaseline;
import hirs.data.persist.info.TPMInfo;
import hirs.data.persist.baseline.TpmWhiteListBaseline;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.codec.binary.Base64;
import org.testng.Assert;
import org.testng.annotations.Test;

import hirs.data.persist.DeviceInfoReport;
import hirs.data.persist.Digest;
import hirs.data.persist.enums.DigestAlgorithm;
import hirs.data.persist.info.FirmwareInfo;
import hirs.data.persist.info.HardwareInfo;
import hirs.data.persist.IMAReport;
import hirs.data.persist.IntegrityReport;
import hirs.data.persist.info.OSInfo;
import hirs.data.persist.TPMReport;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * <code>CreateTPMBaselineTest</code> is a unit test class for the
 * <code>CreateTPMBaseline</code> class.
 */
public class TPMBaselineGeneratorTest {

    private static final String DEFAULT_BASELINE = "/tpm/TPMTestBaseline.csv";
    private static final String DEFAULT_INTEGRITY_REPORT = "/reports/integrity/"
            + "integrity_report_with_AIK_certificate.xml";
    private static final String NO_PCRS_INTEGRITY_REPORT = "/reports/"
            + "integrity/integrity_report_with_AIK_certificate_noPCRs.xml";
    private static final Logger LOGGER
            = LogManager.getLogger(TPMBaselineGeneratorTest.class);

    /**
     * Tests creation of baseline from TPM .csv file and initialization of a TPM
     * measurement baseline.
     *
     * @throws IOException
     *             if error encountered reading data from input stream
     * @throws ParseException
     *             if error encountered parsing data
     * @throws TPMBaselineGeneratorException
     *             if error encountered when retrieving entries from
     *             input stream
     */
    @Test
    public final void generateBaselineFromCSVFile() throws IOException,
            ParseException, TPMBaselineGeneratorException {
        InputStream in;
        TpmWhiteListBaseline baseline;
        TPMBaselineGenerator baselineCreator = new TPMBaselineGenerator();
        in = this.getClass().getResourceAsStream(DEFAULT_BASELINE);
        try {
            baseline = baselineCreator.generateWhiteListBaselineFromCSVFile(
                    "TestTPMBaseline", in);
            Assert.assertNotNull(baseline);
            Assert.assertEquals(baseline.getFirmwareInfo(), new FirmwareInfo());
            Assert.assertEquals(baseline.getHardwareInfo(), new HardwareInfo());
            Assert.assertEquals(baseline.getOSInfo(), new OSInfo());
            Assert.assertEquals(baseline.getTPMInfo(), new TPMInfo());
        } finally {
            in.close();
        }
    }

    /**
     * Tests generateWhiteListBaselineFromCSVFile() throws NullPointerException with null
     * baseline name.
     *
     * @throws IOException
     *             if error encountered reading data from input stream
     * @throws ParseException
     *             if error encountered parsing data
     * @throws TPMBaselineGeneratorException
     *             if error encountered when retrieving measurement entries from
     *             input stream
     */
    @Test
    public final void generateBaselineFromCSVFileNullBaselineName()
            throws IOException, ParseException, TPMBaselineGeneratorException {
        Exception expectedEx = null;
        InputStream in;
        in = this.getClass().getResourceAsStream(DEFAULT_BASELINE);
        try {
            new TPMBaselineGenerator().generateWhiteListBaselineFromCSVFile(null, in);
        } catch (NullPointerException e) {
            expectedEx = e;
        } finally {
            in.close();
            Assert.assertNotNull(expectedEx);
        }
    }

    /**
     * Tests generateWhiteListBaselineFromCSVFile() throws NullPointerException with null
     * input stream.
     *
     * @throws IOException
     *             if error encountered reading data from input stream
     * @throws ParseException
     *             if error encountered parsing data
     * @throws TPMBaselineGeneratorException
     *             if error encountered when retrieving measurement entries from
     *             input stream
     */
    @Test(expectedExceptions = NullPointerException.class)
    public final void generateBaselineFromCSVFileNullInputStream()
            throws IOException, ParseException, TPMBaselineGeneratorException {
        new TPMBaselineGenerator().generateWhiteListBaselineFromCSVFile(
                "TestTPMBaseline", null);
    }

    /**
     * Tests generateWhiteListBaselineFromCSVFile() throws CreateTPMBaselineException if
     * .csv file contains no records.
     *
     * @throws IOException
     *             if error encountered reading data from input stream
     * @throws ParseException
     *             if error encountered parsing data
     */
    @Test
    public final void generateBaselineFromCSVFileContainsNoRecords()
            throws IOException, ParseException {
        Exception expectedEx = null;
        String emptyBaselinePath = "/tpm/TPMEmptyFile.csv";
        InputStream in;
        in = this.getClass().getResourceAsStream(emptyBaselinePath);
        try {
            new TPMBaselineGenerator().generateWhiteListBaselineFromCSVFile(
                    "TestTPMBaseline", in);
        } catch (TPMBaselineGeneratorException e) {
            expectedEx = e;
        } finally {
            in.close();
            Assert.assertNotNull(expectedEx);
        }

    }

    /**
     * Tests generateWhiteListBaselineFromCSVFile() handles measurement entries that have
     * more than 2 fields (2 fields per record are expected).
     *
     * @throws IOException
     *             if error encountered reading data from input stream
     * @throws ParseException
     *             if error encountered parsing data
     * @throws TPMBaselineGeneratorException
     *             if error encountered when retrieving measurement entries from
     *             input stream
     */
    @Test(expectedExceptions = TPMBaselineGeneratorException.class)
    public final void generateBaselineFromCSVFileContainsAdditionalFields()
            throws IOException, ParseException, TPMBaselineGeneratorException {
        String testBaselinePath = "/tpm/TPMTestAdditionalFields.csv";
        InputStream in;
        TPMBaselineGenerator baselineCreator = new TPMBaselineGenerator();
        in = this.getClass().getResourceAsStream(testBaselinePath);
        try {
            baselineCreator.generateWhiteListBaselineFromCSVFile("TestTPMBaseline", in);
            Assert.fail("baseline generator did not throw exception");
        } finally {
            in.close();
        }
    }

    /**
     * Tests generateWhiteListBaselineFromCSVFile throws CreateTPMBaselineException if
     * .csv file contains records that do not adhere to the record format. In
     * this case the pcr record does not meet the minimum number of required
     * fields.
     *
     * @throws IOException
     *             if error encountered reading data from input stream
     * @throws ParseException
     *             if error encountered parsing data
     */
    @Test
    public final void generateBaselineFromCSVFileContainsInvalidRecords()
            throws IOException, ParseException {
        Exception expectedEx = null;
        String invalidBaselinePath = "/tpm/TPMInvalidRecords.csv";
        InputStream in;
        in = this.getClass().getResourceAsStream(invalidBaselinePath);
        try {
            new TPMBaselineGenerator().generateWhiteListBaselineFromCSVFile(
                    "TestTPMBaseline", in);
        } catch (TPMBaselineGeneratorException e) {
            expectedEx = e;
        } finally {
            in.close();
            Assert.assertNotNull(expectedEx);
        }
    }

    /**
     * Tests generateWhiteListBaselineFromCSVFile to make sure each device info
     * field can be set.
     *
     * @throws IOException
     *             if error encountered reading data from input stream
     * @throws ParseException
     *             if error encountered parsing data
     * @throws TPMBaselineGeneratorException
     * if error encountered when retrieving entries from
     *             input stream
     */
    @Test
    public final void generateBaselineFromCSVFileContainsDeviceInfoRecords()
            throws IOException, ParseException, TPMBaselineGeneratorException {
        final String baselinePath = "/tpm/TPMTestBaselineWithDeviceInfo.csv";
        final String biosVersion = "abc";
        final String biosVendor = "HirsBIOS";
        final String biosReleaseDate = "04/25/2014";
        final String manufacturer = "U.S.A";
        final String productName = "The best product";
        final String version = "0.6.9";
        final String serialNumber = "8_8";
        final String chassisSerialNumber = "9_9";
        final String baseboardSerialNumber = "ABC123";
        final String osName = "Linux";
        final String osVersion = "3.10.0-123.el7.x86_64";
        final String osArch = DeviceInfoReport.NOT_SPECIFIED;
        final String distribution = "CentOS";
        final String distributionRelease = "7.0.1406";
        final String tpmMake = "Infineon";
        final short tpmVersionMajor = 1;
        final short tpmVersionMinor = 2;
        final short tpmVersionRevMajor = 3;
        final short tpmVersionRevMinor = 4;
        final FirmwareInfo expectedFirmwareInfo =
            new FirmwareInfo(biosVendor, biosVersion, biosReleaseDate);
        final HardwareInfo expectedHardwareInfo =
            new HardwareInfo(manufacturer, productName, version, serialNumber, chassisSerialNumber,
                    baseboardSerialNumber);
        final OSInfo expectedOSInfo =
            new OSInfo(osName, osVersion, osArch, distribution, distributionRelease);
        final TPMInfo expectedTPMInfo = new TPMInfo(tpmMake, tpmVersionMajor,
            tpmVersionMinor, tpmVersionRevMajor, tpmVersionRevMinor);

        InputStream in;
        TpmWhiteListBaseline baseline;
        TPMBaselineGenerator baselineCreator = new TPMBaselineGenerator();
        in = this.getClass().getResourceAsStream(baselinePath);
        try {
            baseline = baselineCreator.generateWhiteListBaselineFromCSVFile("TestTPMBaseline", in);
            Assert.assertNotNull(baseline);
            Assert.assertEquals(baseline.getFirmwareInfo(), expectedFirmwareInfo);
            Assert.assertEquals(baseline.getHardwareInfo(), expectedHardwareInfo);
            Assert.assertEquals(baseline.getOSInfo(), expectedOSInfo);
            Assert.assertEquals(baseline.getTPMInfo(), expectedTPMInfo);
        } finally {
            in.close();
        }
    }

    /**
     * Tests generateWhiteListBaselineFromCSVFile to make sure an invalid row of
     * data within the CSV file results in an error.
     *
     * @throws IOException
     *             if error encountered reading data from input stream
     * @throws ParseException
     *             if error encountered parsing data
     * @throws TPMBaselineGeneratorException
     * if error encountered when retrieving entries from
     *             input stream
     */
    @Test
    public final void generateBaselineFromCSVFileContainsInvalidDeviceInfo()
            throws IOException, ParseException, TPMBaselineGeneratorException {
        final String baselinePath = "/tpm/TPMTestBaselineWithInvalidDeviceInfo.csv";
        final String expectedErrorMessage = "parse a number in CSV file"
                + " record: \"osEversion,3.10.0-123.el7.x86_64\"";

        InputStream in;
        TPMBaseline baseline;
        TPMBaselineGenerator baselineCreator = new TPMBaselineGenerator();
        in = this.getClass().getResourceAsStream(baselinePath);
        try {
            baseline = baselineCreator.generateWhiteListBaselineFromCSVFile("TestTPMBaseline", in);
            Assert.assertNotNull(baseline);
        } catch (TPMBaselineGeneratorException e) {
            Assert.assertTrue(e.getMessage().contains(expectedErrorMessage));
        } finally {
            in.close();
        }
    }

    /**
     * Tests generating a <code>TPMBaseline</code> from a report. This generates
     * a report and then verifies the expected TPM PCR values.
     *
     * @throws Exception
     *             if any unexpected errors occur
     */
    @Test
    public final void generateBaselineFromIntegrityReport() throws Exception {
        final IntegrityReport report = getIntegrityReport(
                DEFAULT_INTEGRITY_REPORT);
        final TPMBaselineGenerator generator = new TPMBaselineGenerator();
        final String name = "testTPMBaselineFromIntegrityReport";
        final Digest[] expectedHashes = {
                getDigest("dqv2d3gfy5g9p4Cgj+RpIOuxoFg="),
                getDigest("Oj94DxGktJlp/KqAzW45V8M7InU="),
                getDigest("Oj94DxGktJlp/KqAzW45V8M7InU="),
                getDigest("Oj94DxGktJlp/KqAzW45V8M7InU="),
                getDigest("UonomADxmAUZKiD7vHEtGDYdPUU="),
                getDigest("fjmz2i+746NnmOrV6Hen6mDQDbI="),
                getDigest("Oj94DxGktJlp/KqAzW45V8M7InU="),
                getDigest("Oj94DxGktJlp/KqAzW45V8M7InU="),
                getDigest("AAAAAAAAAAAAAAAAAAAAAAAAAAA="),
                getDigest("AAAAAAAAAAAAAAAAAAAAAAAAAAA="),
                getDigest("7KaI/e+tcAt7aBqZb672EDFr2Xk="),
                getDigest("AAAAAAAAAAAAAAAAAAAAAAAAAAA="),
                getDigest("AAAAAAAAAAAAAAAAAAAAAAAAAAA="),
                getDigest("AAAAAAAAAAAAAAAAAAAAAAAAAAA="),
                getDigest("AAAAAAAAAAAAAAAAAAAAAAAAAAA="),
                getDigest("AAAAAAAAAAAAAAAAAAAAAAAAAAA="),
                getDigest("AAAAAAAAAAAAAAAAAAAAAAAAAAA="),
                getDigest("//////////////////////////8="),
                getDigest("//////////////////////////8="),
                getDigest("//////////////////////////8="),
                getDigest("//////////////////////////8="),
                getDigest("//////////////////////////8="),
                getDigest("//////////////////////////8="),
                getDigest("AAAAAAAAAAAAAAAAAAAAAAAAAAA=")
        };
        final TPMBaseline baseline = generator.
                generateBaselineFromIntegrityReport(name, report);
        Assert.assertNotNull(baseline);
        Assert.assertEquals(baseline.getPcrRecords().size(),
                expectedHashes.length);
        for (int i = 0; i < expectedHashes.length; i++) {
            final Set<Digest> baselineHashes = baseline.getPCRHashes(i);
            Assert.assertEquals(baselineHashes.size(), 1);
            Assert.assertTrue(baselineHashes.contains(expectedHashes[i]));
        }
    }

    /**
     * Tests generateBaselineFromIntegrityReport() throws NullPointerException
     * with null baseline name.
     *
     * @throws Exception
     *             if any unexpected errors occur
     */
    @Test(expectedExceptions = NullPointerException.class)
    public final void generateBaselineFromIntegrityReportNullBaselineName()
            throws Exception  {
        final IntegrityReport report = getIntegrityReport(
                DEFAULT_INTEGRITY_REPORT);
        final TPMBaselineGenerator generator = new TPMBaselineGenerator();
        generator.generateBaselineFromIntegrityReport(null, report);
    }

    /**
     * Tests generateBaselineFromInegrityReport() throws NullPointerException
     * with null Integrity Report.
     *
     * @throws TPMBaselineGeneratorException
     *              if number of PCRs measurements in report is zero, or more
     *              than maximum, or no PCR measurements in the report.
     */
    @Test(expectedExceptions = NullPointerException.class)
    public final void generateBaselineFromInetgrityReportNullIntegrityReport()
            throws TPMBaselineGeneratorException {
        final TPMBaselineGenerator generator = new TPMBaselineGenerator();
        final String name = "TestReportNullReport";
        final TPMBaseline baseline = generator.
                generateBaselineFromIntegrityReport(name, null);
        Assert.assertNotNull(baseline);
        Assert.assertEquals(baseline.getPcrRecords().size(), 0);
    }

    /**
     * Test generateBaselineFromIntegrityReport() using a report with no TPM
     * Measurement records.
     * @throws TPMBaselineGeneratorException
     *              if number of PCRs measurements in report is zero, or more
     *              than maximum, or no PCR measurements in the report.
     * @throws Exception
     *              if any unexpected error occur
     */
    @Test
    public final void generateBaselineFromIntegrityReportNoPCRs()
            throws TPMBaselineGeneratorException, Exception {
        final IntegrityReport report = getIntegrityReport(
                NO_PCRS_INTEGRITY_REPORT);
        final TPMBaselineGenerator generator = new TPMBaselineGenerator();
        final String name = "testTPMBaselineFromIntegrityReportWithNoPCRs";
        generator.generateBaselineFromIntegrityReport(name, report);
    }

    /**
     * Test generateWhiteListBaselineOnKernelUpdate to ensure it creates a baseline with the
     * expected name, the appropriate device info, and the selected kernel PCR values.
     * @throws Exception If there is a problem.
     */
    @Test
    public final void generateBaselineOnKernelUpdate() throws Exception {
        final String name = "Monday";
        final IntegrityReport report = getIntegrityReport(DEFAULT_INTEGRITY_REPORT);
        final int kernelPcrMask = 0x060000;
        final TPMBaselineGenerator generator = new TPMBaselineGenerator();
        final TpmWhiteListBaseline baseline =
            generator.generateWhiteListBaselineOnKernelUpdate(name, report, kernelPcrMask);
        final DeviceInfoReport diReport = report.extractReport(DeviceInfoReport.class);
        final String osName = diReport.getOSInfo().getOSName();
        final String osVersion = diReport.getOSInfo().getOSVersion();
        final TPMReport tpmReport = report.extractReport(TPMReport.class);
        final int[] pcrIds = new int[] {17, 18};

        Assert.assertNotNull(baseline);
        Assert.assertEquals(baseline.getName(), name);
        Assert.assertEquals(baseline.getOSInfo().getOSName(), osName);
        Assert.assertEquals(baseline.getOSInfo().getOSVersion(), osVersion);
        Assert.assertEquals(baseline.getPcrRecords().size(), pcrIds.length);
        for (int i = 0; i < pcrIds.length; i++) {
            Assert.assertNotNull(tpmReport.getTPMMeasurementRecord(pcrIds[i]));
            Assert.assertTrue(baseline.getPCRHashes(pcrIds[i])
                .contains(tpmReport.getTPMMeasurementRecord(pcrIds[i]).getHash()));
        }
    }

    /**
     * Test generateWhiteListBaselineOnKernelUpdate to ensure it creates a baseline with the
     * expected name, the appropriate device info, and no measurement records.
     * @throws Exception If there is a problem.
     */
    @Test
    public final void generateBaselineOnKernelUpdateKernelPcrIs0() throws Exception {
        final String name = "Monday";
        final IntegrityReport report = getIntegrityReport(DEFAULT_INTEGRITY_REPORT);
        final int kernelPcrMask = 0;
        final TPMBaselineGenerator generator = new TPMBaselineGenerator();
        final TpmWhiteListBaseline baseline =
            generator.generateWhiteListBaselineOnKernelUpdate(name, report, kernelPcrMask);
        final DeviceInfoReport diReport = report.extractReport(DeviceInfoReport.class);
        final String osName = diReport.getOSInfo().getOSName();
        final String osVersion = diReport.getOSInfo().getOSVersion();
        final int[] pcrIds = new int[] {};

        Assert.assertNotNull(baseline);
        Assert.assertEquals(baseline.getName(), name);
        Assert.assertEquals(baseline.getOSInfo().getOSName(), osName);
        Assert.assertEquals(baseline.getOSInfo().getOSVersion(), osVersion);
        Assert.assertEquals(baseline.getPcrRecords().size(), pcrIds.length);
    }

    /**
     * Test generateWhiteListBaselineOnKernelUpdate to ensure it fails as expected when
     * the name or report parameters are null.
     * @throws Exception If there is an unexpected problem.
     */
    @Test
    public final void generateBaselineOnKernelUpdateNullParams() throws Exception {
        final String name = "Monday";
        final IntegrityReport report = getIntegrityReport(DEFAULT_INTEGRITY_REPORT);
        final int kernelPcrMask = 0;
        final TPMBaselineGenerator generator = new TPMBaselineGenerator();

        // Test when the name parameter is null.
        try {
            generator.generateWhiteListBaselineOnKernelUpdate(null, report, kernelPcrMask);
        } catch (NullPointerException e) {
            Assert.assertTrue(e.getMessage().contains("baselineName"));
        }

        // Test when the report parameter is null.
        try {
            generator.generateWhiteListBaselineOnKernelUpdate(name, null, kernelPcrMask);
        } catch (NullPointerException e) {
            Assert.assertTrue(e.getMessage().contains("report"));
        }
    }

    /**
     * Test generateWhiteListBaselineOnKernelUpdate to ensure it copies the PCR values
     * and leaves the default device info when there is no device info report.
     * @throws Exception If there is a problem.
     */
    @Test
    public final void generateBaselineOnKernelUpdateNoReport() throws Exception {
        final String name = "Monday";
        final IntegrityReport report = getIntegrityReport(DEFAULT_INTEGRITY_REPORT);
        final int kernelPcrMask = 0xFFFFFF;
        final TPMBaselineGenerator generator = new TPMBaselineGenerator();
        final int numRecords = 24;
        // remove the device info report
        report.removeReport(report.extractReport(DeviceInfoReport.class));
        final TpmWhiteListBaseline baseline =
                generator.generateWhiteListBaselineOnKernelUpdate(name, report, kernelPcrMask);

        Assert.assertNotNull(baseline);
        Assert.assertEquals(baseline.getName(), name);
        Assert.assertEquals(baseline.getOSInfo().getOSName(), DeviceInfoReport.NOT_SPECIFIED);
        Assert.assertEquals(baseline.getOSInfo().getOSVersion(), DeviceInfoReport.NOT_SPECIFIED);
        Assert.assertEquals(baseline.getPcrRecords().size(), numRecords);
    }

    /**
     * Test generateNameForKernelUpdateBaseline returns an expected name for a
     * given integrity report.
     * @throws Exception if a problem occurs.
     */
    @Test
    public final void generateNameForKernelUpdateBaseline() throws Exception {
        final IntegrityReport report = getIntegrityReport(DEFAULT_INTEGRITY_REPORT);
        final TPMBaselineGenerator generator = new TPMBaselineGenerator();
        final String expected = "Kernel Update Linux 3.10.0-693.11.1.el7.x86_64";
        Assert.assertEquals(generator.generateNameForKernelUpdateBaseline(report), expected);
    }

    /**
     * Test for generateNameForKernelUpdateBaseline to fail appropriately when
     * given an integrity report with no device info report.
     * @throws Exception if a problem occurs.
     */
    @Test
    public final void generateNameForKernelUpdateBaselineNoDeviceInfoReport() throws Exception {
        final IntegrityReport report = getIntegrityReport(DEFAULT_INTEGRITY_REPORT);
        // remove the device info report
        report.removeReport(report.extractReport(DeviceInfoReport.class));
        final TPMBaselineGenerator generator = new TPMBaselineGenerator();
        final String expected = "not contain a device info report";

        try {
            generator.generateNameForKernelUpdateBaseline(report);
        } catch (TPMBaselineGeneratorException e) {
            Assert.assertTrue(e.getMessage().contains(expected));
        }
    }

    /**
     * Test generateNameForKernelUpdateBaseline returns an expected name for an
     * integrity report with no relevant OS info.
     * @throws Exception if a problem occurs.
     */
    @Test
    public final void generateNameForKernelUpdateBaselineNoRelevantDeviceInfo()  throws Exception {
        final IntegrityReport report = getIntegrityReport(DEFAULT_INTEGRITY_REPORT);
        // remove the existing deviceInfoReport
        DeviceInfoReport diReport = report.extractReport(DeviceInfoReport.class);
        report.removeReport(diReport);
        // update the os info
        diReport = new DeviceInfoReport(diReport.getNetworkInfo(), new OSInfo(),
            diReport.getFirmwareInfo(), diReport.getHardwareInfo(), diReport.getTPMInfo());
        // add the updated device info report
        report.addReport(diReport);
        final TPMBaselineGenerator generator = new TPMBaselineGenerator();
        final String expected = "Kernel Update " + DeviceInfoReport.NOT_SPECIFIED
            + " " + DeviceInfoReport.NOT_SPECIFIED;
        Assert.assertEquals(generator.generateNameForKernelUpdateBaseline(report), expected);
    }

    private IntegrityReport getIntegrityReport(final String path)
            throws Exception {
        InputStream istream = null;
        IntegrityReport report = null;
        try {
            istream = this.getClass().getResourceAsStream(path);
            JAXBContext context =
                    JAXBContext.newInstance(IntegrityReport.class,
                            DeviceInfoReport.class,  TPMReport.class,
                            IMAReport.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            report = (IntegrityReport) unmarshaller.unmarshal(istream);
            return report;
        } catch (Exception e) {
            Assert.fail("error occurred while unmarshalling report", e);
        } finally {
            if (istream != null) {
                try {
                    istream.close();
                } catch (Exception e) {
                    LOGGER.error("error closing istream", e);
                }
            }
        }

        return report;
    }
    private Digest getDigest(final String hash) {
        final byte[] bytes = Base64.decodeBase64(hash);
        return new Digest(DigestAlgorithm.SHA1, bytes);
    }
}
