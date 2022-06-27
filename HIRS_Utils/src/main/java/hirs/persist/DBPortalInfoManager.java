package hirs.persist;

import hirs.data.persist.enums.PortalScheme;
import hirs.data.persist.info.PortalInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.SessionFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * A <code>DBPortalInfoManager</code> is a service (extends <code>DBManager</code>) that
 * implements the <code>PortalInfoManager</code> that stores and retrieves Portal Info objects.
 */
@Service
public class DBPortalInfoManager extends DBManager<PortalInfo> implements PortalInfoManager {

    private static final Logger LOGGER = LogManager.getLogger(DBPortalInfoManager.class);

    /**
     * Creates a new <code>DBPortalInfoManager</code>. The optional SessionFactory parameter is
     * used to manage sessions with a hibernate db.
     *
     * @param factory a hibernate session
     */
    public DBPortalInfoManager(final SessionFactory factory) {
        super(PortalInfo.class, factory);
    }

    /**
     * Saves the <code>PortalInfo</code> in the database. This creates a new database
     * session and saves the PortalInfo. If the <code>PortalInfo</code> had previously
     * been saved then a <code>PortalInfoManagerException</code> is thrown.
     *
     * @param info PortalInfo to save
     * @return reference to saved PortalInfo
     * @throws PortalInfoManagerException if PortalInfo has previously been saved or an
     * error occurs while trying to save it to the database
     */
    @Override
    public final PortalInfo savePortalInfo(final PortalInfo info)
            throws PortalInfoManagerException {
        LOGGER.debug("saving Portal Info {}", info);
        try {
            return super.save(info);
        } catch (DBManagerException e) {
            throw new PortalInfoManagerException(e);
        }
    }

    /**
     * Updates a <code>PortalInfo</code>. This updates the database entries to reflect the new
     * values that should be set.
     *
     * @param info PortalInfo
     * @throws PortalInfoManagerException if PortalInfo has not previously been saved or
     * an error occurs while trying to save it to the database
     */
    @Override
    public final void updatePortalInfo(final PortalInfo info)
            throws PortalInfoManagerException {
        LOGGER.debug("updating Portal Info: {}", info);
        try {
            super.update(info);
        } catch (DBManagerException e) {
            throw new PortalInfoManagerException(e);
        }
    }

    /**
     * Retrieves the <code>PortalInfo</code> from the database. This searches the database for an
     * entry whose name matches <code>name</code>. It then reconstructs a <code>PortalInfo</code>
     * object from the database entry.
     *
     * @param scheme PortalInfo.Scheme of the PortalInfo
     * @return PortalInfo if found, otherwise null.
     * @throws PortalInfoManagerException if unable to search the database or recreate the
     * <code>PortalInfo</code>
     */
    @Override
    public final PortalInfo getPortalInfo(final PortalScheme scheme)
            throws PortalInfoManagerException {
        LOGGER.debug("getting Portal Info: {}", scheme.name());
        try {
            return super.get(scheme.name());
        } catch (DBManagerException e) {
            throw new PortalInfoManagerException(e);
        }
    }

    /**
     * Deletes the <code>PortalInfo</code> from the database. This removes all of the database
     * entries that stored information with regards to the this <code>PortalInfo</code>.
     * Currently, iterates over <code>Policy</code> entries and removes the selected
     * <code>PortalInfo</code> from them if it exists. This needs to be fixed as this should not
     * be performed without user action. Update is expected soon.
     *
     * @param scheme PortalInfo.Scheme of the <code>PortalInfo</code> to delete
     * @return true if successfully found and deleted <code>PortalInfo</code>
     * @throws PortalInfoManagerException if unable to find the PortalInfo or delete it
     * from the database
     */
    @Override
    public final boolean deletePortalInfo(final PortalScheme scheme)
            throws PortalInfoManagerException {
        LOGGER.debug("deleting Portal Info: {}", scheme.name());
        try {
            return super.delete(scheme.name());
        } catch (DBManagerException e) {
            throw new PortalInfoManagerException(e);
        }
    }

    /**
     * Retrieve the <code>PortalInfo</code> object stored into the repo
     * and return the url it represents.
     *
     * @return the URL represented by the <code>PortalInfo</code> object.
     */
    @Override
    public final String getPortalUrlBase() {
        PortalInfo info;

        try {
            // Prefer HIRS to use HTTPS, but check HTTP if needed
            info = getPortalInfo(PortalScheme.HTTPS);
            if (info == null) {
                info = getPortalInfo(PortalScheme.HTTP);
            }
        } catch (Exception e) {
            info = null;
        }

        // The default base url
        String url = "Your_HIRS_Portal/";

        try {
            if (info != null && info.getIpAddress() != null) {
                String context = "/";
                if (info.getContextName() != null) {
                    context += info.getContextName() + "/";
                }
                URI uri = new URI(info.getSchemeName().toLowerCase(), null,
                        info.getIpAddress().getHostName(), info.getPort(),
                        context, null, null);
                url = uri.toString();
            }
        } catch (URISyntaxException e) {
            LOGGER.error("DBPortalInfoManager.getPortalUrlBase():"
                    + " Could not create the URI. Returning the default.");
        }

        return url;
    }
}
