package hirs.persist;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import hirs.FilteredRecordsList;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.Transaction;
import org.hibernate.criterion.Conjunction;
import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.internal.SessionFactoryImpl;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import static org.hibernate.criterion.Restrictions.ilike;
import static org.hibernate.criterion.Restrictions.sqlRestriction;

/**
 * Abstract class that has the underlying Hibernate commands used by other DB Managers.
 * This class exists primarily to reduce code in {@link DBManager} which retries these methods
 * using a RetryTemplate.
 *
 * @param <T> type of objects to manage by this manager
 */
public abstract class AbstractDbManager<T> implements CrudManager<T> {

    private static final Logger LOGGER = LogManager.getLogger(AbstractDbManager.class);
    private static final int MAX_CLASS_CACHE_ENTRIES = 500;

    private final Class<T> clazz;

    private SessionFactory factory;

    /**
     * Creates a new <code>AbstractDbManager</code>.
     *
     * @param clazz Class to search for when doing Hibernate queries,
     * unfortunately class type of T cannot be determined using only T
     * @param sessionFactory the session factory to use to interact with the database
     */
    public AbstractDbManager(final Class<T> clazz, final SessionFactory sessionFactory) {
        if (clazz == null) {
            LOGGER.error("AbstractDbManager cannot be instantiated with a null class");
            throw new IllegalArgumentException(
                    "AbstractDbManager cannot be instantiated with a null class"
            );
        }
        if (sessionFactory == null) {
            LOGGER.error("AbstractDbManager cannot be instantiated with a null SessionFactory");
            throw new IllegalArgumentException(
                    "AbstractDbManager cannot be instantiated with a null SessionFactory"
            );
        }
        this.clazz = clazz;
        this.factory = sessionFactory;
    }

    private static final LoadingCache<Class, Set<Field>> PERSISTED_FIELDS =
            CacheBuilder.newBuilder()
                    .maximumSize(MAX_CLASS_CACHE_ENTRIES)
                    .build(
                            new CacheLoader<Class, Set<Field>>() {
                                @Override
                                public Set<Field> load(final Class clazz) throws Exception {
                                    return getPersistedFields(clazz);
                                }
                            }
                    );

    private static Set<Field> getPersistedFields(final Class clazz) {
        Set<Field> fields = new HashSet<>();

        for (Field f : clazz.getDeclaredFields()) {
            if (f.isAnnotationPresent(OneToMany.class)
                    || f.isAnnotationPresent(ManyToMany.class)
                    || f.isAnnotationPresent(ManyToOne.class)
                    || f.isAnnotationPresent(OneToOne.class)
                    || f.isAnnotationPresent(Column.class)) {
                fields.add(f);
            }
        }

        if (clazz.getSuperclass() != Object.class) {
            fields.addAll(getPersistedFields(clazz.getSuperclass()));
        }

        return fields;
    }

    private static final LoadingCache<Class, Set<Field>> LAZY_LOADED_FIELDS =
            CacheBuilder.newBuilder()
                    .maximumSize(MAX_CLASS_CACHE_ENTRIES)
                    .build(
                            new CacheLoader<Class, Set<Field>>() {
                                @Override
                                public Set<Field> load(final Class clazz) throws Exception {
                                    return getLazyFields(clazz);
                                }
                            }
                    );

    private static Set<Field> getLazyFields(final Class clazz) {
        Set<Field> fields = new HashSet<>();

        for (Field f : clazz.getDeclaredFields()) {
            if (f.isAnnotationPresent(OneToMany.class)) {
                if (f.getAnnotation(OneToMany.class).fetch().equals(FetchType.LAZY)) {
                    fields.add(f);
                }
                continue;
            }

            if (f.isAnnotationPresent(ManyToMany.class)) {
                if (f.getAnnotation(ManyToMany.class).fetch().equals(FetchType.LAZY)) {
                    fields.add(f);
                }
                continue;
            }
        }

        if (clazz.getSuperclass() != Object.class) {
            fields.addAll(getLazyFields(clazz.getSuperclass()));
        }

        return fields;
    }

