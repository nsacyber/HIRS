package hirs.data.persist;

import hirs.data.persist.enums.DigestAlgorithm;
import hirs.data.persist.baseline.TpmWhiteListBaseline;
import hirs.data.persist.baseline.TPMBaseline;
import hirs.data.persist.baseline.Baseline;
import static hirs.data.persist.TPMMeasurementRecord.MAX_PCR_ID;
import static hirs.data.persist.TPMMeasurementRecord.MIN_PCR_ID;
import static hirs.data.persist.DeviceInfoReport.NOT_SPECIFIED;

import hirs.persist.BaselineManager;
import hirs.persist.DBBaselineManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import hirs.utils.Callback;
import hirs.utils.Functional;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.hibernate.Session;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * <code>TPMBaselineTest</code> represents a unit test class for <code>TPMBaseline</code>.
 */
public class TPMBaselineTest extends SpringPersistenceTest {

    private static final Logger LOGGER = LogManager.getLogger(TPMBaselineTest.class);

    /**
     * Initializes a <code>SessionFactory</code>. The factory is used for an in-memory database that
     * is used for testing.
     */
    @BeforeClass
    public final void setup() {
        LOGGER.debug("retrieving session factory");
    }

    /**
     * Closes the <code>SessionFactory</code> from setup.
     */
    @AfterClass
    public final void tearDown() {
        LOGGER.debug("closing session factory");
    }

    /**
     * Resets the test state to a known good state. This currently only resets the database by
     * removing all <code>Baseline</code> objects.
     */
    @AfterMethod
    public final void resetTestState() {
        LOGGER.debug("reset test state");
        LOGGER.debug("deleting all baselines");
        Session session = sessionFactory.getCurrentSession();
        session.beginTransaction();
        final List<?> baselines = session.createCriteria(Baseline.class).list();
        for (Object o : baselines) {
            LOGGER.debug("deleting baseline: {}", o);
            session.delete(o);
        }
        LOGGER.debug("all baselines removed");
        session.getTransaction().commit();
    }

    /**
     * Tests instantiation of new <code>PCRMeasurementRecord</code>.
     */
    @Test
    public final void tpmBaseline() {
        TpmWhiteListBaseline baseline = new TpmWhiteListBaseline("testTPMBaseline");
        Assert.assertNotNull(baseline);
        Assert.assertEquals(baseline.getFirmwareInfo(), new FirmwareInfo());
        Assert.assertEquals(baseline.getHardwareInfo(), new HardwareInfo());
        Assert.assertEquals(baseline.getOSInfo(), new OSInfo());
        Assert.assertEquals(baseline.getTPMInfo(), new TPMInfo());
    }

    /**
     * Tests that <code>PCRMeasurementRecord</code> constructor throws a NullPointerException with
     * null hash.
     */
    @Test(expectedExceptions = NullPointerException.class)
    public final void tpmBaselineNullTest() {
        new TpmWhiteListBaseline(null);
    }

    /**
     * Tests adding PCRMeasurementRecord to TPM baseline.
     */
    @Test
    public final void addToBaseline() {
        TPMBaseline baseline = new TpmWhiteListBaseline("testTPMBaseline");
        TPMMeasurementRecord pcrRecord = new TPMMeasurementRecord(0,
                getDigest("3d5f3c2f7f3003d2e4baddc46ed4763a4954f648"));
        baseline.addToBaseline(pcrRecord);
        Set<TPMMeasurementRecord> recordList = baseline.getPcrRecords();
        Assert.assertNotNull(recordList);
        Assert.assertEquals(recordList.size(), 1);
        Assert.assertTrue(recordList.contains(pcrRecord));
    }

    /**
     * Tests addToBaseline() throws a NullPointerException with null PCR record.
     */
    @Test(expectedExceptions = NullPointerException.class)
    public final void addToBaselineNullRecord() {
        TPMBaseline baseline = new TpmWhiteListBaseline("testTPMBaseline");
        TPMMeasurementRecord pcrRecord = null;
        baseline.addToBaseline(pcrRecord);
    }

