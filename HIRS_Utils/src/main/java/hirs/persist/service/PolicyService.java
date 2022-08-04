package hirs.persist.service;

import hirs.appraiser.Appraiser;
import hirs.data.persist.policy.Policy;

import java.util.UUID;

/**
 * A <code>PolicyService</code> manages <code>Policy</code>s. A
 * <code>PolicyService</code> is used to store and manage policies. It has
 * support for the basic create, read, update, and delete methods.
 */
public interface PolicyService {

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
}