    /**
     * Return the currently configured database implementation.
     *
     * @return the configured database implementation
     */
    protected DBManager.DBImpl getConfiguredImplementation() {
        String dialect = ((SessionFactoryImpl) factory).getDialect().toString().toLowerCase();
        if (dialect.contains("hsql")) {
            return DBManager.DBImpl.HSQL;
        } else if (dialect.contains("mysql")) {
            return DBManager.DBImpl.MYSQL;
        } else {
            throw new DBManagerException(String.format(
                    "Using unknown implementation: %s", dialect
            ));
        }
    }

    /**
     * Deletes the object from the database. This removes all of the database
     * entries that stored information with regards to this object.
     * <p>
     * If the object is referenced by any other tables then this will throw a
     * <code>DBManagerException</code>.
     *
     * @param id id of the object to delete
     * @return true if successfully found and deleted the object
     * @throws DBManagerException if unable to find the baseline or delete it
     * from the database
     */
    protected boolean doDelete(final Serializable id) throws DBManagerException {
        LOGGER.debug("deleting object: {}", id);
        if (id == null) {
            LOGGER.debug("null id argument");
            return false;
        }

        boolean deleted = false;
        Transaction tx = null;
        Session session = factory.getCurrentSession();
        try {
            LOGGER.debug("retrieving object from db");
            tx = session.beginTransaction();
            Object o = session.get(clazz, id);
            if (o != null && clazz.isInstance(o)) {
                T objectOfTypeT = clazz.cast(o);
                LOGGER.debug("found object, deleting it");
                session.delete(objectOfTypeT);
                deleted = true;
            } else {
                LOGGER.debug("object not found");
            }
            tx.commit();
        } catch (Exception e) {
            final String msg = "unable to retrieve object";
            LOGGER.error(msg, e);
            if (tx != null) {
                LOGGER.debug("rolling back transaction");
                tx.rollback();
            }
            throw new DBManagerException(msg, e);
        }
        return deleted;
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
    protected boolean doDelete(final String name) throws DBManagerException {
        LOGGER.debug("deleting object: {}", name);
        if (name == null) {
            LOGGER.debug("null name argument");
            return false;
        }

        boolean deleted = false;
        Transaction tx = null;
        Session session = factory.getCurrentSession();
        try {
            LOGGER.debug("retrieving object from db");
            tx = session.beginTransaction();
            Object object = session.createCriteria(clazz)
                    .add(Restrictions.eq("name", name)).uniqueResult();
            if (object != null && clazz.isInstance(object)) {
                T objectOfTypeT = clazz.cast(object);
                LOGGER.debug("found object, deleting it");
                session.delete(objectOfTypeT);
                deleted = true;
            }
            tx.commit();
        } catch (Exception e) {
            final String msg = "unable to retrieve object";
            LOGGER.error(msg, e);
            if (tx != null) {
                LOGGER.debug("rolling back transaction");
                tx.rollback();
            }
            throw new DBManagerException(msg, e);
        }
        return deleted;
    }

    /**
     * Deletes the object from the database. This removes all of the database
     * entries that stored information with regards to the this object.
     * <p>
     * If the object is referenced by any other tables then this will throw a
     * <code>DBManagerException</code>.
     *
     * @param object object to delete
     * @return true if successfully found and deleted the object
     * @throws DBManagerException if unable to find the baseline or delete it
     * from the database
     */
    protected boolean doDelete(final T object) throws DBManagerException {
        LOGGER.debug("deleting object: {}", object);
        if (object == null) {
            LOGGER.debug("null object argument");
            return false;
        }

        Transaction tx = null;
        Session session = factory.getCurrentSession();
        try {
            LOGGER.debug("deleting object from db");
            tx = session.beginTransaction();
            session.delete(object);
            tx.commit();
            return true;
        } catch (Exception e) {
            final String msg = "unable to delete object";
            LOGGER.error(msg, e);
            if (tx != null) {
                LOGGER.debug("rolling back transaction");
                tx.rollback();
            }
            throw new DBManagerException(msg, e);
        }
    }

    /**
     * Deletes all instances of the associated class.
     *
     * @return the number of entities deleted
     * @throws DBManagerException if unable to delete the records
     */
    protected int doDeleteAll() throws DBManagerException {
        int numEntitiesDeleted = 0;
        Transaction tx = null;
        Session session = factory.getCurrentSession();
        try {
            LOGGER.debug("Deleting instances of class: {}", clazz);
            tx = session.beginTransaction();
            List instances = session.createCriteria(clazz)
                    .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY).list();
            for (Object instance : instances) {
                if (instance != null && clazz.isInstance(instance)) {
                    session.delete(clazz.cast(instance));
                    numEntitiesDeleted++;
                }
            }
            tx.commit();
        } catch (Exception e) {
            final String msg = "unable to truncate class";
            LOGGER.error(msg, e);
            if (tx != null) {
                LOGGER.debug("rolling back transaction");
                tx.rollback();
            }
            throw new DBManagerException(msg, e);
        }
        return numEntitiesDeleted;
    }

