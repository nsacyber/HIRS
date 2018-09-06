package hirs.persist;

import hirs.FilteredRecordsList;
import hirs.data.bean.SimpleImaRecordBean;
import hirs.data.persist.DeviceInfoReport;
import hirs.data.persist.Digest;
import hirs.data.persist.DigestAlgorithm;
import hirs.data.persist.ExamineState;
import hirs.data.persist.FirmwareInfo;
import hirs.data.persist.HardwareInfo;
import hirs.data.persist.IMAMeasurementRecord;
import hirs.data.persist.IMAReport;
import hirs.data.persist.IntegrityReport;
import hirs.data.persist.NetworkInfo;
import hirs.data.persist.OSInfo;
import hirs.data.persist.Report;
import hirs.data.persist.ReportSummary;
import hirs.data.persist.SpringPersistenceTest;
import hirs.data.persist.TPMInfo;
import hirs.data.persist.TPMMeasurementRecord;
import hirs.data.persist.TPMReport;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import hirs.data.persist.TPMReportTest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static hirs.persist.IMARecordField.HASH;
import static hirs.persist.IMARecordField.PATH;
import static hirs.persist.IMARecordScope.DEVICE;
import static hirs.persist.IMARecordScope.NONE;
import static hirs.persist.IMARecordScope.REPORT;
import static org.apache.logging.log4j.LogManager.getLogger;

/**
 * Tests DBReportManager.
 */
public class DBReportManagerTest extends SpringPersistenceTest {

    private static final Logger LOGGER = getLogger(DBReportManagerTest.class);
    private static final boolean ASCENDING = true;
    private static final boolean DESCENDING = false;
    private static final int START = 0;
    private static final int LIMIT = 5;
    private static final int TOTAL = 10;
    private static final String NO_SEARCH = "";
    private static final String INSENSITIVE_SEARCH = "xYz";
    private static final String SEARCH = "xyz";
    private final Map<String, Boolean> searchColumns = new HashMap<>();

    private ReportManager reportManager;
    private ReportSummaryManager reportSummaryManager;
    private int totalRecords = 0;
    private int searchableRecords = 0;

    /**
     * Initializes a <code>SessionFactory</code>. The factory is used for an in-memory database that
     * is used for testing.
     */
    @BeforeClass
    public void setup() {
        reportManager = new DBReportManager(sessionFactory);
        reportSummaryManager = new DBReportSummaryManager(sessionFactory);
    }

    /**
     * Closes the <code>SessionFactory</code> from setup.
     */
    @AfterClass
    public void tearDown() {

    }