    /**
     * Tests addToBaseline() throws a IllegalArgumentException when attempting to store duplicate
     * PCR records.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public final void addToBaselineDuplicateRecord() {
        TPMBaseline baseline = new TpmWhiteListBaseline("testTPMBaseline");
        TPMMeasurementRecord pcrRecord = new TPMMeasurementRecord(0,
                getDigest("3d5f3c2f7f3003d2e4baddc46ed4763a4954f648"));
        baseline.addToBaseline(pcrRecord);
        baseline.addToBaseline(pcrRecord);
    }

    /**
     * Tests that getName() returns the baseline name.
     */
    @Test
    public final void getName() {
        String name;
        TPMBaseline baseline = new TpmWhiteListBaseline("testTPMBaseline");
        name = baseline.getName();
        Assert.assertNotNull(name);
    }

    /**
     * Tests that getPCRHashes() returns a valid list of hashes when multiple hashes are added.
     */
    @Test
    public final void getPCRHashes() {
        final int pcrId = 0;
        final Digest[] hashes = {
                getDigest("3d5f3c2f7f3003d2e4baddc46ed4763a4954f648"),
                getDigest("3d5f3c2f7f3003d2e4baddc46ed4763a4954f650"),
                getDigest("3d5f3c2f7f3003d2e4baddc46ed4763a4954f662"),
                getDigest("3d5f3c2f7f3003d2e4baddc46ed4763a4954f674")
        };
        final TPMBaseline baseline = new TpmWhiteListBaseline("testTPMBaseline");
        for (int i = 0; i < hashes.length; ++i) {
            baseline.addToBaseline(new TPMMeasurementRecord(pcrId, hashes[i]));
        }
        final Set<Digest> baselineHashes = baseline.getPCRHashes(pcrId);
        Assert.assertNotNull(baselineHashes);
        Assert.assertEquals(baselineHashes.size(), hashes.length);
        for (int i = 0; i < hashes.length; ++i) {
            Assert.assertTrue(baselineHashes.contains(hashes[i]));
        }
    }

    /**
     * Tests that getPCRHashes() returns a empty list of hashes when none have been added.
     */
    @Test
    public final void getPCRHashesNoneAdded() {
        final TPMBaseline baseline = new TpmWhiteListBaseline("testTPMBaseline");
        for (int i = MIN_PCR_ID; i <= MAX_PCR_ID; ++i) {
            final Set<Digest> hashes = baseline.getPCRHashes(i);
            Assert.assertNotNull(hashes);
            Assert.assertEquals(hashes.size(), 0);
        }
    }

    /**
     * Tests that getPCRHash() throws a IllegalArgumentException if PCR id is invalid (not between 0
     * and 23).
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public final void getPCRHashInvalidPcr() {
        final int pcr35 = 35;
        final TPMBaseline baseline = new TpmWhiteListBaseline("testTPMBaseline");
        final TPMMeasurementRecord pcrRecord = new TPMMeasurementRecord(0,
                getDigest("3d5f3c2f7f3003d2e4baddc46ed4763a4954f648"));
        baseline.addToBaseline(pcrRecord);
        baseline.getPCRHashes(pcr35);
    }

    /**
     * Tests that getPCRRecords() returns a list of PCR measurement records.
     */
    @Test
    public final void getPCRRecords() {
        TPMBaseline baseline = new TpmWhiteListBaseline("testTPMBaseline");
        TPMMeasurementRecord pcrRecord = new TPMMeasurementRecord(0,
                getDigest("3d5f3c2f7f3003d2e4baddc46ed4763a4954f648"));
        baseline.addToBaseline(pcrRecord);
        Set<TPMMeasurementRecord> recordList = baseline.getPcrRecords();
        Assert.assertNotNull(recordList);
        Assert.assertEquals(recordList.size(), 1);
        Assert.assertTrue(recordList.contains(pcrRecord));
    }

