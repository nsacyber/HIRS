package hirs.data.persist;

import hirs.data.persist.baseline.Baseline;
import java.io.Serializable;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;


/**
 * <code>BaselineTest</code> is a unit test class for the <code>Baseline</code>
 * class.
 */
public final class BaselineTest extends SpringPersistenceTest {
    private static final Logger LOGGER = LogManager.getLogger(BaselineTest.class);

    /**
     * Empty constructor that does nothing.
     */
    public BaselineTest() {
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
     * Tests <code>Baseline</code> constructor with valid name.
     */
    @Test
    public void testBaseline() {
        LOGGER.debug("testBaseline test started");
        final String name = "myBaseline";
        Baseline b = new TestBaseline(name);
        Assert.assertNotNull(b);
    }

    /**
     * Tests that <code>Baseline</code> constructor throws
     * <code>NullPointerException</code> with null name.
     */
    @Test(expectedExceptions = NullPointerException.class)
    public void testBaselineNullName() {
        LOGGER.debug("testBaselineNullName test started");
        final String name = null;
        new TestBaseline(name);
    }

    /**
     * Tests that <code>getName()</code> returns the name.
     */
    @Test
    public void testGetName() {
        LOGGER.debug("testGetName test started");
        final String name = "myBaseline";
        Baseline b = new TestBaseline(name);
        Assert.assertEquals(b.getName(), name);
    }

    /**
     * Tests that isSoftDeleted is false by default and true when set.
     */
    @Test
    public void testIsArchived() {
        Baseline b = new TestBaseline("test baseline");
        Assert.assertFalse(b.isArchived());
        b.archive();
        Assert.assertTrue(b.isArchived());
    }

    /**
     * Tests that two <code>Baseline</code>s are equal if they have the same
     * name.
     */
    @Test
    public void testEquals() {
        LOGGER.debug("testEquals test started");
        final String name1 = "myBaseline";
        Baseline b1 = new TestBaseline(name1);
        final String name2 = "myBaseline";
        Baseline b2 = new TestBaseline(name2);
        Assert.assertEquals(b1, b2);
        Assert.assertEquals(b2, b1);
        Assert.assertEquals(b1, b1);
        Assert.assertEquals(b2, b2);
    }

    /**
     * Tests that two <code>Baseline</code>s are not equal if the names are
     * different.
     */
    @Test
    public void testNotEquals() {
        LOGGER.debug("testNotEquals test started");
        final String name1 = "myBaseline1";
        Baseline b1 = new TestBaseline(name1);
        final String name2 = "myBaseline2";
        Baseline b2 = new TestBaseline(name2);
        Assert.assertNotEquals(b1, b2);
        Assert.assertNotEquals(b2, b1);
        Assert.assertEquals(b1, b1);
        Assert.assertEquals(b2, b2);
    }

    /**
     * Tests that hash code is that of the name.
     */
    @Test
    public void testHashCode() {
        LOGGER.debug("testHashCode test started");
        final String name = "myBaseline";
        Baseline b = new TestBaseline(name);
        Assert.assertEquals(name.hashCode(), b.hashCode());
    }

    /**
     * Tests that the hash code of two <code>Baseline</code>s is the same if the
     * names are the same.
     */
    @Test
    public void testHashCodeEquals() {
        LOGGER.debug("testHashCodeEquals test started");
        final String name1 = "myBaseline";
        Baseline b1 = new TestBaseline(name1);
        final String name2 = "myBaseline";
        Baseline b2 = new TestBaseline(name2);
        Assert.assertEquals(b1.hashCode(), b2.hashCode());
    }

    /**
     * Tests that the hash code of two <code>Baseline</code>s is different if
     * they have different names.
     */
    @Test
    public void testHashCodeNotEquals() {
        LOGGER.debug("testHashCodeNotEquals test started");
        final String name1 = "myBaseline1";
        Baseline b1 = new TestBaseline(name1);
        final String name2 = "myBaseline2";
        Baseline b2 = new TestBaseline(name2);
        Assert.assertNotEquals(b1.hashCode(), b2.hashCode());
    }

    /**
     * Tests that the name can be set for a <code>Baseline</code>.
     */
    @Test
    public void setName() {
        LOGGER.debug("setName test started");
        final String name = "myBaseline";
        Baseline b = new TestBaseline(name);
        final String newName = "newBaseline";
        b.setName(newName);
        Assert.assertEquals(newName, b.getName());
        Assert.assertEquals(newName.hashCode(), b.hashCode());
    }

    /**
     * Tests that a name cannot be null for a <code>Baseline</code>.
     */
    @Test(expectedExceptions = NullPointerException.class)
    public void setNameNull() {
        LOGGER.debug("setNameNull test started");
        final String name = "myBaseline";
        Baseline b = new TestBaseline(name);
        final String newName = null;
        b.setName(newName);
    }

    /**
     * Tests that a <code>Baseline</code> can be stored in the repository.
     */
    @Test
    public void saveBaseline() {
        LOGGER.debug("saveBaseline test started");
        Session session = sessionFactory.getCurrentSession();
        session.beginTransaction();
        final String name = "myBaseline";
        Baseline b = new TestBaseline(name);
        session.save(b);
        session.getTransaction().commit();
    }

    /**
     * Tests that a <code>Baseline</code> can be stored in the repository.
     */
    @Test
    public void saveGetBaseline() {
        LOGGER.debug("saveGetBaseline test started");
        Session session = sessionFactory.getCurrentSession();
        session.beginTransaction();
        final String name = "myBaseline";
        final Baseline b = new TestBaseline(name);
        Serializable bId = session.save(b);
        session.getTransaction().commit();

        session = sessionFactory.getCurrentSession();
        session.beginTransaction();
        final Baseline b2 = (Baseline) session.get(TestBaseline.class, bId);
        session.getTransaction().commit();

        Assert.assertEquals(b2, b);
        Assert.assertEquals(b2.getName(), name);
        Assert.assertEquals(b2.getId(), bId);
    }

    /**
     * Tests that a <code>Baseline</code> can be archived.
     */
    @Test
    public void testArchiveBaseline() {
        BaselineManager mgr = new DBBaselineManager(sessionFactory);
        LOGGER.debug("archive baseline test started");

        final Baseline baseline = new TestBaseline("Test Baseline");
        mgr.saveBaseline(baseline);
        mgr.archive(baseline.getName());
        Baseline retrievedBaseline = mgr.getBaseline(baseline.getName());
        Assert.assertTrue(retrievedBaseline.isArchived());
    }

    /**
     * Tests that a <code>Baseline</code> can be stored in the repository and
     * deleted.
     */
    @Test
    public void saveDeleteBaseline() {
        LOGGER.debug("saveGetBaseline test started");
        Session session = sessionFactory.getCurrentSession();
        session.beginTransaction();
        final String name = "myBaseline";
        final Baseline b = new TestBaseline(name);
        Serializable bId = session.save(b);
        session.getTransaction().commit();

        session = sessionFactory.getCurrentSession();
        session.beginTransaction();
        final Baseline b2 = (Baseline) session.get(TestBaseline.class, bId);
        session.delete(b2);
        session.getTransaction().commit();

        session = sessionFactory.getCurrentSession();
        session.beginTransaction();
        final Baseline b3 = (Baseline) session.get(TestBaseline.class, bId);
        session.getTransaction().commit();
        Assert.assertNull(b3);
    }

}
