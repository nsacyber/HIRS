package hirs.data.persist;

import hirs.data.persist.enums.DigestAlgorithm;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import hirs.foss.XMLCleaner;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * IMAReportTest is a unit test class for the IMAReport class.
 */
public class IMAReportTest extends SpringPersistenceTest {

    private static final Logger LOGGER = LogManager.getLogger(IMAReportTest.class);
    private static final String DEFAULT_REPORT =
            "/reports/ima/ima.xml";
    private static final String TEST_BOOTCYCLE_ID = "2015-04-10T14:55:44-0400";
    private static final String DEFAULT_PATH = "/path/to/my/file";
    private static final String DEFAULT_DIGEST =
            "098306e430e1121d3eb2967df6227b011c79c722";

    /**
     * Initializes a <code>SessionFactory</code>. The factory is used for an
     * in-memory database that is used for testing.
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
     * Resets the test state to a known good state. This currently only resets
     * the database by removing all <code>Report</code> objects.
     */
    @AfterMethod
    public final void resetTestState() {
        LOGGER.debug("reset test state");
        LOGGER.debug("deleting all reports");
        Session session = sessionFactory.getCurrentSession();
        session.beginTransaction();
        try {
            final List<?> reports = session.createCriteria(Report.class).list();
            for (Object o : reports) {
                LOGGER.debug("deleting report: {}", o);
                session.delete(o);
            }
            LOGGER.debug("all reports removed");
        } finally {
            session.getTransaction().commit();
        }
    }

    /**
     * Tests instantiation of IMAReport object.
     */
    @Test
    public final void imaReportTest() {
        new IMAReport();
    }

    /**
     * Tests that bootcycle ID can be retrieved from IMA report.
     */
    @Test
    public final void setAndGetBootcycleId() {
        final IMAReport report = new IMAReport();
        report.setBootcycleId(TEST_BOOTCYCLE_ID);
        Assert.assertEquals(report.getBootcycleId(), TEST_BOOTCYCLE_ID);
    }

    /**
     * Tests that the default bootcycle ID is null.
     */
    @Test
    public final void getDefaultBootcycleId() {
        final IMAReport report = new IMAReport();
        Assert.assertEquals(report.getBootcycleId(), null);
    }

    /**
     * Tests that boot cycle ID can be set to null to remove from report.
     */
    @Test
    public final void setBootcycleIdNull() {
        final IMAReport report = new IMAReport();
        report.setBootcycleId(null);
        Assert.assertEquals(report.getBootcycleId(), null);
    }

    /**
     * Tests that the default index is set to zero and will be considered a full report.
     */
    @Test
    public final void getDefaultIndex() {
        final IMAReport report = new IMAReport();
        Assert.assertEquals(report.getIndex(), 0);
        Assert.assertTrue(report.isFullReport());
    }

    /**
     * Tests that an <code>IMAReport</code> can set its report index. If the index is not 0, it
     * should not be considered a full report.
     */
    @Test
    public final void setAndGetIndex() {
        final IMAReport report = new IMAReport();
        final int index = 100;
        report.setIndex(index);
        Assert.assertEquals(report.getIndex(), index);
        Assert.assertFalse(report.isFullReport());
    }

    /**
     * Tests that a negative value cannot be used for an IMA report index.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public final void setInvalidIndex() {
        final IMAReport report = new IMAReport();
        final int invalidIndex = -1;
        report.setIndex(invalidIndex);
    }

    /**
     * Tests adding a record to the report.
     */
    @Test
    public final void addRecord() {
        IMAReport report = new IMAReport();
        IMAMeasurementRecord record;
        record = getTestRecord(DEFAULT_PATH, DEFAULT_DIGEST);
        report.addRecord(record);
        Set<IMAMeasurementRecord> records = report.getRecords();
        Set<IMAMeasurementRecord> expectedRecords =
                new LinkedHashSet<>();
        expectedRecords.add(record);
        Assert.assertEquals(records, expectedRecords);
    }

    /**
     * Tests adding a null record throws an error.
     */
    @Test(expectedExceptions = NullPointerException.class)
    public final void addNullRecord() {
        IMAReport report = new IMAReport();
        report.addRecord(null);
    }

