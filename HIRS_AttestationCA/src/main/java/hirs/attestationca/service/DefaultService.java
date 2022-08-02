package hirs.attestationca.service;

import java.util.List;
import java.util.UUID;

/**
 * A <code>DefaultService</code> manages base operations. A
 * <code>DefaultService</code> is used to store and manage devices. It has
 * support for the basic create, read, update, and delete methods.
 * @param <T> class type
 */
public interface DefaultService<T> {

    /**
     * Returns a list of all <code>T</code>. This searches through
     * the database for this information.
     *
     * @return list of <code>T</code>
     */
    List<T> getList();

    /**
     * All passed in objects of type T will either be updated.
     * However if the element doesn't exist, it will be saved.
     * @param elements list of objects to save
     */
    void updateElements(List<T> elements);

    /**
     * Deletes the <code>T</code> from the database. This removes all
     * of the database entries that stored information with regards to the
     * <code>T</code> with a foreign key relationship.
     *
     * @param uuid of the object to be deleted
     */
    void deleteObjectById(UUID uuid);
}
