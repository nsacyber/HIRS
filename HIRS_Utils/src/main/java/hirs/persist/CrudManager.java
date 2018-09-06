package hirs.persist;

import org.hibernate.criterion.Criterion;

import java.io.Serializable;
import java.util.List;

/**
 * Interface defining database CRUD operations (Create, Read, Update, Delete).
 * @param <T> the object type, T.
 */
public interface CrudManager<T> extends OrderedListQuerier<T> {

    /**
     * Deletes all instances of the associated class.
     *
     * @return the number of entities deleted
     */
    int deleteAll();

    /**
     * Saves the <code>Object</code> in the database. If the <code>Object</code> had
     * previously been saved then a <code>DBManagerException</code> is thrown.
     *
     * @param object object to save
     * @return reference to saved object
     * @throws DBManagerException if object has previously been saved or an
     * error occurs while trying to save it to the database
     */
    T save(T object) throws DBManagerException;

    /**
     * Updates an object stored in the database. This updates the database
     * entries to reflect the new values that should be set.
     *
     * @param object object to update
     * @throws DBManagerException if an error occurs while trying to save it to the database
     */
    void update(T object) throws DBManagerException;

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
    T get(String name) throws DBManagerException;

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
    T getAndLoadLazyFields(String name, boolean recurse)
            throws DBManagerException;

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
    T get(Serializable id) throws DBManagerException;

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
    T getAndLoadLazyFields(Serializable id, boolean recurse)
            throws DBManagerException;

    /**
     * Returns a list of all <code>T</code>s of type <code>clazz</code> in the database, with no
     * additional restrictions.
     * <p>
     * This would be useful if <code>T</code> has several subclasses being
     * managed. This class argument allows the caller to limit which types of
     * <code>T</code> should be returned.
     *
     * @param clazz class type of <code>T</code>s to search for (may be null to
     * use Class&lt;T&gt;)
     * @return list of <code>T</code> names
     * @throws DBManagerException if unable to search the database
     */
    List<T> getList(Class<? extends T> clazz)
                    throws DBManagerException;

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
    List<T> getList(Class<? extends T> clazz, Criterion additionalRestriction)
                            throws DBManagerException;

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
    boolean delete(String name) throws DBManagerException;

    /**
     * Deletes the object from the database. This removes all of the database
     * entries that stored information with regards to the this object.
     * <p>
     * If the object is referenced by any other tables then this will throw a
     * <code>DBManagerException</code>.
     *
     * @param id id of the object to delete
     * @return true if successfully found and deleted the object
     * @throws DBManagerException if unable to find the baseline or delete it
     * from the database
     */
    boolean delete(Serializable id)
            throws DBManagerException;

    /**
     * Deletes the object from the database. This removes all of the database
     * entries that stored information with regards to the this object.
     * <p>
     * If the object is referenced by any other tables then this will throw a
     * <code>DBManagerException</code>.
     *
     * @param object object to delete
     * @return true if successfully found and deleted the object
     * @throws DBManagerException if unable to delete the object from the database
     */
    boolean delete(T object) throws DBManagerException;

    /**
     * Archives the named object and updates it in the database.
     *
     * @param name name of the object to archive
     * @return true if the object was successfully found and archived, false if the object was not
     * found
     * @throws DBManagerException if the object is not an instance of <code>ArchivableEntity</code>
     */
    boolean archive(String name) throws DBManagerException;
}