    /**
     * Runs a Criteria query using the given collection of Criterion over the
     * associated class.
     *
     * @param criteriaCollection the collection of Criterion to apply
     *
     * @return a List of objects that match the criteria
     * @throws DBManagerException if an error is encountered while performing the query or creating
     * the result objects
     */
    protected List<T> doGetWithCriteria(final Collection<Criterion> criteriaCollection)
            throws DBManagerException {
        return doGetWithCriteria(clazz, criteriaCollection);
    }

    /**
     * Runs a Criteria query using the given collection of Criterion over the
     * associated class.
     *
     * @param <U> the specific type of class to retrieve
     *            (should extend this class' &lt;T&gt; parameter)
     * @param clazzToGet the class of object to retrieve
     * @param criteriaCollection the collection of Criterion to apply
     *
     * @return a List of objects that match the criteria
     * @throws DBManagerException if an error is encountered while performing the query or creating
     * the result objects
     */
    protected final <U extends T> List<U> doGetWithCriteria(
            final Class<U> clazzToGet,
            final Collection<Criterion> criteriaCollection
    ) throws DBManagerException {
        LOGGER.debug("running criteria query over: {}", clazzToGet);
        if (clazzToGet == null || criteriaCollection == null) {
            LOGGER.debug("null object argument");
            throw new NullPointerException("criteria or restrictions");
        }
        List<U> ret = new ArrayList<>();
        Transaction tx = null;
        Session session = factory.getCurrentSession();
        try {
            LOGGER.debug("retrieving criteria from db");
            tx = session.beginTransaction();
            Criteria criteria = session.createCriteria(clazzToGet);
            for (Criterion crit : criteriaCollection) {
                criteria.add(crit);
            }
            List list = criteria.list();
            for (Object o : list) {
                if (o != null && clazzToGet.isInstance(o)) {
                    ret.add(clazzToGet.cast(o));
                }
            }
            tx.commit();
        } catch (Exception e) {
            final String msg = "unable to retrieve list";
            LOGGER.error(msg, e);
            if (tx != null) {
                LOGGER.debug("rolling back transaction");
                tx.rollback();
            }
            throw new DBManagerException(msg, e);
        }
        return ret;
    }


    /**
     * Saves the <code>Object</code> in the database. This creates a new
     * database session and saves the object. If the <code>Object</code> had
     * previously been saved then a <code>DBManagerException</code> is thrown.
     *
     * @param object object to save
     * @return reference to saved object
     * @throws DBManagerException if object has previously been saved or an
     * error occurs while trying to save it to the database
     */
    protected T doSave(final T object) throws DBManagerException {
        LOGGER.debug("saving object: {}", object);
        if (object == null) {
            LOGGER.debug("null object argument");
            throw new NullPointerException("object");
        }

        Transaction tx = null;
        Session session = factory.getCurrentSession();
        try {
            LOGGER.debug("saving object in db");
            tx = session.beginTransaction();
            final Serializable id = session.save(object);
            Object o = session.get(object.getClass(), id);
            session.getTransaction().commit();
            return clazz.cast(o);
        } catch (Exception e) {
            final String msg = "unable to save object";
            LOGGER.error(msg, e);
            if (tx != null) {
                LOGGER.debug("rolling back transaction");
                tx.rollback();
            }
            throw new DBManagerException(msg, e);
        }
    }

