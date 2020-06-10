package hirs.persist;

import hirs.FilteredRecordsList;
import java.io.UnsupportedEncodingException;

import hirs.data.bean.SimpleBaselineBean;
import hirs.data.persist.baseline.Baseline;
import hirs.data.persist.baseline.SimpleImaBaseline;
import hirs.data.persist.Digest;
import hirs.data.persist.enums.DigestAlgorithm;
import hirs.data.persist.baseline.ImaBaseline;
import hirs.data.persist.baseline.IMABaselineRecord;
import hirs.data.persist.SpringPersistenceTest;
import hirs.data.persist.baseline.TPMBaseline;
import hirs.data.persist.baseline.TpmWhiteListBaseline;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import hirs.data.persist.TestBaseline;
import hirs.data.persist.TestBaseline2;
import hirs.ima.IMATestUtil;

import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * <code>DBBaselineManagerTest</code> is a unit test class for the
 * <code>DBBaselineManager</code> class.
 */
public final class DBBaselineManagerTest extends SpringPersistenceTest {
    private static final Logger LOGGER = LogManager.getLogger(DBBaselineManagerTest.class);
    private static final String BASELINE_NAME = "Test Baseline";

    /**
     * Creates a new <code>DBBaselineManagerTest</code>.
     */
    public DBBaselineManagerTest() {
        /* do nothing */
    }

    /**
     * Initializes a <code>SessionFactory</code>. The factory is used for an
     * in-memory database that is used for testing.
     */
    @BeforeClass
    public void setup() {
        LOGGER.debug("retrieving session factory");
    }

    /**
     * Closes the <code>SessionFactory</code> from setup.
     */
    @AfterClass
    public void tearDown() {
        LOGGER.debug("closing session factory");
    }

