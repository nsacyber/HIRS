package hirs.ima;

import hirs.data.persist.DeviceInfoReport;
import hirs.data.persist.Digest;
import hirs.data.persist.enums.DigestAlgorithm;
import hirs.data.persist.FirmwareInfo;
import hirs.data.persist.HardwareInfo;
import hirs.data.persist.baseline.IMABaselineRecord;
import hirs.data.persist.baseline.ImaBlacklistBaseline;
import hirs.data.persist.ImaBlacklistRecord;
import hirs.data.persist.OSInfo;
import hirs.data.persist.baseline.SimpleImaBaseline;
import hirs.data.persist.TPMInfo;
import hirs.data.persist.TPMMeasurementRecord;
import hirs.data.persist.baseline.TpmWhiteListBaseline;
import hirs.tpm.TPMBaselineGenerator;
import hirs.tpm.TPMBaselineGeneratorException;

import org.apache.commons.codec.Charsets;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import hirs.data.persist.ImaBlacklistRecordTest;

import javax.xml.bind.DatatypeConverter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.Set;

/**
 *
 * Tests for {@see CSVGenerator}.
 */
public final class CSVGeneratorTest {
    private SimpleImaBaseline imaBaseline;
    private IMABaselineRecord imaRecord1, imaRecord2, imaRecord3;
    private static final String
            HASH1 = "abcdef12abcdef12abcdef12abcdef12abcdef12",
            HASH2 = "1234567812345678123456781234567812345678",
            HASH3 = "1234abcd1234abcd1234abcd1234abcd1234abcd";

    /**
     * Builds objects for test.
     * @throws DecoderException when the Digest fails.
     */
    @BeforeTest
    void setUp() throws DecoderException {
        imaRecord1 = new IMABaselineRecord("/path/1",
                new Digest(DigestAlgorithm.SHA1, DatatypeConverter.parseHexBinary(HASH1)));
        imaRecord2 = new IMABaselineRecord("\\\"my path\"\\2",
                new Digest(DigestAlgorithm.SHA1, DatatypeConverter.parseHexBinary(HASH2)));
        imaRecord3 = new IMABaselineRecord("/path/3,4",
                new Digest(DigestAlgorithm.SHA1, DatatypeConverter.parseHexBinary(HASH3)));

        imaBaseline = new SimpleImaBaseline("mybaseline");
        imaBaseline.addToBaseline(imaRecord1);
        imaBaseline.addToBaseline(imaRecord2);
        imaBaseline.addToBaseline(imaRecord3);
    }

    /**
     * Cleans up after test.
     */
    @AfterTest
    void tearDown() {
        imaBaseline = null;
        imaRecord1 = null;
        imaRecord2 = null;
        imaRecord3 = null;
    }

    private static final String CSV =
              "\"/path/1\",abcdef12abcdef12abcdef12abcdef12abcdef12\n"
            + "\"\\\"\"my path\"\"\\2\",1234567812345678123456781234567812345678\n"
            + "\"/path/3,4\",1234abcd1234abcd1234abcd1234abcd1234abcd\n";

    /**
     * Test encodeImaBaseline.
     */
    @Test
    public void testEncodeImaBaseline() {
        System.out.println(CSVGenerator.imaRecordsToCsv(imaBaseline));
        Assert.assertEquals(CSV, CSVGenerator.imaRecordsToCsv(imaBaseline));
    }

    /**
     * Tests that an IMA blacklist baseline (whose entries have descriptions), when serialized to
     * a CSV, matches the expected output.
     *
     * @throws IOException if there is a problem serializing the baseline
     */
    @Test
    public void testImaBlacklistBaselineWithDescriptionsToCsv() throws IOException {
        Assert.assertEquals(
                CSVGenerator.blacklistToCsv(getTestImaBlacklistBaselineWithDescriptions()),
                IOUtils.toString(getClass().getResource("/ima/IMABlacklistBaseline.csv"))
        );
    }

    /**
     * Tests that an IMA blacklist baseline, when serialized to a CSV, matches
     * the expected output.
     *
     * @throws IOException if there is a problem serializing the baseline
     */
    @Test
    public void testImaBlacklistBaselineWithoutDescriptionsToCsv() throws IOException {
        Assert.assertEquals(
                CSVGenerator.blacklistToCsv(getTestImaBlacklistBaselineWithoutDescriptions()),
                IOUtils.toString(
                        getClass().getResource("/ima/IMABlacklistBaselineNoDescriptions.csv")
                )
        );
    }

