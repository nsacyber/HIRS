package hirs.persist.service;

import hirs.appraiser.Appraiser;
import hirs.data.persist.policy.Policy;
import hirs.persist.OrderedQuery;

import java.util.UUID;

/**
 * A <code>PolicyService</code> manages <code>Policy</code>s. A
 * <code>PolicyService</code> is used to store and manage policies. It has
 * support for the basic create, read, update, and delete methods.
 */
public interface PolicyService extends OrderedQuery<Policy> {

    /**
     * Saves the <code>Policy</code> in the database. This creates a new
     * database session and saves the policy.
     *
     * @param policy Policy to save
     * @return reference to saved policy
     */
    Policy savePolicy(Policy policy);

    /**
     * Updates a <code>Policy</code>. This updates the database entries to
     * reflect the new values that should be set.
     *
     * @param policy Policy object to save
     * @param uuid UUID for the database object
     * @return a Policy object
     */
    Policy updatePolicy(Policy policy, UUID uuid);

    /**
     * Returns the default <code>Policy</code> for the <code>Appraiser</code>.
     * If the default <code>Policy</code> has not been set then this returns
     * null.
     *
     * @param appraiser
     *            appraiser
     * @return default policy
     */
    Policy getDefaultPolicy(Appraiser appraiser);

    /**
     * Retrieves the <code>Policy</code> from the database. This searches the
     * database for an entry whose name matches <code>name</code>. It then
     * reconstructs a <code>Policy</code> object from the database entry
     *
     * @param name  name of the policy
     * @return policy if found, otherwise null.
     */
    Policy getPolicyByName(String name);

    /**
     * Sets the <code>Policy</code> for the <code>Appraiser</code>.
     *
     * @param appraiser  appraiser
     * @param policy   policy
     */
    void setPolicy(Appraiser appraiser, Policy policy);

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
     * @param appraiser  appraiser
     * @param policy default policy
     */
    void setDefaultPolicy(Appraiser appraiser, Policy policy);
}