    /**
     * Updates an object stored in the database. This updates the database
     * entries to reflect the new values that should be set.
     *
     * @param object object to update
     * @throws DBManagerException if unable to update the record
     */
    protected void doUpdate(final T object) throws DBManagerException {
        LOGGER.debug("updating object");
        if (object == null) {
            LOGGER.debug("null object argument");
            throw new NullPointerException("object");
        }

        Transaction tx = null;
        Session session = factory.getCurrentSession();
        try {
            LOGGER.debug("updating object in db");
            tx = session.beginTransaction();
            session.merge(object);
            tx.commit();
        } catch (Exception e) {
            final String msg = "unable to update object";
            LOGGER.error(msg, e);
            if (tx != null) {
                LOGGER.debug("rolling back transaction");
                tx.rollback();
            }
            throw new DBManagerException(msg, e);
        }
    }

    /**
     * Retrieves the <code>Object</code> from the database. This searches the
     * database for an entry whose name matches <code>name</code>. It then
     * reconstructs the <code>Object</code> from the database entry.
     *
     * @param name name of the object
     * @return object if found, otherwise null.
     * @throws DBManagerException if unable to search the database or recreate
     * the <code>Object</code>
     */
    protected T doGet(final String name) throws DBManagerException {
        LOGGER.debug("getting object: {}", name);
        if (name == null) {
            LOGGER.debug("null name argument");
            return null;
        }

        Transaction tx = null;
        Session session = factory.getCurrentSession();
        try {
            LOGGER.debug("retrieving " + clazz.toString() + " from db");
            tx = session.beginTransaction();
            T ret = clazz.cast(session.createCriteria(clazz)
                    .add(Restrictions.eq("name", name)).uniqueResult());
            tx.commit();
            return ret;
        } catch (Exception e) {
            final String msg = "unable to retrieve object";
            LOGGER.error(msg, e);
            if (tx != null) {
                LOGGER.debug("rolling back transaction");
                tx.rollback();
            }
            throw new DBManagerException(msg, e);
        }
    }

    /**
     * Retrieves the <code>Object</code> from the database. This searches the
     * database for an entry whose id matches <code>id</code>. It then
     * reconstructs the <code>Object</code> from the database entry.
     *
     * @param id id of the object
     * @return object if found, otherwise null.
     * @throws DBManagerException if unable to search the database or recreate
     * the <code>Object</code>
     */
    protected T doGet(final Serializable id) throws DBManagerException {
        LOGGER.debug("getting object: {}", id);
        if (id == null) {
            LOGGER.debug("null id argument");
            return null;
        }
        Transaction tx = null;
        Session session = factory.getCurrentSession();
        try {
            LOGGER.debug("retrieving object from db");
            tx = session.beginTransaction();
            T ret = clazz.cast(session.get(clazz, id));
            tx.commit();
            return ret;
        } catch (Exception e) {
            final String msg = "unable to retrieve object";
            LOGGER.error(msg, e);
            if (tx != null) {
                LOGGER.debug("rolling back transaction");
                tx.rollback();
            }
            throw new DBManagerException(msg, e);
        }
    }

    private void doLoadLazyFields(final Object obj, final boolean recurse)
            throws ExecutionException, IllegalAccessException, NoSuchMethodException,
            InvocationTargetException {
        doLoadLazyFields(obj, recurse, new HashSet<>());
    }

    private void doLoadLazyFields(final Object obj, final boolean recurse,
                                  final Set<Object> doNotLoad)
            throws ExecutionException, IllegalAccessException, NoSuchMethodException,
            InvocationTargetException {
        if (obj == null) {
            return;
        }

        if (!obj.getClass().isAnnotationPresent(Entity.class)) {
            return;
        }

        doNotLoad.add(obj);

        for (Field field : LAZY_LOADED_FIELDS.get(obj.getClass())) {
            field.setAccessible(true);
            Object fieldObj = FieldUtils.readField(obj, field.getName(), true);
            Hibernate.initialize(fieldObj);
            field.setAccessible(false);
            if (fieldObj instanceof Collection) {
                Collection.class.getMethod("size").invoke(fieldObj);
            }
        }

        if (recurse) {
            for (Field field : PERSISTED_FIELDS.get(obj.getClass())) {
                field.setAccessible(true);
                Object fieldObj = FieldUtils.readField(obj, field.getName(), true);
                field.setAccessible(false);
                if (!doNotLoad.contains(fieldObj)) {
                    if (fieldObj instanceof Collection) {
                        for (Object o : (Collection) fieldObj) {
                            doLoadLazyFields(o, true, doNotLoad);
                        }
                    } else {
                        doLoadLazyFields(fieldObj, true, doNotLoad);
                    }
                }
            }
        }
    }

