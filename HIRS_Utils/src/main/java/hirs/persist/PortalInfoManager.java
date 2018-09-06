package hirs.persist;

import hirs.data.persist.PortalInfo;

/**
 * A <code>PortalInfoManager</code> manages <code>PortalInfo</code> objects. A
 * <code>PortalInfo</code> object is used to store data about the portal when
 * the service running it starts.
 */
public interface PortalInfoManager {
    /**
     * Stores a new <code>PortalInfo</code>. This stores a new
     * <code>PortalInfo</code> to be managed by the
     * <code>PortalInfoManager</code>. If the <code>PortalInfo</code> is
     * successfully saved then a reference to it is returned.
     *
     * @param info
     *            the PortalInfo to save PortalInfo to save
     * @return reference to saved PortalInfo
     * @throws PortalInfoManagerException
     *             if the PortalInfo has previously been saved or unexpected
     *             error occurs
     */
    PortalInfo savePortalInfo(PortalInfo info)
            throws PortalInfoManagerException;

    /**
     * Updates a <code>PortalInfo</code>. This updates the
     * <code>PortalInfo</code> that is managed so subsequent calls to get this
     * <code>PortalInfo</code> will return the values set by the incoming
     * <code>PortalInfo</code>.
     *
     * @param info PortalInfo
     *            PortalInfo
     * @throws PortalInfoManagerException
     *             if unable to update the PortalInfo
     */
    void updatePortalInfo(PortalInfo info)
            throws PortalInfoManagerException;

    /**
     * Retrieves the <code>PortalInfo</code> identified by <code>name</code>.
     * If the <code>PortalInfo</code> cannot be found then null is returned.
     *
     * @param scheme
     *            PortalInfo.Scheme of the <code>PortalInfo</code>
     * @return <code>PortalInfo</code> whose name is <code>name</code> or null
     *         if not found
     * @throws PortalInfoManagerException
     *             if unable to retrieve the PortalInfo
     */
    PortalInfo getPortalInfo(PortalInfo.Scheme scheme)
            throws PortalInfoManagerException;

    /**
     * Deletes the <code>PortalInfo</code> identified by <code>name</code>. If
     * the <code>PortalInfo</code> is found and deleted then true is returned,
     * otherwise false.
     *
     * @param scheme
     *            PortalInfo.Scheme of the <code>PortalInfo</code> to delete
     * @return true if successfully found and deleted from repo, otherwise false
     * @throws PortalInfoManagerException
     *             if unable to delete the PortalInfo for any reason other
     *             than not found
     */
    boolean deletePortalInfo(PortalInfo.Scheme scheme)
            throws PortalInfoManagerException;

    /**
     * Returns the base URL of the running Portal, or the default base URL if it is unknown.
     *
     * @return the base URL of the running Portal, or the default base URL if it is unknown
     */
    String getPortalUrlBase();
}
