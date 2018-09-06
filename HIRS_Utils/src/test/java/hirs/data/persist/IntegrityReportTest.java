package hirs.data.persist;

import hirs.foss.XMLCleaner;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.hibernate.Session;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * IntegrityReportTest is a unit test class for IntegrityReports.
 */
public class IntegrityReportTest extends HibernateTest<IntegrityReport> {
    private IntegrityReport defaultReport;

    /**
     * Initializes an <code>IntegrityReport</code> with all report types.
     */
    @BeforeClass
    public final void initializeReport() {
        defaultReport = getTestReport();
    }

    /**
     * Makes a new report to start fresh for the next test.
     */
    @AfterMethod
    public final void resetTestReport() {
        defaultReport = getTestReport();
    }


    /**
     * Make a test report that can be used in this class and other test classes.
     * This one contains all three appraisers.
     * @return a test report
     */
    private static IntegrityReport getTestReport() {
        IntegrityReport integrityReport = new IntegrityReport();
        integrityReport.addReport(DeviceInfoReportTest.getTestReport());
        return integrityReport;
    }

    /**
     * Tests that an IntegrityReport can be instantiated.
     */
    @Test
    public final void integrityReport() {
        new IntegrityReport();
    }

    /**
     * Tests that a report can be added to an IntegrityReport.
     */
    @Test
    public final void addReport() {
        TestReport report = new TestReport();
        IntegrityReport integrityReport = new IntegrityReport();
        integrityReport.addReport(report);
        Set<Report> reportSet = integrityReport.getReports();
        Assert.assertNotNull(reportSet);
        Assert.assertTrue(reportSet.contains(report));
        Assert.assertEquals(reportSet.size(), 1);
    }

    /**
     * Tests that addReport handles duplicate Reports.
     */
    @Test
    public final void addDuplicateReport() {
        TestReport report = new TestReport();
        IntegrityReport integrityReport = new IntegrityReport();
        integrityReport.addReport(report);
        integrityReport.addReport(report);
        Set<Report> reportSet = integrityReport.getReports();
        Assert.assertNotNull(reportSet);
        Assert.assertTrue(reportSet.contains(report));
        Assert.assertEquals(reportSet.size(), 1);
    }

    /**
     * Tests that the Integrity Report does not contain a TPMReport unless one was added.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public final void extractNonexistentTPMReport() {
        TestReport report = new TestReport();
        IntegrityReport integrityReport = new IntegrityReport();
        integrityReport.addReport(report);
        integrityReport.extractReport(TPMReport.class);
    }

    /**
     * Tests that the Integrity Report does not contain a DeviceInfoReport unless one was added.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public final void extractNonexistentDeviceInfoReport() {
        IntegrityReport integrityReport = new IntegrityReport();
        integrityReport.extractReport(DeviceInfoReport.class);
    }

    /**
     * Tests that the Integrity Report does not contain an IMAReport unless one was added.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public final void extractNonexistentIMAReport() {
        TestReport report = new TestReport();
        IntegrityReport integrityReport = new IntegrityReport();
        integrityReport.addReport(report);
        integrityReport.extractReport(IMAReport.class);
    }

    /**
     * Tests that a report can be removed.
     */
    @Test
    public final void removeReport() {
        TestReport report = new TestReport();
        TestReport report2 = new TestReport();
        IntegrityReport integrityReport = new IntegrityReport();
        integrityReport.addReport(report);
        integrityReport.addReport(report2);
        Set<Report> reports = integrityReport.getReports();
        Assert.assertTrue(reports.contains(report));
        Assert.assertTrue(reports.contains(report2));
        Assert.assertEquals(reports.size(), 2);
        integrityReport.removeReport(report);
        Assert.assertFalse(reports.contains(report));
        Assert.assertTrue(reports.contains(report2));
        Assert.assertEquals(reports.size(), 1);
    }

    /**
     * Tests that removeReport() handles attempts to remove a report that is not
     * part of the IntegrityReport.
     */
    @Test
    public final void removeReportNotFound() {
        TestReport report = new TestReport();
        TestReport report2 = new TestReport();
        IntegrityReport integrityReport = new IntegrityReport();
        integrityReport.addReport(report);
        Set<Report> reports = integrityReport.getReports();
        Assert.assertTrue(reports.contains(report));
        Assert.assertFalse(reports.contains(report2));
        Assert.assertEquals(reports.size(), 1);
        integrityReport.removeReport(report2);
        Assert.assertTrue(reports.contains(report));
        Assert.assertFalse(reports.contains(report2));
        Assert.assertEquals(reports.size(), 1);
    }