    /**
     * Retrieves the <code>Object</code> from the database. This searches the
     * database for an entry whose name matches <code>name</code>. It then
     * reconstructs the <code>Object</code> from the database entry.  It will also
     * load all the lazy fields in the given class.  If the parameter <code>recurse</code>
     * is set to true, this method will recursively descend into each of the object's fields
     * to load all the lazily-loaded entities.  If false, only the fields belonging to the object
     * itself will be loaded.
     *
     * @param name name of the object
     * @param recurse whether to recursively load lazy data throughout the object's structures
     * @return object if found, otherwise null.
     * @throws DBManagerException if unable to search the database or recreate
     * the <code>Object</code>
     */
    protected T doGetAndLoadLazyFields(final String name, final boolean recurse)
            throws DBManagerException {
        LOGGER.debug("getting object: {}", name);
        if (name == null) {
            LOGGER.debug("null id argument");
            return null;
        }

        Transaction tx = null;
        Session session = factory.getCurrentSession();
        try {
            LOGGER.debug("retrieving " + clazz.toString() + " from db");
            tx = session.beginTransaction();
            T ret = clazz.cast(session.createCriteria(clazz)
                    .add(Restrictions.eq("name", name)).uniqueResult());
            doLoadLazyFields(ret, recurse);
            tx.commit();
            return ret;
        } catch (Exception e) {
            final String msg = "unable to retrieve object";
            LOGGER.error(msg, e);
            if (tx != null) {
                LOGGER.debug("rolling back transaction");
                tx.rollback();
            }
            throw new DBManagerException(msg, e);
        }
    }

    /**
     * Retrieves the <code>Object</code> from the database. This searches the
     * database for an entry whose id matches <code>id</code>. It then
     * reconstructs the <code>Object</code> from the database entry.  It will also
     * load all the lazy fields in the given class.  If the parameter <code>recurse</code>
     * is set to true, this method will recursively descend into each of the object's fields
     * to load all the lazily-loaded entities.  If false, only the fields belonging to the object
     * itself will be loaded.
     *
     * @param id id of the object
     * @param recurse whether to recursively load lazy data throughout the object's structures
     * @return object if found, otherwise null.
     * @throws DBManagerException if unable to search the database or recreate
     * the <code>Object</code>
     */
    protected T doGetAndLoadLazyFields(final Serializable id, final boolean recurse)
            throws DBManagerException {
        LOGGER.debug("getting object: {}", id);
        if (id == null) {
            LOGGER.debug("null id argument");
            return null;
        }

        Transaction tx = null;
        Session session = factory.getCurrentSession();
        try {
            LOGGER.debug("retrieving object from db");
            tx = session.beginTransaction();
            T ret = clazz.cast(session.get(clazz, id));
            doLoadLazyFields(ret, recurse);
            tx.commit();
            return ret;
        } catch (Exception e) {
            final String msg = "unable to retrieve object";
            LOGGER.error(msg, e);
            if (tx != null) {
                LOGGER.debug("rolling back transaction");
                tx.rollback();
            }
            throw new DBManagerException(msg, e);
        }
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
    protected List<T> doGetList(final Class<? extends T> clazz,
                                final Criterion additionalRestriction)
            throws DBManagerException {
        LOGGER.debug("Getting object list");
        Class<? extends T> searchClass = clazz;
        if (clazz == null) {
            LOGGER.debug("clazz is null");
            searchClass = this.clazz;
        }

        List<T> objects = new ArrayList<>();
        Transaction tx = null;
        Session session = factory.getCurrentSession();
        try {
            LOGGER.debug("Retrieving objects from db of class {}", searchClass.getName());
            tx = session.beginTransaction();
            Criteria criteria = session.createCriteria(searchClass);
            if (additionalRestriction != null) {
                criteria.add(additionalRestriction);
            }
            List list = criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY).list();
            for (Object o : list) {
                if (searchClass.isInstance(o)) {
                    objects.add(searchClass.cast(o));
                }
            }
            tx.commit();
            LOGGER.debug("Got {} objects", objects.size());
        } catch (HibernateException e) {
            LOGGER.error("Unable to retrieve baseline", e);
            if (tx != null) {
                LOGGER.debug("Rolling back transaction");
                tx.rollback();
            }
            throw e;
        }
        return objects;
    }

