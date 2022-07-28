package hirs.data.persist;

import hirs.data.persist.enums.DigestAlgorithm;
import hirs.data.persist.tpm.PcrComposite;
import hirs.data.persist.tpm.PcrInfoShort;
import hirs.data.persist.tpm.PcrSelection;
import hirs.data.persist.tpm.Quote2;
import hirs.data.persist.tpm.QuoteData;
import hirs.data.persist.tpm.QuoteInfo2;
import hirs.data.persist.tpm.QuoteSignature;
import hirs.foss.XMLCleaner;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.namespace.QName;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.apache.logging.log4j.LogManager.getLogger;

/**
 * TPMReportTest is a unit test class for the TPMReport class.
 */
public class

TPMReportTest extends SpringPersistenceTest {
    /**
     * Calling {@link #getTestReport()} will return a TPMReport with a TPMMeasurementRecord with
     * this PCR ID.
     */
    public static final int TEST_REPORT_PCR_ID = 2;

    private static final Logger LOGGER = getLogger(TPMReportTest.class);
    private static final QName REPORT_QNAME = new QName("http://www.trusted"
            + "computinggroup.org/XML/SCHEMA/Integrity_Report_v1_0#", "Report");

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
        Session session = sessionFactory.unwrap(org.hibernate.Session.class);
        session.beginTransaction();
        CriteriaBuilder builder = session.getCriteriaBuilder();
        CriteriaQuery<Report> criteriaQuery = builder.createQuery(Report.class);
        Root<Report> root = criteriaQuery.from(Report.class);
        criteriaQuery.select(root);
        Query<Report> query = session.createQuery(criteriaQuery);
        try {
//            final List<?> reports = session.createCriteria(Report.class).list();
            final List<Report> reports = query.getResultList();
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
     * Tests that a <code>TPMReport</code> can be saved using Hibernate.
     */
    @Test
    public final void testSaveReport() {
        Session session = sessionFactory.unwrap(org.hibernate.Session.class);
        session.beginTransaction();
        final TPMReport r = getTestReport();
        session.save(r);
        session.getTransaction().commit();
    }

    /**
     * Tests that a <code>TPMReport</code> can be saved and retrieved. This
     * saves a <code>TPMReport</code> in the repo. Then a new session is
     * created, and the report is retrieved and its properties verified.
     */
    @Test
    public final void testGetReport() {
        Session session = sessionFactory.unwrap(org.hibernate.Session.class);
        session.beginTransaction();
        final TPMReport r = getTestReport();
        final UUID id = (UUID) session.save(r);
        session.getTransaction().commit();

        session = sessionFactory.unwrap(org.hibernate.Session.class);
        session.beginTransaction();
        final TPMReport testReport = (TPMReport) session.get(TPMReport.class, id);

        Assert.assertNotNull(testReport.getId());
        Assert.assertEquals(testReport.getReportType(), r.getReportType());
        Assert.assertEquals(
                testReport.getTPMMeasurementRecords(),
                r.getTPMMeasurementRecords()
        );

        Assert.assertEquals(testReport.getQuoteData(), r.getQuoteData());

        // wait to close the transaction, since we have lazy-loaded collections
        session.getTransaction().commit();
    }

    /**
     * Tests that a <code>TPMReport</code> can be stored in the repository and
     * deleted.
     */
    @Test(enabled = false)
    public final void testDeleteReport() {
        Session session = sessionFactory.unwrap(org.hibernate.Session.class);

        session.beginTransaction();
        final TPMReport r = getTestReport();
        session.save(r);
        session.getTransaction().commit();

        session.beginTransaction();
        CriteriaBuilder builder = session.getCriteriaBuilder();
        CriteriaQuery<Report> criteriaQuery = builder.createQuery(Report.class);
        Root<Report> root = criteriaQuery.from(Report.class);
        criteriaQuery.select(root);
        Query<Report> query = session.createQuery(criteriaQuery);
//        List<Report> reports = CollectionHelper.getArrayListOf(Report.class,
//                session.createCriteria(Report.class).list());
        List<Report> reports = query.getResultList();
        session.getTransaction().commit();
        Assert.assertEquals(reports.size(), 1);

        session.beginTransaction();
        session.delete(r);
        session.getTransaction().commit();

        session.beginTransaction();
        builder = session.getCriteriaBuilder();
        criteriaQuery = builder.createQuery(Report.class);
        root = criteriaQuery.from(Report.class);
        criteriaQuery.select(root);
        query = session.createQuery(criteriaQuery);
        reports = query.getResultList();
//        reports = CollectionHelper.getArrayListOf(Report.class,
//                session.createCriteria(Report.class).list());
        session.getTransaction().commit();
        Assert.assertEquals(reports.size(), 0);
    }

    /**
     * Tests that a TPMReport can be instantiated.
     */
    @Test
    public final void testTPMReport() {
        new TPMReport(getTestQuoteData());
    }

    /**
     * Tests that a NullPointerException is thrown if null data is passed to the
     * TPMReport.
     */
    @Test(expectedExceptions = NullPointerException.class)
    public final void testNullQuoteData() {
        new TPMReport(null);
    }

    /**
     * Tests that a TPMReport can be correctly marshalled and unmarshalled.
     *
     * @throws JAXBException
     *             if the test XML objects are not created correctly
     */
    @Test
    public final void marshalUnmarshalTest() throws JAXBException {
        TPMReport tpmReport = new TPMReport(getTestQuoteData());
        String xml = getXMLFromReport(tpmReport);
        TPMReport tpmReportFromXML = getReportFromXML(xml);
        Assert.assertEquals(1, tpmReportFromXML.getTPMMeasurementRecords().size());
    }

    /**
     * Creates a TPMReport instance usable for testing.
     *
     * @return
     *      a test TPMReport
     */
    public static TPMReport getTestReport() {
        return new TPMReport(getTestQuoteData());
    }

    private String getXMLFromReport(final TPMReport tpmReport) throws JAXBException {
        String xml = null;
        JAXBContext context = JAXBContext.newInstance(TPMReport.class);
        JAXBElement<TPMReport> element = createReport(tpmReport);
        StringWriter writer = new StringWriter();
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty("jaxb.formatted.output", Boolean.TRUE);
        marshaller.marshal(element, writer);
        xml = writer.toString();
        xml = XMLCleaner.stripNonValidXMLCharacters(xml);
        return xml;
    }

    private TPMReport getReportFromXML(final String xml) throws JAXBException {
        JAXBContext context;
        context = JAXBContext.newInstance(TPMReport.class);
        Unmarshaller unmarshaller = context.createUnmarshaller();
        StringReader reader = new StringReader(xml);
        TPMReport report = (TPMReport) unmarshaller.unmarshal(reader);
        return report;
    }

    /**
     * Returns blank quote data for unit testing.
     *
     * @return blank quote data for unit testing.
     */
    public static QuoteData getTestQuoteData() {
        final int sha1HashLength = 20;
        TPMMeasurementRecord record =
                new TPMMeasurementRecord(TEST_REPORT_PCR_ID, getTestDigest(sha1HashLength));

        PcrSelection pcrSelection = new PcrSelection(new byte[1]);

        List<TPMMeasurementRecord> pcrValueList = new ArrayList<>();
        pcrValueList.add(record);
        PcrComposite pcrComposite = new PcrComposite(pcrSelection, pcrValueList);

        PcrInfoShort pcrInfoShort = new PcrInfoShort(
                pcrSelection, (short) 0, new byte[0], pcrComposite
        );

        QuoteInfo2 quoteInfo2 = new QuoteInfo2(pcrInfoShort, new byte[0]);

        QuoteSignature quoteSignature = new QuoteSignature(new byte[0]);

        return new QuoteData(new Quote2(quoteInfo2), quoteSignature);
    }

    /**
     * Retrieves a Digest for testing. The value of the bytes included in the digest will increase
     * consecutively, starting at 0x00 and will increment to 0x<hex value of count>.
     *
     * @param count
     *            Number of bytes to include in the Digest
     * @return test Digest
     */
    public static Digest getTestDigest(final int count) {
        final byte[] ret = new byte[count];
        for (int i = 0; i < count; ++i) {
            ret[i] = (byte) i;
        }
        return new Digest(DigestAlgorithm.SHA1, ret);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link TPMReport }
     * {@code >}.
     */
    @XmlElementDecl(
            namespace = "http://www.trustedcomputinggroup.org/XML/SCHEMA/"
                    + "Integrity_Report_v1_0#",
            name = "Report")
    private JAXBElement<TPMReport> createReport(final TPMReport value) {
        return new JAXBElement<>(REPORT_QNAME, TPMReport.class, null, value);
    }
}