    /**
     * Tests that an IntegrityReport can be correctly marshalled and
     * unmarshalled.
     *
     * @throws JAXBException
     *             if the test XML objects are not created correctly
     */
    @Test
    public final void marshalUnmarshalTest() throws JAXBException {
        TestReport report = new TestReport();
        TestReport report2 = new TestReport();
        IntegrityReport integrityReport = new IntegrityReport();
        integrityReport.addReport(report);
        integrityReport.addReport(report2);
        String xml = getXMLFromReport(integrityReport);
        IntegrityReport integrityReportFromXML = getReportFromXML(xml);
        Assert.assertEquals(2, integrityReportFromXML.getReports().size());
    }

    /**
     * Tests that an IntegrityReport without any child reports can be correctly
     * saved and retrieved from the database.
     */
    @Test
    public final void testPersistingNoReports() {
        IntegrityReport integrityReport = new IntegrityReport();
        IntegrityReport savedIntegrityReport = saveAndRetrieve(integrityReport);
        Assert.assertEquals(savedIntegrityReport, integrityReport);
        Assert.assertEquals(
                savedIntegrityReport.getReports(),
                integrityReport.getReports()
        );
    }

    /**
     * Tests that an IntegrityReport with multiple child reports can be
     * correctly saved and retrieved from the database.
     *
     * @throws Exception
     *      if there is an error unmarshalling an IMA report
     */
    @Test
    public final void testPersistingManyReports() throws Exception {
        IntegrityReport integrityReport = new IntegrityReport();
        integrityReport.addReport(DeviceInfoReportTest.getTestReport());
        integrityReport.addReport(IMAReportTest.getTestReport());
        integrityReport.addReport(TPMReportTest.getTestReport());
        integrityReport.addReport(new TestReport());

        IntegrityReport savedIntegrityReport = saveAndRetrieve(integrityReport);
        Assert.assertEquals(savedIntegrityReport, integrityReport);
        Assert.assertEquals(savedIntegrityReport, integrityReport);
        Assert.assertEquals(
                savedIntegrityReport.getReports(),
                integrityReport.getReports()
        );
    }

    @Override
    protected final IntegrityReport getDefault(final Session session) {
        return defaultReport;
    }

    @Override
    protected final Class<?> getDefaultClass() {
        return defaultReport.getClass();
    }

    @Override
    protected final void update(final IntegrityReport object) {
        object.addReport(new TestReport());
    }

    @Override
    protected final void assertGetEqual(final IntegrityReport defaultObject,
                                        final IntegrityReport retrieved) {
        Assert.assertEquals(defaultObject, retrieved);
    }

    @Override
    protected final void assertUpdateEqual(final IntegrityReport defaultObject,
                                           final IntegrityReport update) {
        Assert.assertEquals(defaultObject, update);
    }

    @Override
    protected final Class<?>[] getCleanupClasses() {
        return new Class<?>[] {IntegrityReport.class};
    }





    private String getXMLFromReport(final IntegrityReport integrityReport)
            throws JAXBException {
        ArrayList<Class<? extends Report>> classList =
                new ArrayList<>();
        classList.addAll(ReportUtils
                .getReportTypes("hirs"));
        String xml = null;
        JAXBContext context =
                JAXBContext.newInstance(classList
                        .toArray(new Class[classList.size()]));
        Marshaller marshaller = context.createMarshaller();
        StringWriter writer = new StringWriter();
        marshaller.marshal(integrityReport, writer);
        xml = writer.toString();
        xml = XMLCleaner.stripNonValidXMLCharacters(xml);
        return xml;
    }

    private IntegrityReport getReportFromXML(final String xml)
            throws JAXBException {
        ArrayList<Class<? extends Report>> classesList =
                new ArrayList<>();
        classesList.addAll(ReportUtils
                .getReportTypes("hirs"));
        IntegrityReport integrityReport;
        JAXBContext context =
                JAXBContext.newInstance(classesList
                        .toArray(new Class[classesList.size()]));
        Unmarshaller unmarshaller = context.createUnmarshaller();
        StringReader reader = new StringReader(xml);
        integrityReport = (IntegrityReport) unmarshaller.unmarshal(reader);
        return integrityReport;
    }
}