    /**
     * Tests that addToRecords handles duplicate records. Records are not
     * guaranteed to be unique, so they could be in a report multiple times.
     */
    @Test
    public final void addDuplicateRecord() {
        IMAReport report = new IMAReport();
        IMAMeasurementRecord record;
        record = getTestRecord(DEFAULT_PATH, DEFAULT_DIGEST);
        report.addRecord(record);
        report.addRecord(record);
        Set<IMAMeasurementRecord> records = report.getRecords();
        Set<IMAMeasurementRecord> expectedRecords =
                new LinkedHashSet<>();
        expectedRecords.add(record);
        expectedRecords.add(record);
        Assert.assertEquals(records, expectedRecords);
    }

    /**
     * Tests that getRecords returns the appropriate value.
     */
    @Test
    public final void getRecords() {
        IMAReport report = new IMAReport();
        IMAMeasurementRecord record;
        record = getTestRecord(DEFAULT_PATH, DEFAULT_DIGEST);
        report.addRecord(record);
        Set<IMAMeasurementRecord> records = report.getRecords();
        Set<IMAMeasurementRecord> expectedRecords =
                new LinkedHashSet<>();
        expectedRecords.add(record);
        Assert.assertEquals(records, expectedRecords);
    }

    /**
     * Tests that records can be removed from a report.
     */
    @Test
    public final void removeFromReport() {
        boolean removed;
        IMAReport report = new IMAReport();
        IMAMeasurementRecord record;
        IMAMeasurementRecord record2;
        record = getTestRecord(DEFAULT_PATH, DEFAULT_DIGEST);
        record2 =
                getTestRecord("/another/file/path",
                        "098306e430e1121d3eb2967df6227b011c79c733");
        report.addRecord(record);
        report.addRecord(record2);
        Set<IMAMeasurementRecord> records = report.getRecords();
        Assert.assertTrue(records.contains(record));
        Assert.assertTrue(records.contains(record2));
        Assert.assertEquals(records.size(), 2);
        removed = report.removeRecord(record);
        Assert.assertTrue(removed);
        Assert.assertFalse(records.contains(record));
        Assert.assertTrue(records.contains(record2));
        Assert.assertEquals(records.size(), 1);
    }

    /**
     * Tests that removeFromReport() handles attempt to remove measurement not
     * found in record.
     */
    @Test
    public final void removeFromReportNotFound() {
        IMAReport report = new IMAReport();
        IMAMeasurementRecord record;
        record = getTestRecord(DEFAULT_PATH, DEFAULT_DIGEST);
        report.addRecord(record);
        report.removeRecord(record);
        Assert.assertFalse(report.removeRecord(getTestRecord(DEFAULT_PATH, DEFAULT_DIGEST)));
    }

    /**
     * Tests that an <code>IMAReport</code> can be saved using Hibernate.
     *
     * @throws Exception
     *             in case of improperly formatted xml
     */
    @Test
    public final void testSaveReport() throws Exception {
        LOGGER.debug("save IMA report test started");
        Session session = sessionFactory.getCurrentSession();
        session.beginTransaction();
        final Report r = getTestReport();
        session.save(r);
        session.getTransaction().commit();
    }

    /**
     * Tests that an <code>IMAReport</code> can be saved and retrieved. This
     * saves a <code>IMAReport</code> in the repo. Then a new session is
     * created, and the report is retrieved and its properties verified.
     *
     * @throws Exception
     *             in case of improperly formatted xml
     */
    @Test
    public final void testGetReport() throws Exception {
        LOGGER.debug("get IMA report test started");
        Session session = sessionFactory.getCurrentSession();
        session.beginTransaction();
        final IMAReport r = getTestReport();
        LOGGER.debug("saving report");
        final UUID id = (UUID) session.save(r);
        session.getTransaction().commit();

        session = sessionFactory.getCurrentSession();
        session.beginTransaction();
        LOGGER.debug("getting report");
        final IMAReport testReport =
                (IMAReport) session.get(IMAReport.class, id);

        LOGGER.debug("verifying reports's properties");
        Assert.assertNotNull(testReport.getId());
        Assert.assertEquals(testReport.getReportType(), r.getReportType());
        Assert.assertEquals(testReport.getBootcycleId(), r.getBootcycleId());
        Assert.assertEquals(testReport.getIndex(), r.getIndex());

        System.out.println(testReport.getRecords().getClass());
        System.out.println(r.getRecords().getClass());
        Assert.assertEquals(testReport.getRecords(), r.getRecords());

        // wait to close the transaction, since we have lazy-loaded collections
        session.getTransaction().commit();
    }