    /**
     * Tests that isInBaseline() returns true if record is found.
     */
    @Test
    public final void isInBaseline() {
        boolean matchFound = false;
        TPMBaseline baseline = new TpmWhiteListBaseline("testTPMBaseline");
        TPMMeasurementRecord pcrRecord = new TPMMeasurementRecord(0,
                getDigest("3d5f3c2f7f3003d2e4baddc46ed4763a4954f648"));
        TPMMeasurementRecord pcrRecord1 = new TPMMeasurementRecord(0,
                getDigest("3d5f3c2f7f3003d2e4baddc46ed4763a4954f648"));
        baseline.addToBaseline(pcrRecord);
        matchFound = baseline.isInBaseline(pcrRecord1);
        Assert.assertTrue(matchFound);
    }

    /**
     * Tests that isInBaseline() returns false if record not found.
     */
    @Test
    public final void isInBaselineReturnFalse() {
        final int pcr0 = 0;
        final int pcr10 = 10;
        boolean matchFound = false;
        TPMBaseline baseline = new TpmWhiteListBaseline("testTPMBaseline");
        TPMMeasurementRecord pcrRecord = new TPMMeasurementRecord(pcr0,
                getDigest("3d5f3c2f7f3003d2e4baddc46ed4763a4954f648"));
        TPMMeasurementRecord pcrRecord1 = new TPMMeasurementRecord(pcr10,
                getDigest("3d5f3c2f7f3003d2e4baddc46ed4763a4954f648"));
        baseline.addToBaseline(pcrRecord);
        matchFound = baseline.isInBaseline(pcrRecord1);
        Assert.assertFalse(matchFound);
    }

    /**
     * Tests that isInBaseline() returns false if pcrRecord is null.
     */
    @Test
    public final void isInBaselineNull() {
        TPMBaseline baseline = new TpmWhiteListBaseline("testTPMBaseline");
        Assert.assertFalse(baseline.isInBaseline(null));
    }

    /**
     * Tests that removeFromBaseline() removes PCR record from baseline.
     */
    @Test
    public final void removeFromBaseline() {
        boolean matchFound = false;
        final int pcrZero = 0;
        final int pcrTen = 10;
        TPMBaseline baseline = new TpmWhiteListBaseline("testTPMBaseline");
        TPMMeasurementRecord pcrRecord = new TPMMeasurementRecord(pcrZero,
                getDigest("3d5f3c2f7f3003d2e4baddc46ed4763a4954f648"));
        TPMMeasurementRecord pcrRecord1 = new TPMMeasurementRecord(pcrTen,
                getDigest("aa5f3c2f7f3003d2e4baddc46ed4763a4954f648"));
        baseline.addToBaseline(pcrRecord);
        baseline.addToBaseline(pcrRecord1);
        final boolean removed = baseline.removeFromBaseline(pcrRecord);
        Assert.assertTrue(removed);
        matchFound = baseline.isInBaseline(pcrRecord);
        Assert.assertFalse(matchFound);
    }

    /**
     * Tests that removeFromBaseline() returns false if pcrRecord to remove is null.
     */
    @Test
    public final void removeFromBaselineNull() {
        final int pcr0 = 0;
        TPMBaseline baseline = new TpmWhiteListBaseline("testTPMBaseline");
        TPMMeasurementRecord pcrRecord = new TPMMeasurementRecord(pcr0,
                getDigest("3d5f3c2f7f3003d2e4baddc46ed4763a4954f648"));
        baseline.addToBaseline(pcrRecord);
        Assert.assertFalse(baseline.removeFromBaseline(null));
    }

    /**
     * Tests that removeFromBaseline() returns false if pcrRecord to remove is not found in
     * baseline.
     */
    @Test
    public final void removeFromBaselineInvalidRecord() {
        final int pcr0 = 0;
        final int pcr10 = 0;
        TPMBaseline baseline = new TpmWhiteListBaseline("testTPMBaseline");
        TPMMeasurementRecord pcrRecord = new TPMMeasurementRecord(pcr0,
                getDigest("3d5f3c2f7f3003d2e4baddc46ed4763a4954f648"));
        TPMMeasurementRecord pcrRecord1 = new TPMMeasurementRecord(pcr10,
                getDigest("aa5f3c2f7f3003d2e4baddc46ed4763a4954f648"));
        baseline.addToBaseline(pcrRecord);
        Assert.assertFalse(baseline.removeFromBaseline(pcrRecord1));
    }

