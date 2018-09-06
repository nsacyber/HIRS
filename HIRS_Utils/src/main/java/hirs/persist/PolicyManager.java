package hirs.persist;

import hirs.appraiser.Appraiser;
import hirs.data.persist.Baseline;
import hirs.data.persist.Device;
import hirs.data.persist.DeviceGroup;
import hirs.data.persist.Policy;

import java.io.Serializable;
import java.util.List;


/**
 * A <code>PlicyManager</code> manages <code>Policy</code> objects. A
 * <code>PolicyManager</code> is used to store and manage policies. It has
 * support for the basic create, read, update, and delete methods.
 */
public interface PolicyManager {

    /**
     * Stores a new <code>Policy</code>. This stores a new
     * <code>Policy</code> to be managed by the <code>PolicyManager</code>.
     * If the <code>Policy</code> is successfully saved then a reference to it
     * is returned.
     *
     * @param policy
     *            Policy to save
     * @return reference to saved Policy
     * @throws PolicyManagerException
     *             if the Policy has previously been saved or unexpected error
     *             occurs
     */
    Policy savePolicy(Policy policy) throws PolicyManagerException;

    /**
     * Updates a <code>Policy</code>. This updates the <code>Policy</code>
     * that is managed so subsequent calls to get this <code>Policy</code>
     * will return the values set by the incoming <code>Policy</code>.
     *
     * @param policy
     *            Policy
     * @throws PolicyManagerException
     *             if unable to update the Policy
     */
    void updatePolicy(Policy policy) throws PolicyManagerException;

    /**
     * Returns a list of all <code>Policy</code>s managed by this manager. A
     * <code>Class</code> argument may be specified to limit which types of
     * <code>Policy</code>s to return. This argument may be null to return all
     * <code>Policy</code>s.
     *
     * @param clazz
     *            class type of <code>Policy</code>s to return (may be null)
     * @return list of all managed <code>Policy</code> objects
     * @throws PolicyManagerException
     *             if unable to create the list
     */
    List<Policy> getPolicyList(Class<? extends Policy> clazz)
            throws PolicyManagerException;

    /**
     * Return a list of all the policies that contain the given baseline.
     *
     * @param clazz the class of Policy to search
     * @param baseline the baseline that should be a member of returned Policies
     * @return the list of matching Policies
     */
    List<Policy> getPoliciesContainingBaseline(
            Class<? extends Policy> clazz,
            Baseline baseline
    );

    /**
     * Retrieves the <code>Policy</code> identified by <code>name</code>. If
     * the <code>Policy</code> cannot be found then null is returned.
     *
     * @param name
     *            name of the <code>Policy</code>
     * @return <code>Policy</code> whose name is <code>name</code> or null if
     *         not found
     * @throws PolicyManagerException
     *             if unable to retrieve the Policy
     */
    Policy getPolicy(String name) throws PolicyManagerException;

    /**
     * Retrieves the <code>Policy</code> identified by the given <code>id</code>. If
     * the <code>Policy</code> cannot be found then null is returned.
     *
     * @param id
     *            id of the desired <code>Policy</code>
     * @return <code>Policy</code> whose id is <code>id</code> or null if
     *         not found
     * @throws PolicyManagerException
     *             if unable to retrieve the Policy
     */
    Policy getPolicy(Serializable id) throws PolicyManagerException;

    /**
     * Archives the named <code>Policy</code> and updates it in the database.
     *
     * @param name name of the <code>Policy</code> to archive
     * @return true if the <code>Policy</code> was successfully found and archived, false if
     * it was not found
     * @throws DBManagerException if the <code>Policy</code> is not an instance of
     * <code>ArchivableEntity</code>
     */
    boolean archive(String name) throws DBManagerException;

    /**
     * Deletes the named {@link Policy} from the database.
     *
     * @param policy      {@link Policy} to be deleted
     * @return              <code>true</code> if the {@link Policy} was successfully found and
     *                      deleted, <code>false</code> if the {@link Policy} was not found
     * @throws DBManagerException
     *      if the {@link Policy} is not an instance of ArchivableEntity.
     */
    boolean delete(Policy policy) throws DBManagerException;

    /**
     * Sets the default <code>Policy</code> for an <code>Appraiser</code>. The
     * default policy is used by an appraiser when a specific policy has not
     * been set for a platform.
     * <p>
     * In this current release a specific policy for a platform cannot yet be
     * set, so <code>Appraiser</code>s can only call
     * {@link #getDefaultPolicy(Appraiser)} to retrieve the <code>Policy</code>
     * for a platform.
     * <p>
     * The default policy can be unset by using null for the <code>policy</code>
     * parameter. In that case future calls to get the default policy will
     * return null.
     *
     * @param appraiser
     *            appraiser
     * @param policy
     *            default policy
     */
    void setDefaultPolicy(Appraiser appraiser, Policy policy);

    /**
     * Returns the default <code>Policy</code> for an <code>Appraiser</code>. If
     * the default policy has not been set then this will return null.
     *
     * @param appraiser
     *            appraiser
     * @return default policy or null if not set
     */
    Policy getDefaultPolicy(Appraiser appraiser);

    /**
     * Returns the <code>Policy</code> set for the <code>Appraiser</code> and
     * <code>Device</code>. If the policy has not been set, this will return
     * null.
     *
     * @param appraiser
     *            appraiser
     * @param device
     *            device
     * @return policy or null if not set
     */
    Policy getPolicy(Appraiser appraiser, Device device);

    /**
     * Returns the <code>Policy</code> set for the <code>Appraiser</code> and
     * <code>DeviceGroup</code>. If the policy has not been set, this will
     * return null.
     *
     * @param appraiser
     *            appraiser
     * @param deviceGroup
     *            deviceGroup
     * @return policy or null if not set
     */
    Policy getPolicy(Appraiser appraiser, DeviceGroup deviceGroup);

    /**
     * Sets the <code>Policy</code> for the <code>Appraiser</code> and
     * <code>DeviceGroup</code>. See {@link #getPolicy(Appraiser, DeviceGroup)}
     * for more details on the algorithm used. Policy can be null to remove
     * the policy for the appraiser-deviceGroup pair, which will retrieve the
     * default policy instead.
     *
     * @param appraiser
     *            appraiser
     * @param deviceGroup
     *            deviceGroup
     * @param policy
     *            policy
     */
    void setPolicy(Appraiser appraiser, DeviceGroup deviceGroup, Policy policy);

    /**
     * Retrieves the <code>Policy</code> identified by <code>name</code>. If
     * the <code>Policy</code> cannot be found then null is returned.  This method
     * loads all components of the Policy; for example, this is necessary when exporting
     * a policy. If the <code>Policy</code> cannot be found then null is returned.
     *
     * @param name
     *            name of the <code>Policy</code>
     * @return <code>Policy</code> whose name is <code>name</code> or null if
     *         not found
     * @throws PolicyManagerException
     *             if unable to retrieve the Policy
     */
    Policy getCompletePolicy(String name) throws PolicyManagerException;

    /**
     * Count the number of <code>DeviceGroup</code>s which use the given
     * <code>Policy</code>.
     *
     * @param policy the Policy to investigate.
     * @return int of the number of groups that are using the policy.
     */
    int getGroupCountForPolicy(Policy policy);
}
