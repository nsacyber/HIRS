package hirs.persist;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.List;

/**
 * This is a utility class with common methods for DB*ManagerTest classes.
 */
public final class DBUtility {
    /**
     * Hibernate configuration for in-memory database.
     */

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * This class should never be constructed.
     */
    private DBUtility() {

    }

    /**
     * Removes all instances of type <code>clazz</code> from the database. The
     * {@link #setup()} method should have been called before this. Otherwise an
     * error will occur.
     *
     * @param sessionFactory session factory to use to access the database
     * @param clazz class instances to remove
     */
    @SuppressWarnings("unchecked")
    public static void removeAllInstances(
            final SessionFactory sessionFactory,
            final Class clazz) {
        LOGGER.debug("deleting all {} instances", clazz.toString());
        Session session = sessionFactory.getCurrentSession();
        session.beginTransaction();
        CriteriaBuilder builder = session.getCriteriaBuilder();
        CriteriaQuery<Class> criteriaQuery = builder.createQuery(clazz);
        Root<Class> root = criteriaQuery.from(clazz);
        criteriaQuery.select(root);
        Query<Class> query = session.createQuery(criteriaQuery);
        List<Class> instances = query.getResultList();
//        final List<?> instances = session.createCriteria(clazz).list();
        for (Object o : instances) {
            LOGGER.debug("deleting {}", o);
            session.delete(o);
        }
        LOGGER.debug("all {} instances removed", clazz.toString());
        session.getTransaction().commit();
    }

    /**
     * Checks if an instance of <code>clazz</code> is in the database and has
     * the name <code>name</code>.
     *
     * @param sessionFactory session factory to use to access the database
     * @param clazz class
     * @param name name
     * @return true if in database, otherwise false
     */
    public static boolean isInDatabase(
            final SessionFactory sessionFactory,
            final Class<?> clazz,
            final String name) {
        LOGGER.debug("checking if {} is in db", clazz.toString(), name);
        return isInDatabase(sessionFactory, clazz, "name", name);
    }

    /**
     * Checks if an instance of <code>clazz</code> is in the database and has
     * the property <code>propName</code> set to <code>value</code>.
     *
     * @param sessionFactory session factory to use to access the database
     * @param clazz class
     * @param propName property name
     * @param value value
     * @return true if in database, otherwise false
     */
    @SuppressWarnings("unchecked")
    public static boolean isInDatabase(
            final SessionFactory sessionFactory,
            final Class clazz,
            final String propName, final Object value) {
        LOGGER.debug("checking if {} with property {} set to {}",
                clazz.toString(), propName, value);
        Object search = null;
        Transaction tx = null;
        Session session = sessionFactory.getCurrentSession();
        try {
            LOGGER.debug("retrieving");
            tx = session.beginTransaction();
            CriteriaBuilder builder = session.getCriteriaBuilder();
            CriteriaQuery<Class> criteriaQuery = builder.createQuery(clazz);
            Root<Class> root = criteriaQuery.from(clazz);
            criteriaQuery.select(root);
            Query<Class> query = session.createQuery(criteriaQuery);
            Class instances = query.getSingleResult();
            search = instances;
//            search = session.createCriteria(clazz)
//                    .add(Restrictions.eq(propName, value)).uniqueResult();
            session.getTransaction().commit();
        } catch (Exception e) {
            final String msg = "unable to retrieve";
            LOGGER.error(msg, e);
            if (tx != null) {
                LOGGER.debug("rolling back transaction");
                tx.rollback();
            }
            throw e;
        }
        return search != null;
    }

    /**
     * Returns a count of how many instances of <code>clazz</code> are in the
     * database.
     *
     * @param sessionFactory session factory to use to access the database
     * @param clazz clazz
     * @return number of instance of <code>clazz</code> in the database
     */
    @SuppressWarnings("unchecked")
    public static int getCount(final SessionFactory sessionFactory, final Class clazz) {
        LOGGER.debug("counting number of instances of {} in db", clazz);
        int count = 0;
        Transaction tx = null;
        Session session = sessionFactory.getCurrentSession();
        try {
            tx = session.beginTransaction();
            CriteriaBuilder builder = session.getCriteriaBuilder();
            CriteriaQuery<Class> criteriaQuery = builder.createQuery(clazz);
            Root<Class> root = criteriaQuery.from(clazz);
            criteriaQuery.select(root);
            Query<Class> query = session.createQuery(criteriaQuery);
            List<Class> instances = query.getResultList();
//            List<?> found = session.createCriteria(clazz).list();
            count = instances.size();
            session.getTransaction().commit();
        } catch (Exception e) {
            final String msg = "unable to retrieve";
            LOGGER.error(msg, e);
            if (tx != null) {
                LOGGER.debug("rolling back transaction");
                tx.rollback();
            }
            throw e;
        }
        return count;
    }
}