    /**
     * Tests that a <code>TPMBaseline</code> can be saved using Hibernate.
     */
    @Test
    public final void testSaveBaseline() {
        LOGGER.debug("save TPM baseline test started");
        Session session = sessionFactory.getCurrentSession();
        session.beginTransaction();
        final Baseline b = getDefaultWhiteListBaseline();
        session.save(b);
        session.getTransaction().commit();
    }

    /**
     * Tests that an <code>TPMBaseline</code> can be saved and retrieved. This saves a
     * <code>TPMBaseline</code> in the repo. Then a new session is created, and the baseline is
     * retrieved and its properties verified.
     */
    @Test
    public final void testGetBaseline() {
        LOGGER.debug("get TPM baseline test started");
        Session session = sessionFactory.getCurrentSession();
        session.beginTransaction();
        final TPMBaseline b = getDefaultWhiteListBaseline();
        LOGGER.debug("saving baseline");
        final UUID id = (UUID) session.save(b);
        session.getTransaction().commit();

        session = sessionFactory.getCurrentSession();
        session.beginTransaction();
        LOGGER.debug("getting baseline");
        final TPMBaseline testBaseline =
                (TPMBaseline) session.get(TPMBaseline.class, id);
        session.getTransaction().commit();

        LOGGER.debug("verifying baseline's properties");
        final TPMBaseline expected = getDefaultWhiteListBaseline();
        Assert.assertEquals(testBaseline.getName(), expected.getName());
        Assert.assertEquals(testBaseline.getPcrRecords(),
                expected.getPcrRecords());
    }

    /**
     * Tests that a baseline can be saved and then later updated. This saves the baseline, retrieves
     * it, adds a baseline record to it, and then retrieves it and verifies it.
     */
    @Test
    public final void testUpdateBaseline() {
        LOGGER.debug("update TPM baseline test started");
        Session session = sessionFactory.getCurrentSession();
        session.beginTransaction();
        LOGGER.debug("saving baseline");
        final UUID id = (UUID) session.save(getDefaultWhiteListBaseline());
        session.getTransaction().commit();

        final Digest hash =
                getDigest("445f3c2f7f3003d2e4baddc46ed4763a4954f650");
        final TPMMeasurementRecord addedRecord =
                new TPMMeasurementRecord(MIN_PCR_ID, hash);

        LOGGER.debug("updating baseline");
        session = sessionFactory.getCurrentSession();
        session.beginTransaction();
        TPMBaseline testBaseline =
                (TPMBaseline) session.get(TPMBaseline.class, id);
        testBaseline.addToBaseline(addedRecord);
        session.update(testBaseline);
        session.getTransaction().commit();

        LOGGER.debug("getting baseline");
        session = sessionFactory.getCurrentSession();
        session.beginTransaction();
        testBaseline = (TPMBaseline) session.get(TPMBaseline.class, id);
        session.getTransaction().commit();

        LOGGER.debug("verifying baseline's properties");
        final TPMBaseline expected = getDefaultWhiteListBaseline();
        expected.addToBaseline(addedRecord);
        Assert.assertEquals(testBaseline.getName(), expected.getName());
        Assert.assertEquals(testBaseline.getPcrRecords(),
                expected.getPcrRecords());
    }

    /**
     * Tests that a <code>TPMBaseline</code> can be archived.
     */
    @Test
    public final void testArchiveBaseline() {
        final BaselineManager mgr = new DBBaselineManager(sessionFactory);
        LOGGER.debug("archive TPM baseline test started");

        final TPMBaseline baseline = new TpmWhiteListBaseline("TestTPMBaseline");
        mgr.saveBaseline(baseline);
        mgr.archive(baseline.getName());
        TPMBaseline retrievedBaseline = (TPMBaseline) mgr.getBaseline(baseline.getName());
        Assert.assertTrue(retrievedBaseline.isArchived());
    }

