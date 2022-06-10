package hirs.data.persist;

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
import java.io.Serializable;
import java.util.List;

import static org.apache.logging.log4j.LogManager.getLogger;

/**
 * This is an abstract class for testing the basic create, read, update, and
 * delete operations in Hibernate.
 *
 * @param <T> type of object to test with Hibernate
 */
public abstract class HibernateTest<T> extends SpringPersistenceTest {
    private static final Logger LOGGER = getLogger(HibernateTest.class);

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
     * the database by removing all <code>Policy</code> objects.
     */
    @AfterMethod
    public final void resetTestState() {
        LOGGER.debug("reset test state");
        for (Class<?> c : getCleanupClasses()) {
            removeAllOfClass(c);
        }
    }

    /**
     * Utility method to remove all instances of a certain class from the database.
     *
     * @param clazz the class to remove
     */
    @SuppressWarnings("unchecked")
    protected final void removeAllOfClass(final Class clazz) {
        Session session = sessionFactory.getCurrentSession();
        session.beginTransaction();
        CriteriaBuilder builder = session.getCriteriaBuilder();
        CriteriaQuery<T> criteriaQuery = builder.createQuery(clazz);
        Root<T> root = criteriaQuery.from(clazz);
        criteriaQuery.select(root);
        Query<T> query = session.createQuery(criteriaQuery);
        List<T> results = query.getResultList();
//        final List<?> objects = session.createCriteria(clazz).list();
        for (Object o : results) {
            LOGGER.debug("deleting object: {}", o);
            session.delete(o);
        }
        LOGGER.debug("all {} removed", clazz);
        session.getTransaction().commit();
    }

    /**
     * Tests that a <code>T</code> can be saved using Hibernate.
     */
    @Test
    public final void testSaveT() {
        LOGGER.debug("save {} test started", getDefaultClass());
        Session session = sessionFactory.getCurrentSession();
        session.beginTransaction();
        final T object = getDefault(session);
        session.save(object);
        session.getTransaction().commit();
    }

    /**
     * Tests that a <code>T</code> can be saved and retrieved. This saves a
     * <code>T</code> in the repo. Then a new session is created, and the
     * <code>T</code> is retrieved and its properties verified.
     */
    @Test
    public final void testGetT() {
        LOGGER.debug("get {} test started", getDefaultClass());
        Session session = sessionFactory.getCurrentSession();
        session.beginTransaction();
        final T object = getDefault(session);
        LOGGER.debug("saving {}", object.getClass());
        final Serializable id = session.save(object);
        session.getTransaction().commit();

        session = sessionFactory.getCurrentSession();
        session.beginTransaction();
        LOGGER.debug("getting {}", object.getClass());
        @SuppressWarnings("unchecked")
        final T testObject = (T) session.get(object.getClass(), id);
        session.getTransaction().commit();

        assertGetEqual(object, testObject);
    }

    /**
     * Tests that a <code>T</code> can be saved and then later updated. This
     * saves the <code>T</code>, retrieves it, modifies it, and then retrieves
     * it and verifies it.
     */
    @Test
    @SuppressWarnings("unchecked")
    public final void testUpdateT() {
        LOGGER.debug("update {} test started", getDefaultClass());
        Session session = sessionFactory.getCurrentSession();
        session.beginTransaction();
        final T object = getDefault(session);
        LOGGER.debug("saving {}", object.getClass());
        final Serializable id = session.save(object);
        session.getTransaction().commit();

        LOGGER.debug("updating {}", object.getClass());
        session = sessionFactory.getCurrentSession();
        session.beginTransaction();
        T testObject = (T) session.get(object.getClass(), id);
        update(testObject);
        session.update(testObject);
        session.getTransaction().commit();

        LOGGER.debug("getting {}", object.getClass());
        session = sessionFactory.getCurrentSession();
        session.beginTransaction();
        testObject = (T) session.get(object.getClass(), id);
        session.getTransaction().commit();

        assertUpdateEqual(object, testObject);
    }

    /**
     * Tests that a <code>T</code> can be stored in the repository and deleted.
     */
    @Test
    public final void testDeleteT() {
        LOGGER.debug("delete {} test started", getDefaultClass());
        Session session = sessionFactory.getCurrentSession();
        session.beginTransaction();
        final T object = getDefault(session);
        LOGGER.debug("saving {}", object.getClass());
        final Serializable id = session.save(object);
        session.getTransaction().commit();

        session = sessionFactory.getCurrentSession();
        session.beginTransaction();
        LOGGER.debug("getting {}", object.getClass());
        @SuppressWarnings("unchecked")
        final T t2 = (T) session.get(object.getClass(), id);
        session.delete(t2);
        session.getTransaction().commit();

        session = sessionFactory.getCurrentSession();
        session.beginTransaction();
        LOGGER.debug("getting {} again", object.getClass());
        @SuppressWarnings("unchecked")
        final T t3 = (T) session.get(object.getClass(), id);
        session.getTransaction().commit();
        Assert.assertNull(t3);
    }

    /**
     * Returns a default instance of <code>T</code> to save in the database.
     * This will be the initial object that is saved in the database.
     * <p>
     * The session is provided in case the default needs to save some dependent
     * objects first.
     *
     * @param session
     *            session
     * @return default object
     */
    protected abstract T getDefault(Session session);

    /**
     * Returns the class of T. This is used for logging.
     *
     * @return T class
     */
    protected abstract Class<?> getDefaultClass();

    /**
     * Update <code>object</code> for {@link #testUpdateT()} test.
     *
     * @param object object
     */
    protected abstract void update(T object);

    /**
     * Assert that the default object is equal to the retrieved object. This is
     * done during get T test.
     *
     * @param defaultObject
     *            default object
     * @param retrieved
     *            retrieved object
     */
    protected abstract void assertGetEqual(T defaultObject, T retrieved);

    /**
     * Assert that the default object is equal to the updated object. This is
     * done during update T test.
     *
     * @param defaultObject
     *            default object
     * @param update
     *            updated object
     */
    protected abstract void assertUpdateEqual(T defaultObject, T update);

    /**
     * Removes all instances of each type of the classes returned after each
     * test case.
     *
     * @return class instances to remove
     */
    protected abstract Class<?>[] getCleanupClasses();

    /**
     * Saves the given object to the database and then retrieves and returns it.
     *
     * @param object
     *          the object to save to the database
     * @return
     *          the retrieved copy of the object from the database
     */
    protected final T saveAndRetrieve(final T object) {
        Session session = sessionFactory.getCurrentSession();
        session.beginTransaction();
        Serializable id = session.save(object);
        session.getTransaction().commit();

        session = sessionFactory.getCurrentSession();
        session.beginTransaction();
        @SuppressWarnings("unchecked")
        final T testObject = (T) session.get(object.getClass(), id);
        session.getTransaction().commit();

        return testObject;
    }
}