    /**
     * Returns a list of all <code>T</code>s that are ordered by a column and
     * direction (ASC, DESC) that is provided by the user. This method helps
     * support the server-side processing in the JQuery DataTables. For entities that support
     * soft-deletes, the returned list does not contain <code>T</code>s that have been soft-deleted.
     *
     * @param clazz class type of <code>T</code>s to search for (may be null to
     * use Class&lt;T&gt;)
     * @param columnToOrder - Column to be ordered
     * @param ascending - direction of sort
     * @param firstResult starting point of first result in set
     * @param maxResults total number we want returned for display in table
     * @param search string of criteria to be matched to visible columns
     * @param searchableColumns Map of String and boolean values with column
     * headers and whether they should be searched. Boolean is true if field provides a
     * typical String that can be searched by Hibernate without transformation.
     * @param criteriaModifier - a way to modify the criteria used in the query
     * @return FilteredRecordsList object with query data
     * @throws DBManagerException if unable to create the list
     */
    @SuppressWarnings("checkstyle:parameternumber")
    protected FilteredRecordsList<T> doGetOrderedList(final Class<? extends T> clazz,
            final String columnToOrder, final boolean ascending, final int firstResult,
            final int maxResults, final String search, final Map<String, Boolean> searchableColumns,
            final CriteriaModifier criteriaModifier) throws DBManagerException {
        LOGGER.debug("Getting object list");
        Class<? extends T> searchClass = clazz;
        if (clazz == null) {
            LOGGER.debug("clazz is null");
            searchClass = this.clazz;
        }

        LOGGER.info(searchClass.getName() + " querying for "
            + Arrays.toString(searchableColumns.entrySet().toArray())
            + " with search strings \"" + search + "\"");

        //Object that will store query values
        FilteredRecordsList<T> aqr = new FilteredRecordsList<>();

        List<T> objects = new ArrayList<>();
        Transaction tx = null;
        Session session = factory.getCurrentSession();
        try {
            LOGGER.debug("updating object in db");
            tx = session.beginTransaction();

            //Returns totalResults in the given entity
            Criteria criteria = session.createCriteria(searchClass)
                    .setProjection(Projections.countDistinct("id"));
            criteriaModifier.modify(criteria);

            Long totalResultCount = (Long) criteria.uniqueResult();

            Long recordsFiltered = totalResultCount;
            Conjunction and = Restrictions.conjunction();
            if (totalResultCount != 0) {
                LOGGER.info("Total result count greater than 0");
                //Builds the search criteria from all of the searchable columns
                if (!searchableColumns.isEmpty()) {
                    // Search for all words in all searchable columns
                    String[] searchWords = search.split(" ");
                    for (String word : searchWords) {
                        // Every word must be in at least one column
                        Disjunction or = Restrictions.disjunction();
                        for (Map.Entry<String, Boolean> entry : searchableColumns.entrySet()) {
                            if (entry.getValue()) {
                                or.add(ilike(entry.getKey(), word, MatchMode.ANYWHERE));
                            } else {
                                or.add(ilikeCast(entry.getKey(), word));
                            }
                        }
                        and.add(or);
                    }
                }

                LOGGER.info("Search columns filtered");
                //Retrieves a count of all the records after being filtered
                criteria.setProjection(Projections.countDistinct("id"))
                        .add(and);
                try {
                    LOGGER.info("Get unique result from criteria object");
                    recordsFiltered = (Long) criteria.uniqueResult();
                } catch (HibernateException e) {
                    LOGGER.error(e.getMessage());
                }
            }

            if (recordsFiltered != 0) {
                //Generates an inner query that handles the searching, paging,
                //and sorting of the data.  The query returns distinct ids in
                //order based on these values
                Criteria uniqueSubCriteria = session.createCriteria(searchClass)
                        .setProjection(
                                Projections.distinct(
                                        Projections.property("id")))
                        .add(and)
                        .setFirstResult(firstResult)
                        .setMaxResults(maxResults);
                criteriaModifier.modify(uniqueSubCriteria);
                if (ascending) {
                    uniqueSubCriteria.addOrder(Order.asc(columnToOrder));
                } else {
                    uniqueSubCriteria.addOrder(Order.desc(columnToOrder));
                }

                List ids = uniqueSubCriteria.list();

                //Values take the unique identities that passed all other
                //criteria and returns the desired entity.  Queries needed to be
                //separated in order to keep pagination and distinct results
                Criteria finalCriteria = session.createCriteria(searchClass)
                        .add(Restrictions.in("id", ids))
                        .setResultTransformer(
                                CriteriaSpecification.DISTINCT_ROOT_ENTITY);
                criteriaModifier.modify(finalCriteria);

                //Checks the order and validates before returning the values
                if (ascending) {
                    finalCriteria.addOrder(Order.asc(columnToOrder));
                } else {
                    finalCriteria.addOrder(Order.desc(columnToOrder));
                }

                List list = finalCriteria.list();
                for (Object o : list) {
                    if (clazz.isInstance(o)) {
                        objects.add(clazz.cast(o));
                    }
                }
            }
            //Stores results of all the queries for the JQuery Datatable
            aqr.setRecordsTotal(totalResultCount);
            aqr.setRecordsFiltered(recordsFiltered);
            aqr.addAll(objects);
            tx.commit();
        } catch (HibernateException e) {
            final String msg = "unable to update object";
            LOGGER.error(msg, e);
            if (tx != null) {
                LOGGER.debug("rolling back transaction");
                tx.rollback();
            }
            throw e;
        }
        LOGGER.info(searchClass.getName() + " found " + aqr.getRecordsTotal() + " records");
        return aqr;
    }

