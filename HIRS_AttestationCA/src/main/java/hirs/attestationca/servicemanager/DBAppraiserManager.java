package hirs.attestationca.servicemanager;

import hirs.appraiser.Appraiser;
import hirs.persist.AppraiserManager;
import hirs.persist.AppraiserManagerException;
import hirs.persist.DBManagerException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import java.util.List;

/**
 * This class defines a <code>ApprasierManager</code> that stores the
 * appraisers in a database.
 */
@Service
public class DBAppraiserManager extends DBManager<Appraiser> implements AppraiserManager {
    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Creates a new <code>DBAppraiserManager</code> that uses the default
     * database. The default database is used to store all of the
     * <code>Appraiser</code>s.
     *
     * @param em entity manager used to access database connections
     */
    public DBAppraiserManager(final EntityManager em) {
        super(Appraiser.class, em);
    }

    /**
     * Saves the <code>Appraiser</code> in the database. This creates a new
     * database session and saves the appraiser. If the <code>Appraiser</code>
     * had previously been saved then a <code>AppraiserManagerException</code>
     * is thrown.
     *
     * @param appraiser
     *            appraiser to save
     * @return reference to saved appraiser
     * @throws hirs.persist.AppraiserManagerException
     *             if appraiser has previously been saved or an error occurs
     *             while trying to save it to the database
     */
    @Override
    public final Appraiser saveAppraiser(final Appraiser appraiser)
            throws AppraiserManagerException {
        LOGGER.debug("saving appraiser: {}", appraiser);
        try {
            return super.save(appraiser);
        } catch (DBManagerException e) {
            throw new AppraiserManagerException(e);
        }
    }

    /**
     * Updates an <code>Appraiser</code>. This updates the database entries to
     * reflect the new values that should be set.
     *
     * @param appraiser
     *            appraiser
     * @throws AppraiserManagerException
     *             if appraiser has not previously been saved or an error
     *             occurs while trying to save it to the database
     */
    @Override
    public final void updateAppraiser(final Appraiser appraiser) throws AppraiserManagerException {
        LOGGER.debug("updating appraiser: {}", appraiser);
        try {
            super.update(appraiser);
        } catch (DBManagerException e) {
            throw new AppraiserManagerException(e);
        }
    }

    /**
     * Returns a list of all <code>Appraiser</code> names. This searches through
     * the database for this information.
     *
     * @return list of <code>Appraiser</code> names
     * @throws AppraiserManagerException
     *             if unable to search the database
     */
    @Override
    public final List<Appraiser> getAppraiserList() throws AppraiserManagerException {
        LOGGER.debug("getting appraiser list");
        return getList(Appraiser.class);
    }

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
    @Override
    public final List<Appraiser> getAppraiserList(final Class<? extends Appraiser> clazz)
            throws AppraiserManagerException {
        LOGGER.debug("getting appraiser list");
        return null; //getList(clazz);  cyrus-dev
    }

    /**
     * Retrieves the <code>Appraiser</code> from the database. This searches
     * the database for an entry whose name matches <code>name</code>. It then
     * reconstructs a <code>Appraiser</code> object from the database entry
     *
     * @param name
     *            name of the appraiser
     * @return appraiser if found, otherwise null.
     * @throws AppraiserManagerException
     *             if unable to search the database or recreate the
     *             <code>Appraiser</code>
     */
    @Override
    public final Appraiser getAppraiser(final String name) throws AppraiserManagerException {
        LOGGER.debug("getting appraiser: {}", name);
        try {
            return super.get(name);
        } catch (DBManagerException e) {
            throw new AppraiserManagerException(e);
        }
    }

    /**
     * Deletes the <code>Appraiser</code> from the database. This removes all of
     * the database entries that stored information with regards to the
     * <code>Appraiser</code>.
     * <p>
     * If the <code>Appraiser</code> is referenced by any other tables then this
     * will throw a <code>AppraiserManagerException</code>.
     *
     * @param name
     *            name of the <code>Appraiser</code> to delete
     * @return true if successfully found and deleted the <code>Appraiser</code>
     * @throws AppraiserManagerException
     *             if unable to find the appraiser or delete it from the
     *             database
     */
    @Override
    public final boolean deleteAppraiser(final String name) throws AppraiserManagerException {
        LOGGER.debug("deleting appraiser: {}", name);
        try {
            return super.delete(name);
        } catch (DBManagerException e) {
            throw new AppraiserManagerException(e);
        }
    }

}
