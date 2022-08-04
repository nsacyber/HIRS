package hirs.persist.service;

import hirs.appraiser.Appraiser;
import hirs.persist.AppraiserManagerException;

/**
 * A <code>AppraiserService</code> manages <code>Appraiser</code>s. A
 * <code>AppraiserService</code> is used to store and manage Appraisers. It has
 * support for the basic create, read, update, and delete methods.
 */
public interface AppraiserService {
    /**
     * Stores a new <code>Appraiser</code>. This stores a new
     * <code>Appraiser</code> to be managed by the <code>AppraiserManager</code>
     * . If the <code>Appraiser</code> is successfully saved then a reference to
     * it is returned.
     *
     * @param appraiser
     *            appraiser to save
     * @return reference to saved appraiser
     * @throws hirs.persist.AppraiserManagerException
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
     * @param appraiser
     *            name of the <code>Appraiser</code> to delete
     * @throws AppraiserManagerException
     *             if unable to delete the appraiser for any reason other than
     *             not found
     */
    void deleteAppraiser(Appraiser appraiser) throws AppraiserManagerException;
}