    /**
     * Test tpmRecordsToCsv.
     * @throws IOException if there is a problem reading a ByteArray stream.
     * @throws ParseException if there is a problem parsing the csv.
     * @throws TPMBaselineGeneratorException if there is a problem generating the baseline.
     */
    @Test
    public void testTpmRecordsToCsv()
    throws IOException, ParseException, TPMBaselineGeneratorException {
        final String biosVersion = "abc";
        final String biosVendor = "HirsBIOS";
        final String biosReleaseDate = "04/25/2014";
        final String manufacturer = "U.S.A";
        final String productName = "The best, product";
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
        final String[] pcrValues = {
           "88e0f471e0bd2a2b75e135280bfedd81d8b5fad0",
           "5b93bba0a664a71052594a7095b207757703450b",
           "5b93bba0a664a71052594a7095b207757703450b",
           "5b93bba0a664a71052594a7095b207757703450b",
           "eadac28077eddf9e2f9bb8f75ede6f984e620230",
           "ffc13055368813dedf8c8cce642514d4b27ece27",
           "5b93bba0a664a71052594a7095b207757703450b",
           "5b93bba0a664a71052594a7095b207757703450b",
           "0000000000000000000000000000000000000000",
           "0000000000000000000000000000000000000000",
           "e252b835d77f7c9d6d1eb2aca66fbecfce1d3e5c",
           "0000000000000000000000000000000000000000",
           "0000000000000000000000000000000000000000",
           "0000000000000000000000000000000000000000",
           "0000000000000000000000000000000000000000",
           "8d9f771e534902d6b23bfae49d31ad505b629e03",
           "0000000000000000000000000000000000000000",
           "0000000000000000000000000000000000000000",
           "0000000000000000000000000000000000000000",
           "0000000000000000000000000000000000000000",
           "0000000000000000000000000000000000000000",
           "0000000000000000000000000000000000000000",
           "0000000000000000000000000000000000000000",
           "0000000000000000000000000000000000000000"
        };
        final FirmwareInfo expectedFirmwareInfo =
            new FirmwareInfo(biosVendor, biosVersion, biosReleaseDate);
        final HardwareInfo expectedHardwareInfo =
            new HardwareInfo(manufacturer, productName, version, serialNumber, chassisSerialNumber,
                    baseboardSerialNumber);
        final OSInfo expectedOSInfo =
            new OSInfo(osName, osVersion, osArch, distribution, distributionRelease);
        final TPMInfo expectedTPMInfo = new TPMInfo(tpmMake, tpmVersionMajor,
            tpmVersionMinor, tpmVersionRevMajor, tpmVersionRevMinor);
        final TpmWhiteListBaseline tpmBaseline = new TpmWhiteListBaseline("TPMBaseline");
        tpmBaseline.setFirmwareInfo(expectedFirmwareInfo);
        tpmBaseline.setHardwareInfo(expectedHardwareInfo);
        tpmBaseline.setOSInfo(expectedOSInfo);
        tpmBaseline.setTPMInfo(expectedTPMInfo);
        int index = 0;
        try {
            for (index = 0; index < pcrValues.length; index++) {
                TPMMeasurementRecord record = new TPMMeasurementRecord(index,
                  new Digest(DigestAlgorithm.SHA1, Hex.decodeHex(pcrValues[index].toCharArray())));
                tpmBaseline.addToBaseline(record);
            }
        } catch (DecoderException e) {
            Assert.fail("Trouble creating a TPMMeasurementRecord on index " + index);
        }

        final String csv = CSVGenerator.tpmRecordsToCsv(tpmBaseline);

        final TPMBaselineGenerator tbg = new TPMBaselineGenerator();
        final InputStream istream = new ByteArrayInputStream(Charsets.UTF_8.encode(csv).array());
        final TpmWhiteListBaseline baselineFromCSV =
                tbg.generateWhiteListBaselineFromCSVFile("Copied Baseline", istream);
        istream.close();

        // Test the expected data values from the CSV
        Assert.assertEquals(baselineFromCSV.getFirmwareInfo(), expectedFirmwareInfo);
        Assert.assertEquals(baselineFromCSV.getHardwareInfo(), expectedHardwareInfo);
        Assert.assertEquals(baselineFromCSV.getOSInfo(), expectedOSInfo);
        Assert.assertEquals(baselineFromCSV.getTPMInfo(), expectedTPMInfo);

        final Set<TPMMeasurementRecord> setFromCSV = baselineFromCSV.getPcrRecords();
        Assert.assertEquals(setFromCSV.size(), pcrValues.length, "The number of PCR records"
                + " found on the baseline does not match the number supposedly set.");
        try {
            for (final TPMMeasurementRecord record : setFromCSV) {
                Assert.assertEquals(record.getHash(),
                    new Digest(DigestAlgorithm.SHA1,
                            Hex.decodeHex(pcrValues[record.getPcrId()].toCharArray())));
            }
        } catch (DecoderException e) {
            Assert.fail("Trouble verifying a TPMMeasurementRecord on index " + index);
        }
    }

    /**
     * Get a test IMA blacklist baseline whose entries have descriptions.
     * @return the test IMA blacklist baseline
     */
    public static ImaBlacklistBaseline getTestImaBlacklistBaselineWithDescriptions() {
        ImaBlacklistBaseline baseline = new ImaBlacklistBaseline("Test Baseline");
        baseline.addToBaseline(new ImaBlacklistRecord(
                ImaBlacklistRecordTest.PATH,
                ImaBlacklistRecordTest.HASH,
                ImaBlacklistRecordTest.DESC
        ));
        baseline.addToBaseline(new ImaBlacklistRecord(
                ImaBlacklistRecordTest.PATH,
                ImaBlacklistRecordTest.DESC
        ));
        baseline.addToBaseline(new ImaBlacklistRecord(
                ImaBlacklistRecordTest.HASH,
                "some, weird?  \" text"
        ));
        return baseline;
    }

    /**
     * Get a test IMA blacklist baseline whose entries do not have descriptions.
     * @return the test IMA blacklist baseline
     */
    public static ImaBlacklistBaseline getTestImaBlacklistBaselineWithoutDescriptions() {
        ImaBlacklistBaseline baseline = new ImaBlacklistBaseline("Test Baseline");
        baseline.addToBaseline(new ImaBlacklistRecord(
                ImaBlacklistRecordTest.PATH,
                ImaBlacklistRecordTest.HASH
        ));
        baseline.addToBaseline(new ImaBlacklistRecord(
                ImaBlacklistRecordTest.PATH
        ));
        baseline.addToBaseline(new ImaBlacklistRecord(
                ImaBlacklistRecordTest.HASH
        ));
        return baseline;
    }
}
