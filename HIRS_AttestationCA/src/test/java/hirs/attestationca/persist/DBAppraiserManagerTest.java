package hirs.attestationca.persist;

import hirs.appraiser.Appraiser;
import hirs.appraiser.IMAAppraiser;
import hirs.appraiser.TPMAppraiser;
import hirs.appraiser.TestAppraiser;
import hirs.attestationca.servicemanager.DBAppraiserManager;
import hirs.persist.AppraiserManager;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

/**
 * A test suite for testing the persistence of the Appraiser class.
 */
public class DBAppraiserManagerTest extends SpringPersistenceTest {
    private static final String APPRAISER_NAME = "Test Appraiser";
    private static final String APPRAISER_NAME_2 = "Another Test Appraiser";

    private AppraiserManager appraiserManager;

    /**
     * Sets up the appraiser manager that will manage persistence of the Appraisers.
     */
    @BeforeClass
    public void setup() {
        appraiserManager = new DBAppraiserManager(sessionFactory);
    }

    /**
     * Clears the DB between tests.
     */
    @AfterMethod
    public void resetTestState() {
        Session session = sessionFactory.unwrap(org.hibernate.Session.class);
        session.beginTransaction();

        CriteriaBuilder builder = session.getCriteriaBuilder();
        CriteriaQuery<Appraiser> criteriaQuery = builder.createQuery(Appraiser.class);
        Root<Appraiser> root = criteriaQuery.from(Appraiser.class);
        criteriaQuery.select(root);
        Query<Appraiser> query = session.createQuery(criteriaQuery);
        List<Appraiser> baselines = query.getResultList();
//        final List<?> baselines = session.createCriteria(Appraiser.class).list();
        for (Object o : baselines) {
            session.delete(o);
        }
        session.getTransaction().commit();
    }

    /**
     * Tests saving a new appraiser to the database.
     */
    @Test
    public void testSave() {
        TestAppraiser testAppraiser = new TestAppraiser(APPRAISER_NAME);
        Appraiser savedAppraiser = appraiserManager.saveAppraiser(testAppraiser);
        Assert.assertNotNull(savedAppraiser.getId());
    }

    /**
     * Tests retrieving a saved appraiser from the database.
     */
    @Test
    public void testGet() {
        TestAppraiser testAppraiser = new TestAppraiser(APPRAISER_NAME);
        Appraiser savedAppraiser = appraiserManager.saveAppraiser(testAppraiser);
        Appraiser retrievedAppraiser = appraiserManager.getAppraiser(APPRAISER_NAME);
        Assert.assertEquals(retrievedAppraiser, savedAppraiser);
        Assert.assertEquals(retrievedAppraiser.getId(), savedAppraiser.getId());
    }

    /**
     * Tests that attempting to retrieve a nonexistent appraiser from the database
     * returns a null value.
     */
    @Test
    public void testGetNonexistent() {
        Appraiser retrievedAppraiser = appraiserManager.getAppraiser(APPRAISER_NAME);
        Assert.assertNull(retrievedAppraiser);
    }

    /**
     * Tests that an appraiser can be deleted from the database.
     */
    @Test
    public void testDelete() {
        TestAppraiser testAppraiser = new TestAppraiser(APPRAISER_NAME);
        Appraiser savedAppraiser = appraiserManager.saveAppraiser(testAppraiser);
        Appraiser retrievedAppraiser = appraiserManager.getAppraiser(APPRAISER_NAME);
        Assert.assertEquals(retrievedAppraiser, savedAppraiser);
        Assert.assertTrue(appraiserManager.deleteAppraiser(savedAppraiser.getName()));
        Assert.assertNull(appraiserManager.getAppraiser(savedAppraiser.getName()));
    }

    /**
     * Tests that an appraiser can be updated.
     */
    @Test
    public void testUpdate() {
        TestAppraiser testAppraiser = new TestAppraiser(APPRAISER_NAME);
        appraiserManager.saveAppraiser(testAppraiser);
        Appraiser retrievedAppraiser = appraiserManager.getAppraiser(APPRAISER_NAME);
        retrievedAppraiser.setName(APPRAISER_NAME_2);
        appraiserManager.updateAppraiser(retrievedAppraiser);
        Assert.assertNull(appraiserManager.getAppraiser(APPRAISER_NAME));
        Assert.assertEquals(appraiserManager.getAppraiser(APPRAISER_NAME_2), retrievedAppraiser);
    }

    /**
     * Tests that a list of all appraisers in the system can be retrieved.
     */
    @Test
    public void testGetList() {
        TestAppraiser testAppraiser = new TestAppraiser(APPRAISER_NAME);
        Appraiser savedAppraiser = appraiserManager.saveAppraiser(testAppraiser);
        Assert.assertEquals(
                appraiserManager.getAppraiserList(),
                Collections.singleton(savedAppraiser)
        );

        TestAppraiser anotherTestAppraiser = new TestAppraiser(APPRAISER_NAME_2);
        Appraiser anotherSavedAppraiser = appraiserManager.saveAppraiser(anotherTestAppraiser);
        Assert.assertEquals(appraiserManager.getAppraiserList().size(), 2);
        Assert.assertTrue(
                appraiserManager.getAppraiserList().contains(savedAppraiser)
        );
        Assert.assertTrue(
                appraiserManager.getAppraiserList().contains(anotherSavedAppraiser)
        );
    }

    /**
     * Tests that lists of appraisers by their specific class can be retrieved.
     */
    @Test
    public void testGetListByClass() {
        TestAppraiser testAppraiser = new TestAppraiser(APPRAISER_NAME);
        TestAppraiser anotherTestAppraiser = new TestAppraiser(APPRAISER_NAME_2);

        Appraiser savedTestAppraiser = appraiserManager.saveAppraiser(testAppraiser);
        Appraiser anotherSavedTestAppraiser = appraiserManager.saveAppraiser(anotherTestAppraiser);

        Appraiser savedImaAppraiser = appraiserManager.saveAppraiser(new IMAAppraiser());
        Appraiser savedTpmAppraiser = appraiserManager.saveAppraiser(new TPMAppraiser());

        Assert.assertEquals(
                new HashSet<>(appraiserManager.getAppraiserList()),
                new HashSet<>(Arrays.asList(
                        savedTestAppraiser,
                        anotherSavedTestAppraiser,
                        savedImaAppraiser,
                        savedTpmAppraiser
                ))
        );

        Assert.assertEquals(
                new HashSet<>(appraiserManager.getAppraiserList(TestAppraiser.class)),
                new HashSet<>(Arrays.asList(
                        savedTestAppraiser,
                        anotherSavedTestAppraiser
                ))
        );

        Assert.assertEquals(
                new HashSet<>(appraiserManager.getAppraiserList(IMAAppraiser.class)),
                new HashSet<>(Arrays.asList(
                        savedImaAppraiser
                ))
        );

        Assert.assertEquals(
                new HashSet<>(appraiserManager.getAppraiserList(TPMAppraiser.class)),
                new HashSet<>(Arrays.asList(
                        savedTpmAppraiser
                ))
        );
    }
}