    /**
     * Returns the <code>SessionFactory</code>. The <code>SessionFactory</code>
     * can be used by sub-classes to make database calls.
     *
     * @return session factory
     */
    protected final SessionFactory getFactory() {
        return factory;
    }

    /**
     * Returns a StatelessSession that can be used for querying.
     *
     * @return stateless session
     */
    protected final StatelessSession getStatelessSession() {
        return factory.openStatelessSession();
    }


    /**
     * Helper method in order to properly cast columns that are not Strings into
     * characters for search comparison.
     *
     * @param field - the id of the column being used for comparison
     * @param search - the String that is being searched for
     * @return Criterion object used in a hibernate query
     */
    protected Criterion ilikeCast(final String field,
                                  final String search) {
        return sqlRestriction(" lower(cast(this_." + field
                + " as char)) like '%" + search + "%' ");
    }

    /**
     * Helper method in order to properly cast columns that are not Strings into
     * characters for search comparison.
     *
     * @param field - the id of the column being used for comparison
     * @param search - the String that is being searched for
     * @return Criterion object used in a hibernate query
     */
    protected Criterion ilikeHex(final String field,
                                 final String search) {
        DBManager.DBImpl impl = getConfiguredImplementation();
        String hexFunction;
        if (impl.equals(DBManager.DBImpl.MYSQL)) {
            hexFunction = "hex";
        } else if (impl.equals(DBManager.DBImpl.HSQL)) {
            hexFunction = "RAWTOHEX";
        } else {
            throw new UnsupportedOperationException("Unrecognized database: " + impl.toString());
        }

        String sql = " lower(%s(this_.%s)) like '%%%s%%' ";
        return sqlRestriction(String.format(sql, hexFunction, field, search));
    }
}