    /**
     * Resets the test state to a known good state. This currently only resets
     * the database by removing all <code>Report</code> objects.
     */
    @AfterMethod
    public final void resetTestState() {
        LOGGER.debug("reset test state");
        LOGGER.debug("deleting all reports and report summaries");
        Session session = sessionFactory.getCurrentSession();
        session.beginTransaction();

        try {
            final List<?> reportSummary = session.createCriteria(ReportSummary.class).list();
            for (Object o : reportSummary) {
                LOGGER.debug("deleting report summary: {}", o);
                session.delete(o);
            }
            LOGGER.debug("all reports summaries removed");
        } finally {
            session.getTransaction().commit();
        }

        session = sessionFactory.getCurrentSession();
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

    @SuppressWarnings("checkstyle:avoidinlineconditionals")
    private static String recordToString(final IMAMeasurementRecord record) {
        if (record == null) {
            return "IMAMeasurementRecord null";
        } else {
            return "IMAMeasurementRecord "
                    + " ima: " + (record.getReport() == null ? null : record.getReport().getId())
                    + " rec: " + record.getId() + " " + record;
        }
    }

    private static Digest getDefaultDigest() {
        final int sha1HashLength = 20;
        return new Digest(DigestAlgorithm.SHA1, getTestDigest(sha1HashLength));
    }

    private static byte[] getTestDigest(final int count) {
        Random r = new Random();
        final byte[] ret = new byte[count];
        for (int i = 0; i < count; ++i) {
            ret[i] = (byte) r.nextInt(16);
        }
        return ret;
    }

    private class TestIMAMeasurementReport {

        private final String name;
        private final ReportSummary rs;
        private final IntegrityReport ir;
        private final DeviceInfoReport dev;
        private final IMAReport ima;
        private final TPMReport tpm;

        TestIMAMeasurementReport(final String name, final int imaIndex,
                final Collection<String> extraRecords) {
            this.name = name;

            // create device info report
            dev = new DeviceInfoReport(new NetworkInfo(name, null, null),
                    new OSInfo(), new FirmwareInfo(), new HardwareInfo(), new TPMInfo());
            reportManager.saveReport(dev);

            // create ima report and ima records
            ima = new IMAReport();
            ima.setBootcycleId(UUID.randomUUID().toString());
            ima.setIndex(imaIndex);
            for (int i = 0; i < TOTAL; i++) {
                addIMARecord(i);
            }
            totalRecords += TOTAL;
            if (extraRecords != null) {
                for (final String suffix : extraRecords) {
                    addIMARecord(suffix);
                }
                totalRecords += extraRecords.size();
                searchableRecords += extraRecords.size();
            }
            reportManager.saveReport(ima);

            // create tpm report
            tpm = new TPMReport(TPMReportTest.getTestQuoteData());
            reportManager.saveReport(tpm);

            // assemble integrity report
            ir = new IntegrityReport();
            ir.addReport(dev);
            ir.addReport(ima);
            ir.addReport(tpm);
            reportManager.saveReport(ir);

            // create report summary
            rs = new ReportSummary();
            rs.setClientHostname(name);
            rs.setTimestamp(new Date());
            rs.setReportType("hirs.data.persist.IntegrityReport");
            rs.setReport(ir);
            reportSummaryManager.saveReportSummary(rs);

        }

        TestIMAMeasurementReport(final String name, final int imaIndex) {
            this(name, imaIndex, null);
        }

        TestIMAMeasurementReport(final String name, final Collection<String> extraRecords) {
            this(name, 0, extraRecords);
        }

        TestIMAMeasurementReport(final String name) {
            this(name, 0, null);
        }

        final void addIMARecord(final Object suffix) {
            IMAMeasurementRecord rec = new IMAMeasurementRecord("/" + getName() + "/" + suffix,
                    getDefaultDigest());
            rec.setReport(getIMA());
            getIMA().addRecord(rec);
        }

        private String getName() {
            return name;
        }

        private ReportSummary getRS() {
            return rs;
        }

        private IntegrityReport getIR() {
            return ir;
        }

        private DeviceInfoReport getDev() {
            return dev;
        }

        private IMAReport getIMA() {
            return ima;
        }

        private TPMReport getTPM() {
            return tpm;
        }

    }

    private static FilteredRecordsList<IMAMeasurementRecord>
    convertBeanToRecords(final FilteredRecordsList<SimpleImaRecordBean>
                                 records) {
        FilteredRecordsList<IMAMeasurementRecord> imaRecords =
                new FilteredRecordsList<>();
        for (SimpleImaRecordBean imaRecordBean : records) {
            Digest digest = new Digest(DigestAlgorithm.SHA1,
                    imaRecordBean.getHash().getDigest());
            IMAMeasurementRecord imaRecord = new IMAMeasurementRecord(
                    imaRecordBean.getPath(), digest);
            imaRecords.add(imaRecord);
        }
        imaRecords.setRecordsFiltered(records.getRecordsFiltered());
        imaRecords.setRecordsTotal(records.getRecordsTotal());
        return imaRecords;
    }

    private static List<String> getExtraRecords() {
        final List<String> records = new ArrayList<>();
        for (int i = 0; i < TOTAL; i++) {
            records.add(SEARCH + "/" + i);
        }
        return records;
    }

    private static void assertResults(
            final FilteredRecordsList<IMAMeasurementRecord> records,
            final int size,
            final int total,
            final int filtered,
            final IMARecordField columnToOrder,
            final boolean ascending,
            final TestIMAMeasurementReport... reports) {

        // check record counts
        Assert.assertEquals(records.size(), size);
        Assert.assertEquals(records.getRecordsTotal(), total);
        Assert.assertEquals(records.getRecordsFiltered(), filtered);

        // check ordering
        String last = null;
        for (IMAMeasurementRecord record : records) {

            String curr = "";
            switch (columnToOrder) {
                case PATH:
                    curr = record.getPath();
                    break;
                case HASH:
                    curr = record.getHash().toString();
                    break;
                default:
                    Assert.fail("Unknown columnToOrder : " + columnToOrder);
                    break;
            }

            if (last != null) {
                if (ascending) {
                    Assert.assertTrue(curr.compareTo(last) >= 0);
                } else {
                    Assert.assertTrue(curr.compareTo(last) <= 0);
                }
            }

            last = curr;

        }

        // check for recordset completeness
        if (records.size() > 0 && records.size() == records.getRecordsTotal()) {

            final Set<UUID> foundIds = new HashSet<>();

            for (IMAMeasurementRecord record : records) {

                // record ima id
                final UUID imaID = record.getReport().getId();
                foundIds.add(imaID);

                boolean matchedId = false;
                boolean matchedPath = false;
                boolean matchedHash = false;
                for (TestIMAMeasurementReport report : reports) {

                    // match ima id
                    if (imaID.equals(report.getIMA().getId())) {
                        matchedId = true;

                        // match path and hash
                        for (IMAMeasurementRecord imaRecord : report.getIMA().getRecords()) {
                            if (record.getPath().equals(imaRecord.getPath())) {
                                matchedPath = true;
                            }
                            if (record.getHash().equals(imaRecord.getHash())) {
                                matchedHash = true;
                            }

                            // check for path/hash mismatch
                            if (matchedPath && !matchedHash) {
                                Assert.fail("Matched path, but not hash:"
                                        + " original: " + imaRecord
                                        + " query result: " + record);
                            } else if (matchedHash && !matchedPath) {
                                Assert.fail("Matched hash, but not path:"
                                        + " original: " + imaRecord
                                        + " query result: " + record);
                            }

                        }

                    }

                }

                Assert.assertTrue(matchedId, "Could not match ima id " + imaID + " for "
                        + recordToString(record));
                Assert.assertTrue(matchedPath, "Could not match path " + record.getPath()
                        + " for " + recordToString(record));
                Assert.assertTrue(matchedHash, "Could not match hash " + record.getHash());

            }

            // found records for all reports
            Assert.assertEquals(foundIds.size(), reports.length);

        }

    }

    private static void assertSimpleImaRecordsResults(
            final FilteredRecordsList<IMAMeasurementRecord> records,
            final int size,
            final int total,
            final int filtered,
            final IMARecordField columnToOrder,
            final boolean ascending,
            final TestIMAMeasurementReport... reports) {

        // check record counts
        Assert.assertEquals(records.size(), size);
        Assert.assertEquals(records.getRecordsTotal(), total);
        Assert.assertEquals(records.getRecordsFiltered(), filtered);

        // check ordering
        String last = null;
        for (IMAMeasurementRecord record : records) {

            String curr = "";
            switch (columnToOrder) {
                case PATH:
                    curr = record.getPath();
                    break;
                case HASH:
                    curr = record.getHash().toString();
                    break;
                default:
                    Assert.fail("Unknown columnToOrder : " + columnToOrder);
                    break;
            }

            if (last != null) {
                if (ascending) {
                    Assert.assertTrue(curr.compareTo(last) >= 0);
                } else {
                    Assert.assertTrue(curr.compareTo(last) <= 0);
                }
            }

            last = curr;

        }

        // check for recordset completeness
        if (records.size() > 0 && records.size() == records.getRecordsTotal()) {

            for (IMAMeasurementRecord record : records) {

                boolean matchedId = false;
                boolean matchedPath = false;
                boolean matchedHash = false;
                for (TestIMAMeasurementReport report : reports) {

                    // match path and hash
                    for (IMAMeasurementRecord imaRecord
                            : report.getIMA().getRecords()) {
                        if (record.getPath().equals(imaRecord.getPath())) {
                            matchedPath = true;
                        }
                        if (record.getHash().equals(imaRecord.getHash())) {
                            matchedHash = true;
                        }

                        // check for path/hash mismatch
                        if (matchedPath && !matchedHash) {
                            Assert.fail("Matched path, but not hash:"
                                    + " original: " + imaRecord
                                    + " query result: " + record);
                        } else if (matchedHash && !matchedPath) {
                            Assert.fail("Matched hash, but not path:"
                                    + " original: " + imaRecord
                                    + " query result: " + record);
                        }

                    }

                }

                Assert.assertTrue(matchedPath, "Could not match path "
                        + record.getPath() + " for " + recordToString(record));
                Assert.assertTrue(matchedHash, "Could not match hash "
                        + record.getHash());

            }
        }

    }

    /**
     * Tests {@link DBReportManager#updateReport(Report)}.
     * - Tests that a TPM report that has been persisted with measurement records can
     * be updated successfully when a change has been made to a measurement record.
     */
    @Test
    public void testUpdateReportAlteredMeasurementRecords() {
        TPMReport report = new TPMReport(TPMReportTest.getTestQuoteData());
        reportManager.saveReport(report);
        Assert.assertEquals(DBUtility.getCount(sessionFactory, Report.class), 1);
        List<TPMMeasurementRecord> tpmMeasurementRecords = report.getTPMMeasurementRecords();
        Assert.assertEquals(tpmMeasurementRecords.size(), 1);
        TPMMeasurementRecord tpmMeasurementRecord = tpmMeasurementRecords.get(0);
        Assert.assertEquals(tpmMeasurementRecord.getExamineState(), ExamineState.UNEXAMINED);
        tpmMeasurementRecord.setExamineState(ExamineState.EXAMINED);
        reportManager.updateReport(report);

        final TPMReport retrievedReport = (TPMReport) reportManager.getReport(report.getId());
        Assert.assertEquals(retrievedReport, report);
        final List<TPMMeasurementRecord> retrievedTpmMeasurementRecords =
                retrievedReport.getTPMMeasurementRecords();
        Assert.assertEquals(retrievedTpmMeasurementRecords.size(), 1);
        final TPMMeasurementRecord retrievedTpmMeasurementRecord =
                retrievedTpmMeasurementRecords.get(0);
        Assert.assertEquals(retrievedTpmMeasurementRecord.getExamineState(), ExamineState.EXAMINED);
    }

    /**
     * Tests ReportManager#testGetIMARecordsForREPORTLimitedWithoutRecords.<br>
     * - Tests that the correct number of records are returned when REPORT scope
     * is specified.<br>
     * - Tests PATH order DESCENDING.<br>
     * - Tests that limited number of records for the report are returned.<br>
     */
    @Test
    public void testGetIMARecordsForREPORTLimitedWithoutRecords() {

        final String deviceId = "testDevice-" + UUID.randomUUID();
        final TestIMAMeasurementReport report
                = new TestIMAMeasurementReport(deviceId);

        FilteredRecordsList<SimpleImaRecordBean> records
                = reportManager.getOrderedRecordListWithoutRecords(REPORT,
                report.getIR().getId().toString(), false, PATH, DESCENDING,
                START, LIMIT, NO_SEARCH, searchColumns);

        FilteredRecordsList<IMAMeasurementRecord> imaRecords =
                convertBeanToRecords(records);

        assertSimpleImaRecordsResults(imaRecords, LIMIT, TOTAL, TOTAL, PATH, DESCENDING, report);

    }

    /**
     * Tests ReportManager#testGetIMARecordsForREPORTWithoutRecords.<br>
     * - Tests that the correct number of records are returned when REPORT scope
     * is specified.<br>
     * - Tests PATH order DESCENDING.<br>
     */
    @Test
    public void testGetIMARecordsForREPORTWithoutRecords() {

        final String deviceId = "testDevice-" + UUID.randomUUID();
        final TestIMAMeasurementReport report
                = new TestIMAMeasurementReport(deviceId);

        FilteredRecordsList<SimpleImaRecordBean> records
                = reportManager.getOrderedRecordListWithoutRecords(REPORT,
                report.getIR().getId().toString(), false, PATH, DESCENDING,
                START, Integer.MAX_VALUE, NO_SEARCH, searchColumns);

        FilteredRecordsList<IMAMeasurementRecord> imaRecords =
                convertBeanToRecords(records);

        assertSimpleImaRecordsResults(imaRecords, TOTAL, TOTAL, TOTAL, PATH,
                DESCENDING, report);

    }

    /**
     * Tests ReportManager#testIMARecordsForREPORTStartingAtWithoutRecords.<br>
     * - Tests that the correct number of records are returned when REPORT scope
     * is specified.<br>
     * - Tests PATH order ASCENDING.<br>
     * - Tests that all records for the report are returned.<br>
     * - Tests that only records starting at the specified count are returned.<br>
     */
    @Test
    public void testIMARecordsForREPORTStartingAtWithoutRecords() {

        final String deviceId = "testDevice-" + UUID.randomUUID();
        final TestIMAMeasurementReport report
                = new TestIMAMeasurementReport(deviceId);

        FilteredRecordsList<SimpleImaRecordBean> records
                = reportManager.getOrderedRecordListWithoutRecords(REPORT,
                report.getIR().getId().toString(),
                false, PATH, ASCENDING, 5, Integer.MAX_VALUE, NO_SEARCH,
                searchColumns);

        FilteredRecordsList<IMAMeasurementRecord> imaRecords =
                convertBeanToRecords(records);

        assertResults(imaRecords, 5, TOTAL, TOTAL, PATH, ASCENDING, report);
        int i = 5;
        for (IMAMeasurementRecord imaMeasurementRecordRecords : imaRecords) {
            Assert.assertEquals(imaMeasurementRecordRecords.getPath(), "/"
                    + deviceId + "/" + i);
            i++;
        }

    }

    /**
     * Tests
     * ReportManager
     * #testGetIMARecordsForREPORTLimitedWithoutRecordsMultipleReports
     * .<br>
     * - Tests that the correct number of records are returned when REPORT
     * scope is specified.
     * .<br>
     * - Tests PATH order DESCENDING.<br>
     * - Tests that limited number of records for the report are returned.<br>
     * @throws InterruptedException if error generating while executing the
     * sleep
     */
    @Test
    public void
    testGetIMARecordsForREPORTLimitedWithoutRecordsMultipleReports(
    ) throws InterruptedException {

        final String deviceId = "testDevice-" + UUID.randomUUID();
        final TestIMAMeasurementReport report
                = new TestIMAMeasurementReport(deviceId);

        // Sleep for two seconds to enforce report time stamp difference
        TimeUnit.SECONDS.sleep(1);

        final TestIMAMeasurementReport newReport
                = new TestIMAMeasurementReport(deviceId);

        Assert.assertNotEquals(report.getIR().getId(),
                newReport.getIR().getId());

        FilteredRecordsList<SimpleImaRecordBean> records
                = reportManager.getOrderedRecordListWithoutRecords(REPORT,
                newReport.getIR().getId().toString(), false, PATH, DESCENDING,
                START, TOTAL, NO_SEARCH, searchColumns);

        FilteredRecordsList<IMAMeasurementRecord> imaRecords =
                convertBeanToRecords(records);

        assertSimpleImaRecordsResults(imaRecords, TOTAL, TOTAL, TOTAL, PATH,
                DESCENDING, newReport);

    }

    /**
     * Tests ReportManager#testIMARecordsForDEVICEStartingAtWithoutRecords.<br>
     * - Tests that the correct number of records are returned when REPORT scope
     * is specified.<br>
     * - Tests PATH order ASCENDING.<br>
     * - Tests that all records for the report are returned.<br>
     * - Tests that only records starting at the specified count are returned.<br>
     */
    @Test
    public void testIMARecordsForDEVICEStartingAtWithoutRecords() {

        final String deviceId = "testDevice-" + UUID.randomUUID();
        final TestIMAMeasurementReport report
                = new TestIMAMeasurementReport(deviceId);

        FilteredRecordsList<SimpleImaRecordBean> records
                = reportManager.getOrderedRecordListWithoutRecords(DEVICE, deviceId,
                false, PATH, DESCENDING, 5, Integer.MAX_VALUE, NO_SEARCH,
                searchColumns);

        FilteredRecordsList<IMAMeasurementRecord> imaRecords =
                convertBeanToRecords(records);

        assertResults(imaRecords, 5, TOTAL, TOTAL, PATH, DESCENDING, report);
        int i = 4;
        for (IMAMeasurementRecord record : imaRecords) {
            Assert.assertEquals(record.getPath(), "/" + deviceId + "/" + i);
            i--;
        }

    }

    /**
     * Tests ReportManager#testGetIMARecordsForDEVICELimitedWithoutRecords.<br>
     * - Tests that the correct number of records are returned when DEVICE
     * scope is specified.<br>
     * - Tests PATH order DESCENDING.<br>
     * - Tests that limited number of records for the report are returned.<br>
     */
    @Test
    public void testGetIMARecordsForDEVICELimitedWithoutRecords() {

        final String deviceId = "testDevice-" + UUID.randomUUID();
        final TestIMAMeasurementReport report
                = new TestIMAMeasurementReport(deviceId);

        FilteredRecordsList<SimpleImaRecordBean> records
                = reportManager.getOrderedRecordListWithoutRecords(DEVICE,
                deviceId, false, PATH, DESCENDING,
                START, LIMIT, NO_SEARCH, searchColumns);

        FilteredRecordsList<IMAMeasurementRecord> imaRecords =
                convertBeanToRecords(records);

        assertSimpleImaRecordsResults(imaRecords, LIMIT, TOTAL, TOTAL, PATH,
                DESCENDING, report);

    }

    /**
     * Tests ReportManager#testGetIMARecordsForDEVICEWithoutRecords.<br>
     * - Tests that the correct number of records are returned when DEVICE
     * scope is specified.<br>
     * - Tests PATH order DESCENDING.<br>
     */
    @Test
    public void testGetIMARecordsForDEVICEWithoutRecords() {

        final String deviceId = "testDevice-" + UUID.randomUUID();
        final TestIMAMeasurementReport report
                = new TestIMAMeasurementReport(deviceId);

        FilteredRecordsList<SimpleImaRecordBean> records
                = reportManager.getOrderedRecordListWithoutRecords(DEVICE,
                deviceId, false, PATH, DESCENDING,
                START, Integer.MAX_VALUE, NO_SEARCH, searchColumns);

        FilteredRecordsList<IMAMeasurementRecord> imaRecords =
                convertBeanToRecords(records);

        assertSimpleImaRecordsResults(imaRecords, TOTAL, TOTAL, TOTAL, PATH, DESCENDING,
                report);

    }

    /**
     * Tests
     * ReportManager
     * #testGetIMARecordsForDEVICELimitedWithoutRecordsMultipleReports
     * .<br>
     * - Tests that the correct number of records are returned when DEVICE
     * scope is specified and when reports since last full report is set to true
     * .<br>
     * - Tests PATH order DESCENDING.<br>
     * - Tests that limited number of records for the report are returned.<br>
     * @throws InterruptedException if error generating while executing the
     * sleep
     */
    @Test
    public void
    testGetIMARecordsForDEVICELimitedWithoutRecordsMultipleReports(
    ) throws InterruptedException {

        final String deviceId = "testDevice-" + UUID.randomUUID();
        final TestIMAMeasurementReport report
                = new TestIMAMeasurementReport(deviceId);

        // Sleep for two seconds to enforce report time stamp difference
        TimeUnit.SECONDS.sleep(1);

        final TestIMAMeasurementReport newReport
                = new TestIMAMeasurementReport(deviceId);

        Assert.assertNotEquals(report.getIR().getId(),
                newReport.getIR().getId());

        FilteredRecordsList<SimpleImaRecordBean> records
                = reportManager.getOrderedRecordListWithoutRecords(DEVICE,
                deviceId, true, PATH, DESCENDING,
                START, TOTAL, NO_SEARCH, searchColumns);

        FilteredRecordsList<IMAMeasurementRecord> imaRecords =
                convertBeanToRecords(records);

        assertSimpleImaRecordsResults(imaRecords, TOTAL, TOTAL, TOTAL, PATH,
                DESCENDING, newReport);

    }

    /**
     * Tests ReportManager#testGetIMARecordsForDEVICEFilteredAllWithoutRecords.
     * <br>
     * - Tests that the correct number of records are returned when DEVICE scope
     * is specified.<br>
     * - Tests PATH order DESCENDING.<br>
     * - Tests that all matching records for the device are returned.<br>
     * - Tests that only records matching the search criteria are returned.<br>
     */
    @Test
    public void testGetIMARecordsForDEVICEFilteredAllWithoutRecords() {

        final String deviceId = "testDevice-" + UUID.randomUUID();
        final TestIMAMeasurementReport report1
                = new TestIMAMeasurementReport(deviceId, getExtraRecords());
        final TestIMAMeasurementReport report2
                = new TestIMAMeasurementReport(deviceId, getExtraRecords());

        searchColumns.put("digest", true);
        searchColumns.put("path", true);

        FilteredRecordsList<SimpleImaRecordBean> records
                = reportManager.getOrderedRecordListWithoutRecords(DEVICE,
                deviceId, false, PATH, DESCENDING, START, Integer.MAX_VALUE,
                INSENSITIVE_SEARCH, searchColumns);

        FilteredRecordsList<IMAMeasurementRecord> imaRecords =
                convertBeanToRecords(records);

        assertSimpleImaRecordsResults(imaRecords, TOTAL * 2, TOTAL * 4,
                TOTAL * 2, PATH, DESCENDING, report1, report2);

    }

    /**
     * Tests ReportManager#getOrderedRecordList.<br>
     * - Tests that the correct number of records are returned when REPORT scope is specified.<br>
     * - Tests PATH order DESCENDING.<br>
     * - Tests that limited number of records for the report are returned.<br>
     */
    @Test
    public void testGetIMARecordsForREPORTLimited() {

        final String deviceId = "testDevice-" + UUID.randomUUID();
        final TestIMAMeasurementReport report
                = new TestIMAMeasurementReport(deviceId);

        FilteredRecordsList<IMAMeasurementRecord> records
                = reportManager.getOrderedRecordList(REPORT, report.getIR().getId().toString(),
                        false, PATH, DESCENDING, START, LIMIT, NO_SEARCH);

        assertResults(records, LIMIT, TOTAL, TOTAL, PATH, DESCENDING, report);

    }

    /**
     * Tests ReportManager#getOrderedRecordList.<br>
     * - Tests that the correct number of records are returned when REPORT scope is specified.<br>
     * - Tests HASH order DESCENDING.<br>
     * - Tests that all records for the report are returned.<br>
     */
    @Test
    public void testGetIMARecordsForREPORTFilteredAll() {

        final String deviceId = "testDevice-" + UUID.randomUUID();
        final TestIMAMeasurementReport report
                = new TestIMAMeasurementReport(deviceId, getExtraRecords());

        FilteredRecordsList<IMAMeasurementRecord> records
                = reportManager.getOrderedRecordList(REPORT, report.getIR().getId().toString(),
                        false, HASH, DESCENDING, START, Integer.MAX_VALUE,
                        INSENSITIVE_SEARCH);

        assertResults(records, TOTAL, TOTAL * 2, TOTAL, HASH, DESCENDING, report);

    }

    /**
     * Tests ReportManager#getOrderedRecordList.<br>
     * - Tests that the correct number of records are returned when REPORT scope is specified.<br>
     * - Tests PATH order ASCENDING.<br>
     * - Tests that the limited number of records for the report are returned.<br>
     */
    @Test
    public void testGetIMARecordsForREPORTFilteredLimited() {

        final String deviceId = "testDevice-" + UUID.randomUUID();
        final TestIMAMeasurementReport report
                = new TestIMAMeasurementReport(deviceId, getExtraRecords());

        FilteredRecordsList<IMAMeasurementRecord> records
                = reportManager.getOrderedRecordList(REPORT, report.getIR().getId().toString(),
                        false, PATH, ASCENDING, START, LIMIT, INSENSITIVE_SEARCH);

        assertResults(records, LIMIT, TOTAL * 2, TOTAL, PATH, ASCENDING, report);

    }

    /**
     * Tests ReportManager#getOrderedRecordList.<br>
     * - Tests that the correct number of records are returned when REPORT scope is specified.<br>
     * - Tests PATH order ASCENDING.<br>
     * - Tests that all records for the report are returned.<br>
     * - Tests that only records starting at the specified count are returned.<br>
     */
    @Test
    public void testIMARecordsForREPORTStartingAt() {

        final String deviceId = "testDevice-" + UUID.randomUUID();
        final TestIMAMeasurementReport report
                = new TestIMAMeasurementReport(deviceId);

        FilteredRecordsList<IMAMeasurementRecord> records
                = reportManager.getOrderedRecordList(REPORT, report.getIR().getId().toString(),
                        false, PATH, ASCENDING, 5, Integer.MAX_VALUE, NO_SEARCH);

        assertResults(records, 5, TOTAL, TOTAL, PATH, ASCENDING, report);
        int i = 5;
        for (IMAMeasurementRecord record : records) {
            Assert.assertEquals(record.getPath(), "/" + deviceId + "/" + i);
            i++;
        }

    }

    /**
     * Tests ReportManager#getOrderedRecordList.<br>
     * - Tests that the correct number of records are returned when DEVICE scope is specified.<br>
     * - Tests HASH order DESCENDING.<br>
     * - Tests that all records for the device are returned.<br>
     */
    @Test
    public void testGetIMARecordsForDEVICEAll() {

        final String deviceId = "testDevice-" + UUID.randomUUID();
        final TestIMAMeasurementReport report1
                = new TestIMAMeasurementReport(deviceId);
        final TestIMAMeasurementReport report2
                = new TestIMAMeasurementReport(deviceId);

        FilteredRecordsList<IMAMeasurementRecord> records
                = reportManager.getOrderedRecordList(DEVICE, deviceId,
                        false, HASH, ASCENDING, START, Integer.MAX_VALUE, NO_SEARCH);

        assertResults(records, TOTAL * 2, TOTAL * 2, TOTAL * 2, HASH, ASCENDING,
                report1, report2);

    }

    /**
     * Tests ReportManager#getOrderedRecordList.<br>
     * - Tests that the correct number of records are returned when DEVICE scope is specified.<br>
     * - Tests PATH order DESCENDING.<br>
     * - Tests that a limited number records for the device are returned.<br>
     */
    @Test
    public void testGetIMARecordsForDEVICELimited() {

        final String deviceId = "testDevice-" + UUID.randomUUID();
        final TestIMAMeasurementReport report1
                = new TestIMAMeasurementReport(deviceId);
        final TestIMAMeasurementReport report2
                = new TestIMAMeasurementReport(deviceId);

        FilteredRecordsList<IMAMeasurementRecord> records
                = reportManager.getOrderedRecordList(DEVICE, deviceId,
                        false, PATH, DESCENDING, START, LIMIT, NO_SEARCH);

        assertResults(records, LIMIT, TOTAL * 2, TOTAL * 2, PATH, DESCENDING,
                report1, report2);

    }

    /**
     * Tests ReportManager#getOrderedRecordList.<br>
     * - Tests that the correct number of records are returned when DEVICE scope is specified.<br>
     * - Tests PATH order DESCENDING.<br>
     * - Tests that all matching records for the device are returned.<br>
     * - Tests that only records matching the search criteria are returned.<br>
     */
    @Test
    public void testGetIMARecordsForDEVICEFilteredAll() {

        final String deviceId = "testDevice-" + UUID.randomUUID();
        final TestIMAMeasurementReport report1
                = new TestIMAMeasurementReport(deviceId, getExtraRecords());
        final TestIMAMeasurementReport report2
                = new TestIMAMeasurementReport(deviceId, getExtraRecords());

        FilteredRecordsList<IMAMeasurementRecord> records
                = reportManager.getOrderedRecordList(DEVICE, deviceId,
                        false, PATH, DESCENDING, START, Integer.MAX_VALUE,
                        INSENSITIVE_SEARCH);

        assertResults(records, TOTAL * 2, TOTAL * 4, TOTAL * 2, PATH, DESCENDING,
                report1, report2);

    }

    /**
     * Tests ReportManager#getOrderedRecordList.<br>
     * - Tests that the correct number of records are returned when DEVICE scope is specified.<br>
     * - Tests PATH order ASCENDING.<br>
     * - Tests that a limited number records for the device are returned.<br>
     * - Tests that only records matching the search criteria are returned.<br>
     */
    @Test
    public void testGetIMARecordsForDEVICEFilteredLimited() {

        final String deviceId = "testDevice-" + UUID.randomUUID();
        final TestIMAMeasurementReport report1
                = new TestIMAMeasurementReport(deviceId, getExtraRecords());
        final TestIMAMeasurementReport report2
                = new TestIMAMeasurementReport(deviceId, getExtraRecords());

        FilteredRecordsList<IMAMeasurementRecord> records
                = reportManager.getOrderedRecordList(DEVICE, deviceId,
                        false, PATH, ASCENDING, START, LIMIT, INSENSITIVE_SEARCH);

        assertResults(records, LIMIT, TOTAL * 4, TOTAL * 2, PATH, ASCENDING,
                report1, report2);

    }

    /**
     * Tests ReportManager#getOrderedRecordList.<br>
     * - Tests that the correct number of records are returned when DEVICE scope is specified.<br>
     * - Tests PATH order DESCENDING.<br>
     * - Tests that all records for the device are returned.<br>
     * - Tests that only records starting at the specified count are returned.<br>
     */
    @Test
    public void testIMARecordsForDEVICEStartingAt() {

        final String deviceId = "testDevice-" + UUID.randomUUID();
        final TestIMAMeasurementReport report
                = new TestIMAMeasurementReport(deviceId);

        FilteredRecordsList<IMAMeasurementRecord> records
                = reportManager.getOrderedRecordList(DEVICE, deviceId,
                        false, PATH, DESCENDING, 5, Integer.MAX_VALUE, NO_SEARCH);

        assertResults(records, 5, TOTAL, TOTAL, PATH, DESCENDING, report);
        int i = 4;
        for (IMAMeasurementRecord record : records) {
            Assert.assertEquals(record.getPath(), "/" + deviceId + "/" + i);
            i--;
        }

    }

    /**
     * Tests ReportManager#getOrderedRecordList.<br>
     * - Tests that only records since last full report are returned when DEVICE scope is specified
     * and sinceLastFullReport is specified.<br>
     * - Tests that all records for the device are returned.<br>
     * - Tests HASH order ASCENDING.<br>
     *
     * @throws java.lang.InterruptedException if thread sleep is interrupted.
     */
    @Test
    public void testGetIMARecordsForDEVICESinceLastReportAll() throws InterruptedException {

        final String deviceId = "testDevice-" + UUID.randomUUID();
        new TestIMAMeasurementReport(deviceId);
        new TestIMAMeasurementReport(deviceId, 1);
        Thread.sleep(2000);
        new TestIMAMeasurementReport(deviceId, 2);
        Thread.sleep(2000);
        new TestIMAMeasurementReport(deviceId, 3);
        Thread.sleep(2000);

        final TestIMAMeasurementReport report5
                = new TestIMAMeasurementReport(deviceId);
        final TestIMAMeasurementReport report6
                = new TestIMAMeasurementReport(deviceId, 1);
        Thread.sleep(2000);
        final TestIMAMeasurementReport report7
                = new TestIMAMeasurementReport(deviceId, 2);
        Thread.sleep(2000);
        final TestIMAMeasurementReport report8
                = new TestIMAMeasurementReport(deviceId, 3);

        FilteredRecordsList<IMAMeasurementRecord> records
                = reportManager.getOrderedRecordList(DEVICE, deviceId,
                        true, HASH, ASCENDING, START, Integer.MAX_VALUE, NO_SEARCH);

        assertResults(records, TOTAL * 4, TOTAL * 4, TOTAL * 4, HASH, ASCENDING,
                report5, report6, report7, report8);

    }

    /**
     * Tests ReportManager#getOrderedRecordList.<br>
     * - Tests that only records since last full report are returned when DEVICE scope is specified
     * and sinceLastFullReport is specified.<br>
     * - Tests that the limited number of records are returned.<br>
     * - Tests PATH order ASCENDING.<br>
     *
     * @throws java.lang.InterruptedException if thread sleep is interrupted.
     */
    @Test
    public void testGetIMARecordsForDEVICESinceLastReportLimited() throws InterruptedException {

        final String deviceId = "testDevice-" + UUID.randomUUID();
        new TestIMAMeasurementReport(deviceId);
        new TestIMAMeasurementReport(deviceId, 1);
        Thread.sleep(2000);
        new TestIMAMeasurementReport(deviceId, 2);
        Thread.sleep(2000);
        new TestIMAMeasurementReport(deviceId, 3);
        Thread.sleep(2000);

        final TestIMAMeasurementReport report5
                = new TestIMAMeasurementReport(deviceId);
        final TestIMAMeasurementReport report6
                = new TestIMAMeasurementReport(deviceId, 1);
        Thread.sleep(2000);
        final TestIMAMeasurementReport report7
                = new TestIMAMeasurementReport(deviceId, 2);
        Thread.sleep(2000);
        final TestIMAMeasurementReport report8
                = new TestIMAMeasurementReport(deviceId, 3);

        FilteredRecordsList<IMAMeasurementRecord> records
                = reportManager.getOrderedRecordList(DEVICE, deviceId,
                        true, PATH, ASCENDING, START, LIMIT, NO_SEARCH);

        assertResults(records, LIMIT, TOTAL * 4, TOTAL * 4, PATH, ASCENDING,
                report5, report6, report7, report8);

    }

    /**
     * Tests ReportManager#getOrderedRecordList.<br>
     * - Tests that only records since last full report are returned when DEVICE scope is specified
     * and sinceLastFullReport is specified.<br>
     * - Tests PATH order ASCENDING.<br>
     * - Tests that only matching records are returned.<br>
     *
     * @throws java.lang.InterruptedException if thread sleep is interrupted.
     */
    @Test
    public void testGetIMARecordsForDEVICESinceLastReportFiltered() throws InterruptedException {

        final String deviceId = "testDevice-" + UUID.randomUUID();
        new TestIMAMeasurementReport(deviceId, getExtraRecords());
        new TestIMAMeasurementReport(deviceId, 1);
        Thread.sleep(2000);
        new TestIMAMeasurementReport(deviceId, 2, getExtraRecords());
        Thread.sleep(2000);
        new TestIMAMeasurementReport(deviceId, 3);
        Thread.sleep(2000);

        final TestIMAMeasurementReport report5
                = new TestIMAMeasurementReport(deviceId, getExtraRecords());
        final TestIMAMeasurementReport report6
                = new TestIMAMeasurementReport(deviceId, 1);
        Thread.sleep(2000);
        final TestIMAMeasurementReport report7
                = new TestIMAMeasurementReport(deviceId, 2, getExtraRecords());
        Thread.sleep(2000);
        final TestIMAMeasurementReport report8
                = new TestIMAMeasurementReport(deviceId, 3);

        FilteredRecordsList<IMAMeasurementRecord> records
                = reportManager.getOrderedRecordList(DEVICE, deviceId,
                        true, PATH, ASCENDING, START, Integer.MAX_VALUE, SEARCH);

        assertResults(records, TOTAL * 2, TOTAL * 6, TOTAL * 2, PATH, ASCENDING,
                report5, report6, report7, report8);

    }

    /**
     * Tests ReportManager#getOrderedRecordList.<br>
     * - Tests that only records since last full report are returned when DEVICE scope is specified
     * and sinceLastFullReport is specified.<br>
     * - Tests PATH order ASCENDING.<br>
     * - Tests that only matching records are returned.<br>
     * - Tests that only records starting at the specified count are returned.<br>
     *
     * @throws java.lang.InterruptedException if thread sleep is interrupted.
     */
    @Test
    public void testIMARecordsForDEVICESinceLastReportStartingAt() throws InterruptedException {

        final String deviceId = "testDevice-" + UUID.randomUUID();
        new TestIMAMeasurementReport(deviceId, getExtraRecords());
        new TestIMAMeasurementReport(deviceId, 1);
        Thread.sleep(2000);
        new TestIMAMeasurementReport(deviceId, 2, getExtraRecords());
        Thread.sleep(2000);
        new TestIMAMeasurementReport(deviceId, 3);
        Thread.sleep(2000);

        final TestIMAMeasurementReport report5
                = new TestIMAMeasurementReport(deviceId, getExtraRecords());
        final TestIMAMeasurementReport report6
                = new TestIMAMeasurementReport(deviceId, 1);
        Thread.sleep(2000);
        final TestIMAMeasurementReport report7
                = new TestIMAMeasurementReport(deviceId, 2, getExtraRecords());
        Thread.sleep(2000);
        final TestIMAMeasurementReport report8
                = new TestIMAMeasurementReport(deviceId, 3);

        FilteredRecordsList<IMAMeasurementRecord> records
                = reportManager.getOrderedRecordList(DEVICE, deviceId,
                        true, PATH, ASCENDING, 12, Integer.MAX_VALUE, INSENSITIVE_SEARCH);

        assertResults(records, 8, TOTAL * 6, TOTAL * 2, PATH, ASCENDING,
                report5, report6, report7, report8);

        int i = 6;
        boolean inc = false;
        for (IMAMeasurementRecord record : records) {
            Assert.assertEquals(record.getPath(), "/" + deviceId + "/xyz/" + i);
            if (inc) {
                i++;
                inc = false;
            } else {
                inc = true;
            }
        }

    }

    /**
     * Tests ReportManager#getOrderedRecordList.<br>
     * - Tests that no records are returned when NONE scope is specified.<br>
     * - Tests PATH order ASCENDING.<br>
     */
    @Test
    public void zyTestGetIMARecordsForNONE() {

        final String deviceId = "testDevice-" + UUID.randomUUID();
        new TestIMAMeasurementReport(deviceId);

        FilteredRecordsList<IMAMeasurementRecord> records
                = reportManager.getOrderedRecordList(NONE, null,
                        false, PATH, ASCENDING, START, Integer.MAX_VALUE, NO_SEARCH);

        assertResults(records, 0, 0, 0, PATH, ASCENDING);

    }

}