    /**
     * Resets the test state to a known good state. This currently only resets
     * the database by removing all <code>Baseline</code> objects.
     */
    @AfterMethod
    public void resetTestState() {
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
     * Tests that the <code>DBBaselineManager</code> can save a
     * <code>Baseline</code>.
     *
     * @throws BaselineManagerException if any unexpected errors occur
     */
    @Test
    public void testSave() throws BaselineManagerException {
        LOGGER.debug("testSave test started");
        final TestBaseline baseline = new TestBaseline(BASELINE_NAME);
        final BaselineManager mgr = new DBBaselineManager(sessionFactory);
        final TestBaseline b2 = (TestBaseline) mgr.saveBaseline(baseline);
        Assert.assertEquals(b2, baseline);
        Assert.assertTrue(isInDatabase(BASELINE_NAME, sessionFactory));
    }

    /**
     * Tests that the <code>DBBaselineManager</code> throws a
     * <code>BaselineManagerException</code> if a <code>Baseline</code> is saved
     * twice.
     *
     * @throws BaselineManagerException if any unexpected errors occur
     */
    @Test(expectedExceptions = BaselineManagerException.class)
    public void testSaveTwice() throws BaselineManagerException {
        LOGGER.debug("testSaveTwice test started");
        final TestBaseline baseline = new TestBaseline(BASELINE_NAME);
        final BaselineManager mgr = new DBBaselineManager(sessionFactory);
        final TestBaseline b2 = (TestBaseline) mgr.saveBaseline(baseline);
        mgr.saveBaseline(b2);
        Assert.fail("second save did not fail");
    }

    /**
     * Tests that the <code>DBBaselineManager</code> throws a
     * <code>BaselineManagerException</code> if a <code>Baseline</code> is saved
     * with the same name as an existing <code>Baseline</code>.
     *
     * @throws BaselineManagerException if any unexpected errors occur
     */
    @Test(expectedExceptions = BaselineManagerException.class)
    public void testSaveSameName() throws BaselineManagerException {
        LOGGER.debug("testSaveSameName test started");
        final TestBaseline baseline = new TestBaseline(BASELINE_NAME);
        final TestBaseline baseline2 = new TestBaseline(BASELINE_NAME);
        final BaselineManager mgr = new DBBaselineManager(sessionFactory);
        final TestBaseline b2 = (TestBaseline) mgr.saveBaseline(baseline);
        Assert.assertNotNull(b2);
        mgr.saveBaseline(baseline2);
        Assert.fail("second save did not fail");
    }

    /**
     * Tests that the <code>DBBaselineManager</code> throws a
     * <code>BaselineManagerException</code> if the baseline parameter is null.
     *
     * @throws BaselineManagerException if any unexpected errors occur
     */
    @Test(expectedExceptions = NullPointerException.class)
    public void testSaveNullBaseline() throws BaselineManagerException {
        LOGGER.debug("testSaveNullBaseline test started");
        final BaselineManager mgr = new DBBaselineManager(sessionFactory);
        mgr.saveBaseline(null);
        Assert.fail("save did not fail");
    }

    /**
     * Tests that the <code>DBBaselineManager</code> can update a
     * <code>Baseline</code>.
     *
     * @throws BaselineManagerException if any unexpected errors occur
     */
    @Test
    public void testUpdate() throws BaselineManagerException {
        LOGGER.debug("testUpdate test started");
        final String updatedName = "Updated Baseline";
        final BaselineManager mgr = new DBBaselineManager(sessionFactory);
        final Baseline baseline = createBaseline(mgr);
        baseline.setName(updatedName);
        mgr.updateBaseline(baseline);
        Assert.assertTrue(isInDatabase(updatedName, sessionFactory));
        Assert.assertFalse(isInDatabase(BASELINE_NAME, sessionFactory));
    }

    /**
     * Tests that the <code>DBBaselineManager</code> fails to update a
     * <code>Baseline</code> that has the same name as an existing
     * <code>Baseline</code>.
     *
     * @throws BaselineManagerException if any unexpected errors occur
     */
    @Test
    public void testUpdateSameName() throws BaselineManagerException {
        LOGGER.debug("testUpdateSameName test started");
        final String name1 = "Test Baseline 1";
        final String name2 = "Test Baseline 2";
        final BaselineManager mgr = new DBBaselineManager(sessionFactory);
        createBaseline(mgr, name1);
        final Baseline b2 = createBaseline(mgr, name2);
        b2.setName(name1);
        BaselineManagerException expected = null;
        try {
            mgr.updateBaseline(b2);
        } catch (BaselineManagerException e) {
            expected = e;
        }
        Assert.assertNotNull(expected);
        Assert.assertTrue(isInDatabase(name1, sessionFactory));
        Assert.assertTrue(isInDatabase(name2, sessionFactory));
    }

    /**
     * Tests that the <code>DBBaselineManager</code> can update an empty ImaBaseline with new
     * records.
     *
     * @throws BaselineManagerException if any unexpected errors occur
     * @throws UnsupportedEncodingException if the platform does not support UTF-8 encoding
     */
    @Test
    public void testAddIMABaselineRecordsToEmptyBaseline() throws BaselineManagerException,
            UnsupportedEncodingException {
        final BaselineManager mgr = new DBBaselineManager(sessionFactory);
        SimpleImaBaseline imaBaseline = new SimpleImaBaseline("Test Baseline 1");
        SimpleImaBaseline savedBaseline = (SimpleImaBaseline) mgr.saveBaseline(imaBaseline);

        String path = "/test.path";
        Digest digest = new Digest(DigestAlgorithm.SHA1, "c068d837b672cb3f80ac".getBytes("UTF-8"));
        savedBaseline.addToBaseline(new IMABaselineRecord(path, digest));
        mgr.updateBaseline(savedBaseline);

        SimpleImaBaseline retrievedBaseline =
                (SimpleImaBaseline) mgr.getCompleteBaseline(savedBaseline.getName());
        Assert.assertEquals(retrievedBaseline.getBaselineRecords().size(), 1);
        IMABaselineRecord firstRecord = new ArrayList<>(retrievedBaseline.getBaselineRecords())
                .get(0);
        Assert.assertEquals(firstRecord.getPath(), path);
        Assert.assertEquals(firstRecord.getHash(), digest);
    }

    /**
     * Tests that the <code>DBBaselineManager</code> can update an ImaBaseline, which already has
     * existing records, with new ones.
     *
     * @throws BaselineManagerException if any unexpected errors occur
     * @throws UnsupportedEncodingException if the platform does not support UTF-8 encoding
     */
    @Test
    public void testAddIMABaselineRecordsToNonEmptyBaseline() throws BaselineManagerException,
            UnsupportedEncodingException {
        final BaselineManager mgr = new DBBaselineManager(sessionFactory);
        SimpleImaBaseline imaBaseline = new SimpleImaBaseline("Test Baseline 1");
        String path = "/test.path";
        Digest digest = new Digest(DigestAlgorithm.SHA1, "c068d837b672cb3f80ac".getBytes("UTF-8"));
        imaBaseline.addToBaseline(new IMABaselineRecord(path, digest));
        ImaBaseline savedBaseline = (ImaBaseline) mgr.saveBaseline(imaBaseline);

        SimpleImaBaseline retrievedBaseline = (SimpleImaBaseline)
                mgr.getCompleteBaseline(savedBaseline.getName());
        retrievedBaseline.addToBaseline(new IMABaselineRecord("/another/test.path", digest));
        mgr.updateBaseline(retrievedBaseline);

        retrievedBaseline =
                (SimpleImaBaseline) mgr.getCompleteBaseline(retrievedBaseline.getName());
        Assert.assertEquals(retrievedBaseline.getBaselineRecords().size(), 2);
        for (IMABaselineRecord r : retrievedBaseline.getBaselineRecords()) {
            Assert.assertEquals(r.getHash(), digest);
        }
    }

    /**
     * Tests that the <code>DBBaselineManager</code> throws a
     * <code>BaselineManagerException</code> if the baseline parameter is null.
     *
     * @throws BaselineManagerException if any unexpected errors occur
     */
    @Test(expectedExceptions = NullPointerException.class)
    public void testUpdateNullBaseline() throws BaselineManagerException {
        LOGGER.debug("testUpdateNullBaseline test started");
        final BaselineManager mgr = new DBBaselineManager(sessionFactory);
        mgr.updateBaseline(null);
        Assert.fail("save did not fail");
    }

    /**
     * Tests that the <code>DBBaselineManager</code> can get a
     * <code>Baseline</code>.
     *
     * @throws BaselineManagerException if any unexpected errors occur
     */
    @Test
    public void testGet() throws BaselineManagerException {
        LOGGER.debug("testGet test started");
        final BaselineManager mgr = new DBBaselineManager(sessionFactory);
        Assert.assertNull(mgr.getBaseline(BASELINE_NAME));
        final Baseline baseline = createBaseline(mgr);
        final Baseline getBaseline = mgr.getBaseline(BASELINE_NAME);
        Assert.assertNotNull(getBaseline);
        Assert.assertEquals(getBaseline, baseline);
        Assert.assertEquals(getBaseline.getId(), baseline.getId());
    }

    /**
     * Tests that the <code>DBBaselineManager</code> returns null when an
     * unknown <code>Baseline</code> is searched for.
     *
     * @throws BaselineManagerException if any unexpected errors occur
     */
    @Test
    public void testGetUnknown() throws BaselineManagerException {
        LOGGER.debug("testGetUnknown test started");
        final BaselineManager mgr = new DBBaselineManager(sessionFactory);
        Assert.assertNull(mgr.getBaseline("Some Unknown Baseline"));
    }

    /**
     * Tests that the <code>DBBaselineManager</code> returns null when the name
     * is set to null.
     *
     * @throws BaselineManagerException if any unexpected errors occur
     */
    @Test
    public void testGetNull() throws BaselineManagerException {
        LOGGER.debug("testGetUnknown test started");
        final BaselineManager mgr = new DBBaselineManager(sessionFactory);
        Assert.assertNull(mgr.getBaseline(null));
    }

    /**
     * Tests that a list of <code>Baseline</code> names can be retrieved from
     * the repository.
     *
     * @throws BaselineManagerException
     *             if any unexpected errors occur
     */
    @Test
    public void testGetBaselineList() throws BaselineManagerException {
        LOGGER.debug("testGetBaselineList test started");
        final BaselineManager mgr = new DBBaselineManager(sessionFactory);
        final String[] names = {"Baseline1", "Baseline2", "Baseline3"};
        for (String name : names) {
            createBaseline(mgr, name);
        }
        final List<Baseline> baselines = mgr.getBaselineList(Baseline.class);
        Assert.assertEquals(baselines.size(), names.length);
        for (Baseline baseline : baselines) {
            Assert.assertTrue(Arrays.asList(names)
                    .contains(baseline.getName()));
        }
    }

    /**
     * Tests that a list of <code>Baseline</code> names can be retrieved from
     * the repository with null <code>Class</code> given.
     *
     * @throws BaselineManagerException if any unexpected errors occur
     */
    @Test
    public void testGetBaselineListNullClass() throws BaselineManagerException {
        LOGGER.debug("testGetBaselineList test started");
        final BaselineManager mgr = new DBBaselineManager(sessionFactory);
        final String[] names = {"Baseline1", "Baseline2", "Baseline3"};
        for (String name : names) {
            createBaseline(mgr, name);
        }
        final List<Baseline> baselines = mgr.getBaselineList(null);
        Assert.assertEquals(baselines.size(), names.length);
        for (Baseline baseline : baselines) {
            Assert.assertTrue(Arrays.asList(names)
                    .contains(baseline.getName()));
        }
    }

    /**
     * Tests that a list of <code>Baseline</code> names can be retrieved from
     * the repository when a subclass of <code>Baseline</code> is used.
     *
     * @throws BaselineManagerException if any unexpected errors occur
     */
    @Test
    public void testGetBaselineListDifferentClass()
            throws BaselineManagerException {
        LOGGER.debug("testGetBaselineListDifferentClass test started");
        final BaselineManager mgr = new DBBaselineManager(sessionFactory);
        final String[] names = {"Baseline1", "Baseline2", "Baseline3"};
        for (String name : names) {
            createBaseline(mgr, name);
        }
        final List<Baseline> baselines =
                mgr.getBaselineList(TestBaseline.class);
        Assert.assertEquals(baselines.size(), names.length);
        for (Baseline baseline : baselines) {
            Assert.assertTrue(Arrays.asList(names)
                    .contains(baseline.getName()));
        }
    }

    /**
     * Tests that a list of <code>Baseline</code> names can be retrieved from
     * the repository when a subclass of <code>Baseline</code> is used and the
     * list is empty because all of the stored <code>Baseline</code>s are of a
     * different class.
     *
     * @throws BaselineManagerException if any unexpected errors occur
     */
    @Test
    public void testGetBaselineListDifferentClassNoReturn()
            throws BaselineManagerException {
        LOGGER.debug("testGetBaselineListDifferentClassNoReturn test started");
        final BaselineManager mgr = new DBBaselineManager(sessionFactory);
        final String[] names = {"Baseline1", "Baseline2", "Baseline3"};
        for (String name : names) {
            createBaseline(mgr, name);
        }
        final List<Baseline> baselines =
                mgr.getBaselineList(TestBaseline2.class);
        Assert.assertEquals(baselines.size(), 0);
    }

    /**
     * Tests that a list of <code>Baseline</code> names can be retrieved from
     * the repository based on the ID column and in ascending order.
     *
     * @throws BaselineManagerException if any unexpected errors occur
     */
    @Test
    public void testGetOrderedListAsc() throws BaselineManagerException {
        LOGGER.debug("testGetOrderedListAsc test started");
        final BaselineManager mgr = new DBBaselineManager(sessionFactory);
        final String[] names = {"BaseOne", "BaseTwo", "BaseThree"};
        final Baseline[] expectedBaseline = new Baseline[names.length];
        final Map<String, Boolean> searchColumns = new HashMap<>();
        searchColumns.put("name", Boolean.TRUE);
        for (int i = 0; i < names.length; ++i) {
            expectedBaseline[i] = createBaseline(mgr, names[i]);
        }
        final FilteredRecordsList<Baseline> baseline = mgr.getOrderedBaselineList(
                "id", true, 0, 3, "", searchColumns);
        Assert.assertEquals(baseline.size(), expectedBaseline.length);
        Assert.assertTrue(baseline.get(0).getId().toString()
               .compareTo(baseline.get(1).getId().toString()) <= 0);
        Assert.assertTrue(baseline.get(1).getId().toString()
               .compareTo(baseline.get(2).getId().toString()) <= 0);
    }

    /**
     * Tests that a list of <code>Baseline</code>s can be retrieved from
     * the repository based on the ID column and in descending order.
     *
     * @throws BaselineManagerException if any unexpected errors occur
     */
    @Test
    public void testGetOrderedListDesc() throws BaselineManagerException {
        LOGGER.debug("testGetOrderedListDesc test started");
        final BaselineManager mgr = new DBBaselineManager(sessionFactory);
        final String[] names = {"BaseOne", "BaseTwo", "BaseThree"};
        final Baseline[] expectedBaseline = new Baseline[names.length];
        final Map<String, Boolean> searchColumns = new HashMap<>();
        searchColumns.put("name", Boolean.TRUE);
        for (int i = 0; i < names.length; ++i) {
            expectedBaseline[i] = createBaseline(mgr, names[i]);
        }
        final FilteredRecordsList<Baseline> baseline = mgr.getOrderedBaselineList(
                "id", false, 0, 3, "", searchColumns);
        Assert.assertEquals(baseline.size(), expectedBaseline.length);
        Assert.assertTrue(baseline.get(0).getId().toString()
               .compareTo(baseline.get(1).getId().toString()) >= 0);
        Assert.assertTrue(baseline.get(1).getId().toString()
               .compareTo(baseline.get(2).getId().toString()) >= 0);
    }

    /**
     * Tests that a list of <code>Baseline</code>s can be retrieved from the
     * repository based on searching through the strings in the displayed
     * columns.
     *
     * @throws BaselineManagerException if any unexpected errors occur
     */
    @Test
    public void testGetOrderedListSearch() throws BaselineManagerException {
        LOGGER.debug("testGetOrderedListSearch test started");
        final BaselineManager mgr = new DBBaselineManager(sessionFactory);
        final String[] names = {"BaseOne", "BaseTwo", "BaseThree"};
        final Map<String, Boolean> searchColumns = new HashMap<>();
        searchColumns.put("name", Boolean.TRUE);
        for (int i = 0; i < names.length; ++i) {
            createBaseline(mgr, names[i]);
        }
        final FilteredRecordsList<Baseline> baseline = mgr.getOrderedBaselineList(
                "id", false, 0, 3, "Three", searchColumns);
        Assert.assertEquals(baseline.size(), 1);
        Assert.assertEquals(baseline.get(0).getName(), "BaseThree");
    }

    /**
     * Tests that a list of <code>Baseline</code>s can be retrieved from the
     * repository based on searching through the strings in multiple
     * columns.
     *
     * @throws BaselineManagerException if any unexpected errors occur
     */
    @Test
    public void testGetOrderedListMultiSearch() throws BaselineManagerException {
        LOGGER.debug("testGetOrderedListSearch test started");
        final BaselineManager mgr = new DBBaselineManager(sessionFactory);
        final String[] names = {"BaseOne", "BaseTwo", "BaseThree"};
        final Map<String, Boolean> searchColumns = new HashMap<>();
        searchColumns.put("name", Boolean.TRUE);
        searchColumns.put("description", Boolean.TRUE);
        for (int i = 0; i < names.length; ++i) {
            createBaseline(mgr, names[i]);
        }
        final FilteredRecordsList<Baseline> baseline = mgr.getOrderedBaselineList(
                "id", false, 0, 3, "desc Three", searchColumns);
        Assert.assertEquals(baseline.size(), 1);
        Assert.assertEquals(baseline.get(0).getName(), "BaseThree");
    }

    /**
     * Tests that a list of <code>Baseline</code>s can be retrieved from the
     * repository based on paging the results by telling the query to start
     * at record x and a set number of records.
     *
     * @throws BaselineManagerException if any unexpected errors occur
     */
    @Test
    public void testGetOrderedListPaging() throws BaselineManagerException {
        LOGGER.debug("testGetOrderedListPaging test started");
        final BaselineManager mgr = new DBBaselineManager(sessionFactory);
        final String[] names = {"BaseOne", "BaseTwo", "BaseThree"};
        final Map<String, Boolean> searchColumns = new HashMap<>();
        searchColumns.put("name", Boolean.TRUE);
        for (int i = 0; i < names.length; ++i) {
            createBaseline(mgr, names[i]);
        }
        final FilteredRecordsList<Baseline> baseline = mgr.getOrderedBaselineList(
                "id", false, 2, 3, "", searchColumns);
        Assert.assertEquals(baseline.size(), 1);
    }

    /**
     * Tests that the correct list of <code>Baseline</code>s can be retrieved from the database when
     * some have been archived and others have not.
     *
     * @throws BaselineManagerException if any unexpected errors occur
     */
    @Test
    public void testGetOrderedListAfterArchive() throws BaselineManagerException {
        LOGGER.debug("testGetOrderedListAfterArchive started");
        final BaselineManager mgr = new DBBaselineManager(sessionFactory);
        Session session = sessionFactory.openSession();

        final String[] archivedImaNames = {"Base1", "Base2"};
        final String[] nonArchivedImaNames = {"Base3", "Base4", "Base5"};
        final String[] archivedTpmNames = {"Base6", "Base7", "Base8", "Base9"};
        final String[] nonArchivedTpmNames = {"Base10", "Base11", "Base12", "Base13", "Base14"};
        final Baseline[] archivedImaBaselines = new Baseline[archivedImaNames.length];
        final Baseline[] nonArchivedImaBaselines = new Baseline[nonArchivedImaNames.length];
        final Baseline[] archivedTpmBaselines = new Baseline[archivedTpmNames.length];
        final Baseline[] nonArchivedTpmBaselines = new Baseline[nonArchivedTpmNames.length];
        final Baseline[][] baselineArrays = {archivedImaBaselines, nonArchivedImaBaselines,
                archivedTpmBaselines, nonArchivedTpmBaselines};

        final Map<String, Boolean> searchColumns = new HashMap<>();
        searchColumns.put("name", Boolean.TRUE);

        for (int i = 0; i < baselineArrays.length; ++i) {
            for (int j = 0; j < baselineArrays[i].length; ++j) {
                switch (i) {
                case 0:
                    session.beginTransaction();
                    Baseline imaBaseline = IMATestUtil.getVerifyBaseline();
                    imaBaseline.setName(archivedImaNames[j]);
                    session.save(imaBaseline);
                    session.getTransaction().commit();
                    mgr.archive(imaBaseline.getName());
                    break;
                case 1:
                    session.beginTransaction();
                    imaBaseline = IMATestUtil.getVerifyBaseline();
                    imaBaseline.setName(nonArchivedImaNames[j]);
                    baselineArrays[i][j] = imaBaseline;
                    session.save(imaBaseline);
                    session.getTransaction().commit();
                    break;
                case 2:
                    session.beginTransaction();
                    TPMBaseline tpmBaseline = new TpmWhiteListBaseline(archivedTpmNames[j]);
                    tpmBaseline.setDescription("test description");
                    session.save(tpmBaseline);
                    session.getTransaction().commit();
                    mgr.archive(tpmBaseline.getName());
                    break;
                case 3:
                    session.beginTransaction();
                    tpmBaseline = new TpmWhiteListBaseline(nonArchivedTpmNames[j]);
                    tpmBaseline.setDescription("test description");
                    baselineArrays[i][j] = tpmBaseline;
                    session.save(tpmBaseline);
                    session.getTransaction().commit();
                    break;
                default:
                    throw new IllegalStateException("internal baseline manager test switch error");
                }
            }
        }

        final FilteredRecordsList<Baseline> retrievedBaselines = mgr.getOrderedBaselineList(
                "id", true, 0, 100, "", searchColumns);
        Assert.assertEquals(retrievedBaselines.size(), nonArchivedImaBaselines.length
                + nonArchivedTpmBaselines.length);

        final Baseline[][] nonDeletedBaselineArrays = {nonArchivedImaBaselines,
                nonArchivedTpmBaselines};

        for (int i = 0; i < nonDeletedBaselineArrays.length; ++i) {
            for (int j = 0; j < nonDeletedBaselineArrays[i].length; ++j) {
                Assert.assertTrue(retrievedBaselines.contains(nonDeletedBaselineArrays[i][j]));
            }
        }
    }

    /**
     * Tests that a list of <code>Baseline</code> names can be retrieved from
     * the repository based on the ID column and in ascending order.
     *
     * @throws BaselineManagerException if any unexpected errors occur
     */
    @Test
    public void testGetOrderedListWithoutRecordsAsc() throws BaselineManagerException {
        LOGGER.debug("testGetOrderedListWithoutRecordsAsc test started");
        final BaselineManager mgr = new DBBaselineManager(sessionFactory);
        final String[] names = {"BaseOne", "BaseTwo", "BaseThree"};
        final Baseline[] expectedBaseline = new Baseline[names.length];
        final Map<String, Boolean> searchColumns = new HashMap<>();
        searchColumns.put("name", Boolean.TRUE);
        for (int i = 0; i < names.length; ++i) {
            expectedBaseline[i] = createBaseline(mgr, names[i]);
        }
        final FilteredRecordsList<SimpleBaselineBean> baseline =
            mgr.getOrderedBaselineListWithoutRecords("id", true, 0, 3, "", searchColumns);
        Assert.assertEquals(baseline.size(), expectedBaseline.length);
        Assert.assertTrue(baseline.get(0).getId().toString()
               .compareTo(baseline.get(1).getId().toString()) <= 0);
        Assert.assertTrue(baseline.get(1).getId().toString()
               .compareTo(baseline.get(2).getId().toString()) <= 0);
    }

    /**
     * Tests that a list of <code>Baseline</code>s can be retrieved from the
     * repository based on searching through the strings in multiple
     * columns.
     *
     * @throws BaselineManagerException if any unexpected errors occur
     */
    @Test
    public void testGetOrderedListWithoutRecordsMultiSearch() throws BaselineManagerException {
        LOGGER.debug("testGetOrderedListWithoutRecordsSearch test started");
        final BaselineManager mgr = new DBBaselineManager(sessionFactory);
        final String[] names = {"BaseOne", "BaseTwo", "BaseThree"};
        final Map<String, Boolean> searchColumns = new HashMap<>();
        searchColumns.put("name", Boolean.TRUE);
        searchColumns.put("description", Boolean.TRUE);
        for (int i = 0; i < names.length; ++i) {
            createBaseline(mgr, names[i]);
        }
        final FilteredRecordsList<SimpleBaselineBean> baseline =
          mgr.getOrderedBaselineListWithoutRecords("id", false, 0, 3, "desc Three", searchColumns);
        Assert.assertEquals(baseline.size(), 1);
        Assert.assertEquals(baseline.get(0).getName(), "BaseThree");
    }

    /**
     * Tests that a list of <code>Baseline</code>s can be retrieved from the
     * repository based on paging the results by telling the query to start
     * at record x and a set number of records.
     *
     * @throws BaselineManagerException if any unexpected errors occur
     */
    @Test
    public void testGetOrderedListWithoutRecordsPaging() throws BaselineManagerException {
        LOGGER.debug("testGetOrderedListWithoutRecordsPaging test started");
        final BaselineManager mgr = new DBBaselineManager(sessionFactory);
        final String[] names = {"BaseOne", "BaseTwo", "BaseThree"};
        final Map<String, Boolean> searchColumns = new HashMap<>();
        searchColumns.put("name", Boolean.TRUE);
        for (int i = 0; i < names.length; ++i) {
            createBaseline(mgr, names[i]);
        }
        final FilteredRecordsList<SimpleBaselineBean> baseline =
            mgr.getOrderedBaselineListWithoutRecords("id", false, 2, 3, "", searchColumns);
        Assert.assertEquals(baseline.size(), 1);
    }

    /**
     * Tests that the <code>DBBaselineManager</code> can archive a <code>Baseline</code>.
     *
     * @throws BaselineManagerException if any unexpected errors occur
     */
    @Test
    public void testArchive() throws BaselineManagerException {
        LOGGER.debug("testArchive test started");

        final BaselineManager mgr = new DBBaselineManager(sessionFactory);
        String name = "test simple ima baseline";
        Baseline imaBaseline = IMATestUtil.getVerifyBaseline();
        imaBaseline.setName(name);
        Assert.assertNull(mgr.getBaseline(BASELINE_NAME));

        Session session = sessionFactory.getCurrentSession();
        session.beginTransaction();
        session.save(imaBaseline);
        session.getTransaction().commit();

        Assert.assertNotNull(mgr.getBaseline(name));
        Assert.assertFalse(mgr.getBaseline(name).isArchived());
        boolean archived = mgr.archive(name);
        Assert.assertTrue(archived);
        Assert.assertNotNull(mgr.getBaseline(name));
        Assert.assertTrue(mgr.getBaseline(name).isArchived());
    }

    /**
     * Tests that the <code>DBBaselineManager</code> returns false when an
     * unknown <code>Baseline</code> name is provided.
     *
     * @throws BaselineManagerException if any unexpected errors occur
     */
    @Test
    public void testArchiveUnknown() throws BaselineManagerException {
        LOGGER.debug("testDelete test started");
        final String name = "Some unknown baseline";
        final BaselineManager mgr = new DBBaselineManager(sessionFactory);
        boolean deleted = mgr.archive(name);
        Assert.assertFalse(deleted);
        Assert.assertNull(mgr.getBaseline(BASELINE_NAME));
    }

    /**
     * Tests that the <code>DBBaselineManager</code> returns false when null is
     * provided for the name.
     *
     * @throws BaselineManagerException if any unexpected errors occur
     */
    @Test
    public void testArchiveNull() throws BaselineManagerException {
        LOGGER.debug("testDelete test started");
        final String name = null;
        final BaselineManager mgr = new DBBaselineManager(sessionFactory);
        boolean deleted = mgr.archive(name);
        Assert.assertFalse(deleted);
    }

    private Baseline createBaseline(final BaselineManager mgr)
            throws BaselineManagerException {
        return createBaseline(mgr, null);
    }

    private Baseline createBaseline(final BaselineManager mgr,
            final String name) throws BaselineManagerException {
        LOGGER.debug("creating baseline in db");
        String baselineName = name;
        if (name == null) {
            baselineName = BASELINE_NAME;
        }
        final TestBaseline baseline = new TestBaseline(baselineName);
        baseline.setDescription("my desc");
        return mgr.saveBaseline(baseline);
    }

    /**
     * Returns a boolean indicating if a <code>Baseline</code> with the given name was found in the
     * database.
     *
     * @param name
     *            Baseline name to search for
     * @param sessionFactory
     *            SessionFactory
     * @return boolean indicating if the Baseline was found
     */
    public static boolean isInDatabase(final String name, final SessionFactory sessionFactory) {
        LOGGER.debug("checking if baseline {} is in database", name);
        Baseline baseline = null;
        Transaction tx = null;
        Session session = sessionFactory.getCurrentSession();
        try {
            LOGGER.debug("retrieving baseline");
            tx = session.beginTransaction();
            baseline = (Baseline) session.createCriteria(Baseline.class).
                    add(Restrictions.eq("name", name)).uniqueResult();
            session.getTransaction().commit();
        } catch (Exception e) {
            final String msg = "unable to retrieve baseline";
            LOGGER.error(msg, e);
            if (tx != null) {
                LOGGER.debug("rolling back transaction");
                tx.rollback();
            }
            throw e;
        }
        return baseline != null;
    }
}