    /**
     * Tests that {@link Functional#select(Collection, Callback)} can filter a collection of
     * TPMBaselines down to those that match the given parameters.
     */
    @Test
    public void testSelectBaselinesByDeviceInfo() {
        LOGGER.debug("testSelectBaselinesByDeviceInfo test started");

        TpmWhiteListBaseline emptyBaseline = new TpmWhiteListBaseline("Empty TPM Baseline");
        TpmWhiteListBaseline baseline1 = new TpmWhiteListBaseline("TPM Baseline 1");
        baseline1.setOSInfo(TPMBaselineTest.getTestOSInfo());

        List<TpmWhiteListBaseline> baselines = new ArrayList<>();
        baselines.add(emptyBaseline);
        baselines.add(baseline1);

        Collection<TpmWhiteListBaseline> matchingBaselines = Functional.select(
                baselines,
                new Callback<TpmWhiteListBaseline, Boolean>() {
                    @Override
                    public Boolean call(final TpmWhiteListBaseline candidate) {
                        return getTestOSInfo().getOSVersion().equals(
                                candidate.getOSInfo().getOSVersion()
                        );
                    }
                }
        );


        Assert.assertTrue(matchingBaselines.contains(baseline1));
        Assert.assertFalse(matchingBaselines.contains(emptyBaseline));

        TpmWhiteListBaseline baseline2 = new TpmWhiteListBaseline("TPM Baseline 2");
        baseline2.setOSInfo(TPMBaselineTest.getTestOSInfo());
        baseline2.setHardwareInfo(TPMBaselineTest.getTestHardwareInfo());
        baselines.add(baseline2);

        matchingBaselines = Functional.select(
                baselines,
                new Callback<TpmWhiteListBaseline, Boolean>() {
                    @Override
                    public Boolean call(final TpmWhiteListBaseline candidate) {
                        return getTestOSInfo().getOSVersion().equals(
                                candidate.getOSInfo().getOSVersion()
                        );
                    }
                }
        );

        Assert.assertFalse(matchingBaselines.contains(emptyBaseline));
        Assert.assertTrue(matchingBaselines.contains(baseline1));
        Assert.assertTrue(matchingBaselines.contains(baseline2));

        matchingBaselines = Functional.select(
                baselines,
                new Callback<TpmWhiteListBaseline, Boolean>() {
                    @Override
                    public Boolean call(final TpmWhiteListBaseline candidate) {
                        boolean matches = getTestOSInfo().getOSVersion().equals(
                                candidate.getOSInfo().getOSVersion());
                        matches |= getTestHardwareInfo().getProductName().equals(
                                candidate.getHardwareInfo().getProductName());
                        return matches;
                    }
                }
        );

        Assert.assertFalse(matchingBaselines.contains(emptyBaseline));
        Assert.assertTrue(matchingBaselines.contains(baseline1));
        Assert.assertTrue(matchingBaselines.contains(baseline2));

        TpmWhiteListBaseline baseline3 = new TpmWhiteListBaseline("TPM Baseline 3");
        final OSInfo osInfo = new OSInfo("MINIX", "1", "SPARC", "1", "EL9000");
        baseline3.setOSInfo(osInfo);
        baseline3.setHardwareInfo(TPMBaselineTest.getTestHardwareInfo());
        baselines.add(baseline3);

        matchingBaselines = Functional.select(
                baselines,
                new Callback<TpmWhiteListBaseline, Boolean>() {
                    @Override
                    public Boolean call(final TpmWhiteListBaseline candidate) {
                        return osInfo.getOSVersion().equals(
                                candidate.getOSInfo().getOSVersion()
                        );
                    }
                }
        );

        Assert.assertEquals(matchingBaselines.size(), 1);
        Assert.assertTrue(matchingBaselines.contains(baseline3));

        TpmWhiteListBaseline baseline4 = new TpmWhiteListBaseline("TPM Baseline 4");
        final FirmwareInfo firmwareInfo = TPMBaselineTest.getTestFirmwareInfo();
        baseline4.setFirmwareInfo(firmwareInfo);
        baselines.add(baseline4);

        matchingBaselines = Functional.select(
                baselines,
                new Callback<TpmWhiteListBaseline, Boolean>() {
                    @Override
                    public Boolean call(final TpmWhiteListBaseline candidate) {
                        return firmwareInfo.getBiosVersion().equals(
                                candidate.getFirmwareInfo().getBiosVersion()
                        );
                    }
                }
        );

        Assert.assertEquals(matchingBaselines.size(), 1);
        Assert.assertTrue(matchingBaselines.contains(baseline4));

        TpmWhiteListBaseline baseline5 = new TpmWhiteListBaseline("TPM Baseline 4");
        final TPMInfo tpmInfo = TPMBaselineTest.getTestTPMInfo();
        baseline5.setTPMInfo(tpmInfo);
        baselines.add(baseline5);

        matchingBaselines = Functional.select(
                baselines,
                new Callback<TpmWhiteListBaseline, Boolean>() {
                    @Override
                    public Boolean call(final TpmWhiteListBaseline candidate) {
                        return tpmInfo.getTPMMake().equals(
                                candidate.getTPMInfo().getTPMMake());
                    }
                }
        );

        Assert.assertEquals(matchingBaselines.size(), 1);
        Assert.assertTrue(matchingBaselines.contains(baseline5));
    }