    /**
     * Tests that a <code>IMAReport</code> can be stored in the repository and
     * deleted.
     *
     * @throws Exception
     *             in case of improperly formatted xml
     */
    @Test
    public final void testDeleteReport() throws Exception {
        LOGGER.debug("delete IMA report test started");
        Session session = sessionFactory.getCurrentSession();
        session.beginTransaction();
        final IMAReport r = getTestReport();
        LOGGER.debug("saving report");
        final UUID id = (UUID) session.save(r);
        session.getTransaction().commit();

        session = sessionFactory.getCurrentSession();
        session.beginTransaction();
        LOGGER.debug("getting report");
        final IMAReport r2 = (IMAReport) session.get(IMAReport.class, id);
        session.delete(r2);
        session.getTransaction().commit();

        session = sessionFactory.getCurrentSession();
        session.beginTransaction();
        LOGGER.debug("getting report again");
        final IMAReport r3 = (IMAReport) session.get(IMAReport.class, id);
        session.getTransaction().commit();
        Assert.assertNull(r3);
    }

    /**
     * Creates an IMAReport instance usable for testing.
     *
     * @return
     *      a test IMAReport
     * @throws Exception
     *      if an error is encountered in unmarshalling the report
     */
    public static IMAReport getTestReport() throws Exception {
        InputStream in = IMAReportTest.class.getResourceAsStream(
                DEFAULT_REPORT
        );
        IMAReport report;
        try {
            String reportString = IOUtils.toString(in, "UTF-8");
            report = getReportFromXML(reportString);
            Set<IMAMeasurementRecord> recordSet = new HashSet<>(report.getRecords());
            for (IMAMeasurementRecord record : recordSet) {
                report.removeRecord(record);
                record.setReport(report);
                report.addRecord(record);
            }
        } finally {
            in.close();
        }
        report.setBootcycleId(TEST_BOOTCYCLE_ID);
        return report;
    }

    /**
     * Tests that a <code>IMAReport</code> can be correctly marshalled and
     * unmarshalled.
     *
     * @throws Exception
     *             if anything unexepected happens
     */
    @Test
    public final void marshalUnmarshalTest() throws Exception {

        final IMAReport imaReport = getTestReport();
        final String xml = getXMLFromReport(imaReport);
        final IMAReport imaReportFromXML = getReportFromXML(xml);
        Assert.assertEquals(imaReportFromXML.getBootcycleId(),
                imaReport.getBootcycleId());
    }

    private String getXMLFromReport(final IMAReport imaReport)
            throws JAXBException {
        String xml = null;
        JAXBContext context = JAXBContext.newInstance(IMAReport.class);
        Marshaller marshaller = context.createMarshaller();
        StringWriter writer = new StringWriter();
        marshaller.marshal(imaReport, writer);
        xml = writer.toString();
        xml = XMLCleaner.stripNonValidXMLCharacters(xml);
        return xml;
    }

    private static IMAReport getReportFromXML(final String xml)
            throws JAXBException {
        IMAReport imaReport;
        JAXBContext context = JAXBContext.newInstance(IMAReport.class);
        Unmarshaller unmarshaller = context.createUnmarshaller();
        StringReader reader = new StringReader(xml);
        imaReport = (IMAReport) unmarshaller.unmarshal(reader);
        return imaReport;
    }

    private IMAMeasurementRecord getTestRecord(final String path,
            final String hash) {
        final int sha1HashLength = 20;
        final String sPath;
        if (path == null) {
            sPath = DEFAULT_PATH;
        } else {
            sPath = path;
        }
        byte[] bHash = null;
        if (hash == null) {
            bHash = getTestDigest(sha1HashLength);
        } else {
            try {
                bHash = Hex.decodeHex(hash.toCharArray());
            } catch (DecoderException e) {
                LOGGER.error("error decoding digest bytes", e);
            }
        }
        final Digest digest = new Digest(DigestAlgorithm.SHA1, bHash);
        return new IMAMeasurementRecord(sPath, digest);
    }

    private byte[] getTestDigest(final int count) {
        final byte[] ret = new byte[count];
        for (int i = 0; i < count; ++i) {
            ret[i] = (byte) i;
        }
        return ret;
    }

}
