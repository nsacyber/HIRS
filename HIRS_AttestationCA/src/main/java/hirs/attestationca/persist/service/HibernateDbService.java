package hirs.attestationca.persist.service;

import hirs.attestationca.persist.DBManagerException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract class that has the underlying Hibernate commands used by other DB Managers.
 * This class exists primarily to reduce code in {@link hirs.attestationca.persist.service.DefaultDbService} which retries these methods
 * using a RetryTemplate.
 *
 * @param <T> type of objects to manage by this manager
 */
@Log4j2
public abstract class HibernateDbService<T> {

    private static final int MAX_CLASS_CACHE_ENTRIES = 500;

    private final Class<T> clazz;
    @PersistenceContext
    private EntityManager entityManager;
    private CriteriaBuilder criteriaBuilder;
    private CriteriaQuery<T> criteriaQuery;

    /**
     * Creates a new <code>AbstractDbManager</code>.
     *
     * @param clazz Class to search for when doing Hibernate queries,
     * unfortunately class type of T cannot be determined using only T
     * @param entityManager the session factory to use to interact with the database
     */
    public HibernateDbService(final Class<T> clazz, final EntityManager entityManager) {
        if (clazz == null) {
            log.error("HibernateDbService cannot be instantiated with a null class");
            throw new IllegalArgumentException(
                    "HibernateDbService cannot be instantiated with a null class"
            );
        }
//        if (entityManager == null) {
//            log.error("HibernateDbService cannot be instantiated with a null SessionFactory");
//            throw new IllegalArgumentException(
//                    "HibernateDbService cannot be instantiated with a null SessionFactory"
//            );
//        }
        this.clazz = clazz;
        this.entityManager = entityManager;
    }

    public HibernateDbService() {
        clazz = null;
    }

    /**
     * Returns a list of all <code>T</code>s of type <code>clazz</code> in the database, with an
     * additional restriction also specified in the query.
     * <p>
     * This would be useful if <code>T</code> has several subclasses being
     * managed. This class argument allows the caller to limit which types of
     * <code>T</code> should be returned.
     *
     * @param clazz class type of <code>T</code>s to search for (may be null to
     * use Class&lt;T&gt;)
     * @param additionalRestriction - an added Criterion to use in the query, null for none
     * @return list of <code>T</code> names
     * @throws DBManagerException if unable to search the database
     */
    protected List<T> doGetList(final Class<? extends T> clazz)
            throws DBManagerException {
        log.debug("Getting object list");
        Class<? extends T> searchClass = clazz;
        if (clazz == null) {
            log.debug("clazz is null");
            searchClass = this.clazz;
        }

        List<T> objects = new ArrayList<>();

        return objects;
    }

    /**
     * Deletes the object from the database. This removes all of the database
     * entries that stored information with regards to the this object.
     * <p>
     * If the object is referenced by any other tables then this will throw a
     * <code>DBManagerException</code>.
     *
     * @param name name of the object to delete
     * @return true if successfully found and deleted the object
     * @throws DBManagerException if unable to find the baseline or delete it
     * from the database
     */
//    protected boolean doDelete(final String name) throws DBManagerException {
//        log.debug("deleting object: {}", name);
//        if (name == null) {
//            log.debug("null name argument");
//            return false;
//        }
//
//        boolean deleted = false;
//        Session session = entityManager.unwrap(Session.class);
//        try {
//            log.debug("retrieving object from db");
//            criteriaBuilder = session.getCriteriaBuilder();
//            criteriaQuery = criteriaBuilder.createQuery(clazz);
//            Root<T> root = criteriaQuery.from(clazz);
//            criteriaQuery.select(root).where(criteriaBuilder.equal(root.get("name"), name));
//
//            Object object = session.createQuery(criteriaQuery).getSingleResult();
//
//            if (clazz.isInstance(object)) {
//                T objectOfTypeT = clazz.cast(object);
//                log.debug("found object, deleting it");
//                session.delete(objectOfTypeT);
//                deleted = true;
//            }
//        } catch (Exception e) {
//            final String msg = "unable to retrieve object";
//            log.error(msg, e);
//            throw new DBManagerException(msg, e);
//        }
//        return deleted;
//    }


}