    /**
     * Tests that a <code>TPMBaseline</code> contains FirmwareInfo.
     */
    @Test
    public final void testGetFirmwareInfo() {
        LOGGER.debug("get FirmwareInfo from TPM baseline test started");
        final TpmWhiteListBaseline baseline = getDefaultWhiteListBaseline();
        Assert.assertNotNull(baseline.getFirmwareInfo());
        Assert.assertEquals(baseline.getFirmwareInfo().getBiosVendor(), NOT_SPECIFIED);
    }

    /**
     * Tests that a <code>TPMBaseline</code> contains HardwareInfo.
     */
    @Test
    public final void testGetHardwareInfo() {
        LOGGER.debug("get HardwareInfo from TPM baseline test started");
        final TpmWhiteListBaseline baseline = getDefaultWhiteListBaseline();
        Assert.assertNotNull(baseline.getHardwareInfo());
        Assert.assertEquals(baseline.getHardwareInfo().getVersion(), NOT_SPECIFIED);
    }

    /**
     * Tests that a <code>TPMBaseline</code> contains OSInfo.
     */
    @Test
    public final void testGetOSInfo() {
        LOGGER.debug("get OSInfo from TPM baseline test started");
        final TpmWhiteListBaseline baseline = getDefaultWhiteListBaseline();
        Assert.assertNotNull(baseline.getOSInfo());
        Assert.assertEquals(baseline.getOSInfo().getOSArch(), NOT_SPECIFIED);
    }

    /**
     * Tests that a <code>TPMBaseline</code> contains TPMInfo.
     */
    @Test
    public final void testGetTPMInfo() {
        LOGGER.debug("get TPMInfo from TPM baseline test started");
        final TpmWhiteListBaseline baseline = getDefaultWhiteListBaseline();
        Assert.assertNotNull(baseline.getTPMInfo());
        Assert.assertEquals(baseline.getTPMInfo().getTPMMake(), NOT_SPECIFIED);
    }

    /**
     * Tests that a <code>TPMBaseline</code> can store FirmwareInfo.
     */
    @Test
    public final void testSetFirmwareInfo() {
        LOGGER.debug("set FirmwareInfo on TPM baseline test started");
        final TpmWhiteListBaseline baseline = getDefaultWhiteListBaseline();
        final FirmwareInfo firmwareInfo = getTestFirmwareInfo();
        baseline.setFirmwareInfo(firmwareInfo);
        Assert.assertEquals(baseline.getFirmwareInfo(), firmwareInfo);
    }

    /**
     * Tests that a <code>TPMBaseline</code> can store HardwareInfo.
     */
    @Test
    public final void testSetHardwareInfo() {
        LOGGER.debug("set HardwareInfo on TPM baseline test started");
        final TpmWhiteListBaseline baseline = getDefaultWhiteListBaseline();
        HardwareInfo hardwareInfo = getTestHardwareInfo();
        baseline.setHardwareInfo(hardwareInfo);
        Assert.assertEquals(baseline.getHardwareInfo(), hardwareInfo);
    }

