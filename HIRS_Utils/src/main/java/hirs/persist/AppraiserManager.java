package hirs.persist;

import hirs.appraiser.Appraiser;

import java.util.List;

/**
 * An <code>AppraiserManager</code> manages <code>Appraiser</code>s. An
 * <code>AppraiserManager</code> is used to store and manage appraisers. It has
 * support for the basic create, read, update, and delete methods.
 */
public interface AppraiserManager {

    /**
     * Stores a new <code>Appraiser</code>. This stores a new
     * <code>Appraiser</code> to be managed by the <code>AppraiserManager</code>
     * . If the <code>Appraiser</code> is successfully saved then a reference to
     * it is returned.
     *
     * @param appraiser
     *            appraiser to save
     * @return reference to saved appraiser
     * @throws AppraiserManagerException
     *             if the appraiser has previously been saved or unexpected
     *             error occurs
     */
    Appraiser saveAppraiser(Appraiser appraiser)
            throws AppraiserManagerException;

    /**
     * Updates an <code>Appraiser</code>. This updates the <code>Appraiser</code>
     * that is managed so subsequent calls to get this <code>Appraiser</code>
     * will return the values set by the incoming <code>Appraiser</code>.
     *
     * @param appraiser
     *            appraiser
     * @throws AppraiserManagerException
     *             if unable to update the appraiser
     */
    void updateAppraiser(Appraiser appraiser) throws AppraiserManagerException;

    /**
     * Returns a list of all appraiser names managed by this manager. Every
     * <code>Appraiser</code> must have a name that users can use to reference
     * the <code>Appraiser</code>. This returns a listing of all the
     * <code>Appraiser</code>s.
     *
     * @return list of <code>Appraiser</code> names
     * @throws AppraiserManagerException
     *             if unable to create the list
     */
    List<Appraiser> getAppraiserList() throws AppraiserManagerException;

    /**
     * This returns a listing of all the appraisers of the given appraiser class.
     * Nominally, there should be one appraiser of each type in the database with the
     * current usage pattern.
     * <code>Appraiser</code>s.
     *
     * @param clazz the class of Appraiser to retrieve
     * @return list of all <code>Appraiser</code>s of the given type
     * @throws AppraiserManagerException if unable to create the list
     */
    List<Appraiser> getAppraiserList(Class<? extends Appraiser> clazz)
            throws AppraiserManagerException;

    /**
     * Retrieves the <code>Appraiser</code> identified by <code>name</code>. If
     * the <code>Appraiser</code> cannot be found then null is returned.
     *
     * @param name
     *            name of the <code>Appraiser</code>
     * @return <code>Appraiser</code> whose name is <code>name</code> or null
     *         if not found
     * @throws AppraiserManagerException
     *             if unable to retrieve the appraiser
     */
    Appraiser getAppraiser(String name) throws AppraiserManagerException;

    /**
     * Deletes the <code>Appraiser</code> identified by <code>name</code>. If
     * the <code>Appraiser</code> is found and deleted then true is returned,
     * otherwise false.
     *
     * @param name
     *            name of the <code>Appraiser</code> to delete
     * @return true if successfully found and deleted from repo, otherwise
     *              false
     * @throws AppraiserManagerException
     *             if unable to delete the appraiser for any reason other than
     *             not found
     */
    boolean deleteAppraiser(String name) throws AppraiserManagerException;

}