    /**
     * Tests that a <code>TPMBaseline</code> can store OSInfo.
     */
    @Test
    public final void testSetOSInfo() {
        LOGGER.debug("set OSInfo on TPM baseline test started");
        final OSInfo osInfo = getTestOSInfo();
        final TpmWhiteListBaseline baseline = getDefaultWhiteListBaseline();
        baseline.setOSInfo(osInfo);
        Assert.assertEquals(baseline.getOSInfo(), osInfo);
    }

    /**
     * Tests that a <code>TPMBaseline</code> can store TPMInfo.
     */
    @Test
    public final void testSetTPMInfo() {
        LOGGER.debug("set TPMInfo on TPM baseline test started");
        final TpmWhiteListBaseline baseline = getDefaultWhiteListBaseline();
        TPMInfo tpmInfo = getTestTPMInfo();
        baseline.setTPMInfo(tpmInfo);
        Assert.assertEquals(baseline.getTPMInfo(), tpmInfo);
    }

    /**
     * Verify that a baseline with valid data returns false from isEmpty().
     */
    @Test
    public final void testIsEmptyFalse() {
        final TpmWhiteListBaseline baseline = getDefaultWhiteListBaseline();
        Assert.assertFalse(baseline.isEmpty());
    }

    /**
     * Verify that a baseline with no data returns true from isEmpty().
     */
    @Test
    public final void testIsEmptyTrue() {
        final TpmWhiteListBaseline baseline = new TpmWhiteListBaseline();
        Assert.assertTrue(baseline.isEmpty());
    }

    private TpmWhiteListBaseline getDefaultWhiteListBaseline() {
        final int pcr0 = 0;
        TpmWhiteListBaseline baseline = new TpmWhiteListBaseline("TestTpmWhiteListBaseline");
        TPMMeasurementRecord pcrRecord = new TPMMeasurementRecord(pcr0,
                getDigest("3d5f3c2f7f3003d2e4baddc46ed4763a4954f648"));
        baseline.addToBaseline(pcrRecord);
        return baseline;
    }

    private Digest getDigest(final String hash) {
        try {
            final byte[] bytes = Hex.decodeHex(hash.toCharArray());
            return new Digest(DigestAlgorithm.SHA1, bytes);
        } catch (DecoderException e) {
            LOGGER.error("unable to create digest", e);
            throw new RuntimeException("unable to create digest", e);
        }
    }

    /**
     * @return a test FirmwareInfo object
     */
    public static FirmwareInfo getTestFirmwareInfo() {
        final String biosVendor = "Me";
        final String biosVersion = "1.0";
        final String biosReleaseDate = "Yesterday";
        return new FirmwareInfo(biosVendor, biosVersion, biosReleaseDate);
    }

    /**
     * @return a test HardwareInfo object
     */
    public static HardwareInfo getTestHardwareInfo() {
        final String manufacturer = "US";
        final String productName = "HirsChips";
        final String version = "yes";
        final String serialNumber = "100";
        final String chassisSerialNumber = "9_9";
        final String baseboardSerialNumber = "ABC123";
        return new HardwareInfo(manufacturer, productName, version, serialNumber,
                chassisSerialNumber, baseboardSerialNumber);
    }

    /**
     * @return a test OSInfo object
     */
    public static OSInfo getTestOSInfo() {
        final String osName = "HIOS";
        final String osVersion = "-2";
        final String osArch = "columnar";
        final String distribution = "narrow";
        final String distributionRelease = "ok";
        return new OSInfo(osName, osVersion, osArch, distribution, distributionRelease);
    }

    /**
     * @return a test TPMInfo object
     */
    public static TPMInfo getTestTPMInfo() {
        final String tpmMake = "The Best";
        final short tpmVersionMajor = 0;
        final short tpmVersionMinor = 0;
        final short tpmVersionRevMajor = 0;
        final short tpmVersionRevMinor = 0;
        return new TPMInfo(tpmMake, tpmVersionMajor, tpmVersionMinor,
                tpmVersionRevMajor, tpmVersionRevMinor);
    }
}
