package hirs.persist;

import com.google.common.base.Preconditions;
import hirs.appraiser.Appraiser;
import hirs.data.persist.Baseline;
import hirs.data.persist.Device;
import hirs.data.persist.DeviceGroup;
import hirs.data.persist.HasBaselines;
import hirs.data.persist.Policy;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

/**
 * This class defines a <code>PolicyManager</code> that stores policies in a
 * database.
 */
public class DBPolicyManager extends DBManager<Policy> implements PolicyManager {

    private static final Logger LOGGER = LogManager.getLogger(DBPolicyManager.class);

    /**
     * Creates a new <code>DBPolicyManager</code>. The optional SessionFactory
     * parameter is used to initialize a session factory to manage all hibernate
     * sessions.
     *
     * @param factory session factory to manage connections to hibernate db
     */
    public DBPolicyManager(final SessionFactory factory) {
        super(Policy.class, factory);
    }

    /**
     * Saves the <code>Policy</code> in the database and returns it.
     *
     * @param policy
     *            policy to save
     * @return <code>Policy</code> that was saved
     * @throws PolicyManagerException
     *             if policy has previously been saved or an error occurs while
     *             trying to save it to the database
     */
    @Override
    public final Policy savePolicy(final Policy policy)
            throws PolicyManagerException {
        LOGGER.debug("saving policy: {}", policy);
        try {
            return super.save(policy);
        } catch (DBManagerException e) {
            throw new PolicyManagerException(e);
        }
    }

    /**
     * Updates a <code>Policy</code>. This updates the database entries to
     * reflect the new values that should be set.
     *
     * @param policy
     *            policy
     * @throws PolicyManagerException
     *             if policy has not previously been saved or an error occurs
     *             while trying to save it to the database
     */
    @Override
    public final void updatePolicy(final Policy policy)
            throws PolicyManagerException {
        LOGGER.debug("updating policy: {}", policy);
        try {
            super.update(policy);
        } catch (DBManagerException e) {
            throw new PolicyManagerException(e);
        }
    }

    /**
     * Returns a list of all non-archived <code>Policy</code>s of type <code>clazz</code> in the
     * database.
     *
     * @param clazz
     *            class type of <code>Policy</code>s to return (may be null)
     * @return list of <code>Policy</code>s
     * @throws PolicyManagerException if unable to search the database
     */
    @Override
    public final List<Policy> getPolicyList(final Class<? extends Policy> clazz)
            throws PolicyManagerException {
        LOGGER.debug("getting policy list");
        try {
            return super.getList(clazz, Restrictions.isNull("archivedTime"));
        } catch (DBManagerException e) {
            throw new PolicyManagerException(e);
        }
    }

    /**
     * Return a list of all the policies that contain the given baseline.
     *
     * @param clazz the class of Policy to search
     * @param baseline the baseline that should be a member of returned Policies
     * @return the list of matching Policies
     */
    @Override
    public List<Policy> getPoliciesContainingBaseline(
            final Class<? extends Policy> clazz,
            final Baseline baseline
    ) {
        List<Policy> matchingPolicies = new ArrayList<>();
        List<Policy> policies = getPolicyList(clazz);
        for (Policy policy : policies) {
            if (policy instanceof HasBaselines) {
                HasBaselines policyWithBaselines = (HasBaselines) policy;
                if (policyWithBaselines.getBaselines().contains(baseline)) {
                    matchingPolicies.add(policy);
                }
            }
        }
        return matchingPolicies;
    }

    /**
     * Retrieves the <code>Policy</code> from the database. This searches the
     * database for an entry whose name matches <code>name</code>. It then
     * reconstructs a <code>Policy</code> object from the database entry
     *
     * @param name
     *            name of the policy
     * @return policy if found, otherwise null.
     * @throws PolicyManagerException
     *             if unable to search the database or recreate the
     *             <code>Policy</code>
     */
    @Override
    public final Policy getPolicy(final String name)
            throws PolicyManagerException {
        LOGGER.debug("getting policy by name: {}", name);
        try {
            return super.get(name);
        } catch (DBManagerException e) {
            throw new PolicyManagerException(e);
        }
    }

    /**
     * Retrieves the <code>Policy</code> with this given id from the database. This searches the
     * database for an entry whose id matches <code>id</code>. It then reconstructs a
     * <code>Policy</code> object from the database response.
     *
     * @param id
     *            id of the desired policy
     * @return policy if found, otherwise null.
     * @throws PolicyManagerException
     *             if unable to search the database or retrieve the
     *             <code>Policy</code>
     */
    @Override
    public final Policy getPolicy(final Serializable id)
            throws PolicyManagerException {
        LOGGER.debug("getting policy by id: {}", id);
        try {
            return super.get(id);
        } catch (DBManagerException e) {
            throw new PolicyManagerException(e);
        }
    }

    /**
     * Sets the default <code>Policy</code> to use for an <code>Appraiser</code>.
     * This updates the database to reflect this change so that when this
     * class is loaded it should read that property.
     *
     * @param appraiser
     *            appraiser
     * @param policy
     *            default policy
     */
    @Override
    public final void setDefaultPolicy(final Appraiser appraiser,
            final Policy policy) {
        LOGGER.debug("set default policy");
        if (appraiser == null) {
            LOGGER.error("cannot set default policy on null appraiser");
            throw new NullPointerException("appraiser");
        }
        final SessionFactory factory = getFactory();
        Session session = factory.getCurrentSession();
        Transaction tx = session.beginTransaction();
        try {
            final Criteria criteria = session.createCriteria(DeviceGroup.class)
                    .add(Restrictions.eq("name", DeviceGroup.DEFAULT_GROUP));
            DeviceGroup group = (DeviceGroup) criteria.uniqueResult();
            LOGGER.debug("finding existing policy mapper from db where "
                    + "appraiser = {}", appraiser);
            final Criteria cr = session.createCriteria(PolicyMapper.class)
                    .createAlias("deviceGroup", "group")
                    .add(Restrictions.eq("appraiser", appraiser))
                    .add(Restrictions.eq("group.name", DeviceGroup.DEFAULT_GROUP));
            final PolicyMapper mapper = (PolicyMapper) cr.uniqueResult();
            if (policy == null) {
                LOGGER.debug("policy is null so removing policy");
                if (mapper != null) {
                    session.delete(mapper);
                }
            } else {
                LOGGER.info("setting default policy {} on appraiser {}",
                        policy, appraiser);
                if (mapper == null) {
                    session.save(new PolicyMapper(appraiser, policy, group));
                } else {
                    mapper.setPolicy(policy);
                    session.update(mapper);
                }
            }
            session.getTransaction().commit();
        } catch (Exception e) {
            final String msg = "unable to set default policy";
            LOGGER.error(msg, e);
            if (tx != null) {
                LOGGER.debug("rolling back transaction");
                tx.rollback();
            }
            throw new DBManagerException(msg, e);
        }
    }

    @Override
    public final Policy getCompletePolicy(final String name) throws PolicyManagerException {
        LOGGER.debug("getting policy: {}", name);
        try {
            return super.getAndLoadLazyFields(name, true);
        } catch (DBManagerException e) {
            throw new PolicyManagerException(e);
        }
    }

    /**
     * Returns the default <code>Policy</code> for the <code>Appraiser</code>.
     * If the default <code>Policy</code> has not been set then this returns
     * null.
     *
     * @param appraiser
     *            appraiser
     * @return default policy
     */
    @Override
    public final Policy getDefaultPolicy(final Appraiser appraiser) {
        if (appraiser == null) {
            LOGGER.error("cannot get default policy for null appraiser");
            return null;
        }

        Policy ret = null;
        final SessionFactory factory = getFactory();
        Transaction tx = null;
        Session session = factory.getCurrentSession();
        try {
            tx = session.beginTransaction();
            LOGGER.debug("retrieving policy mapper from db where appraiser = {}",
                    appraiser);
            final Criteria cr = session.createCriteria(PolicyMapper.class)
                    .createAlias("deviceGroup", "group")
                    .add(Restrictions.eq("appraiser", appraiser))
                    .add(Restrictions.eq("group.name", DeviceGroup.DEFAULT_GROUP));
            final PolicyMapper mapper = (PolicyMapper) cr.uniqueResult();
            if (mapper == null) {
                LOGGER.debug("no policy mapper found for appraiser {}",
                        appraiser);
            } else {
                ret = mapper.getPolicy();
            }
            session.getTransaction().commit();
        } catch (Exception e) {
            final String msg = "unable to get default policy";
            LOGGER.error(msg, e);
            if (tx != null) {
                LOGGER.debug("rolling back transaction");
                tx.rollback();
            }
            throw new DBManagerException(msg, e);
        }
        return ret;
    }

    /**
     * This method takes the device that is passed in and searches the database
     * for one with the same name. This ensures it retrieves the version most
     * recently saved. This way, an appraiser can reconstruct a
     * <code>Device</code> using only the name pulled from the deviceInfo from
     * the <code>IntegrityReport</code> without worrying about figuring out
     * which device group it belongs to - this method does the work of finding
     * the device group. If the policy has not been set for that particular
     * device group and appraiser pair, then this method will return null.
     *
     * @param appraiser
     *            appraiser
     * @param device
     *            device that needs only the correct name
     * @return policy associated with the appraiser-device group pair or null if
     *         there is none
     */
    @Override
    public final Policy getPolicy(
            final Appraiser appraiser,
            final Device device) {
        Preconditions.checkArgument(appraiser != null, "Appraiser must not be null");
        Preconditions.checkArgument(device != null, "Device must not be null");

        Policy ret = null;
        final SessionFactory factory = getFactory();
        Transaction tx = null;
        Session session = factory.getCurrentSession();
        try {
            tx = session.beginTransaction();
            LOGGER.debug("retrieving policy mapper from db where appraiser = "
                    + "{} and device= {}", appraiser, device);
            final Criteria deviceCr = session.createCriteria(Device.class)
                    .add(Restrictions.eq("name", device.getName()));
            final Device retrievedDevice = (Device) deviceCr.uniqueResult();
            DeviceGroup deviceGroup = null;
            if (retrievedDevice != null) {
                deviceGroup = retrievedDevice.getDeviceGroup();
            }
            final Criteria cr = session.createCriteria(PolicyMapper.class)
                    .add(Restrictions.eq("appraiser", appraiser))
                    .add(Restrictions.eq("deviceGroup", deviceGroup));
            final PolicyMapper mapper = (PolicyMapper) cr.uniqueResult();
            if (mapper == null) {
                LOGGER.debug("no policy mapper found for appraiser {} and "
                        + "device group {}", appraiser, deviceGroup);
            } else {
                ret = mapper.getPolicy();
            }
            session.getTransaction().commit();
        } catch (Exception e) {
            final String msg = "unable to get policy";
            LOGGER.error(msg, e);
            if (tx != null) {
                LOGGER.debug("rolling back transaction");
                tx.rollback();
            }
            throw new DBManagerException(msg, e);
        }

        return ret;
    }

    /**
     * This class retrieves a policy based on the appraiser and the device
     * group. If the policy has not been set for that particular device group
     * and appraiser pair, then this method will attempt to find the default
     * policy for the given appraiser. If neither the specific policy for the
     * device group or the default policy is found, null is returned.
     *
     * @param appraiser
     *            appraiser
     * @param deviceGroup
     *            device group
     * @return policy associated with the appraiser-device group pair or null if
     *         there is none
     */
    @Override
    public final Policy getPolicy(final Appraiser appraiser,
            final DeviceGroup deviceGroup) {
        if (appraiser == null) {
            LOGGER.error("cannot get policy for null appraiser");
            return null;
        }

        Policy ret = null;
        final SessionFactory factory = getFactory();
        Transaction tx = null;
        Session session = factory.getCurrentSession();
        try {
            tx = session.beginTransaction();
            LOGGER.debug("retrieving policy mapper from db where appraiser = "
                    + "{} and device group = {}", appraiser, deviceGroup);
            final Criteria cr = session.createCriteria(PolicyMapper.class)
                    .add(Restrictions.eq("appraiser", appraiser))
                    .add(Restrictions.eq("deviceGroup", deviceGroup));
            final PolicyMapper mapper = (PolicyMapper) cr.uniqueResult();
            if (mapper == null) {
                LOGGER.debug("no policy mapper found for appraiser {} and "
                    + "device group {}", appraiser, deviceGroup);
            } else {
                ret = mapper.getPolicy();
            }
            session.getTransaction().commit();
        } catch (Exception e) {
            final String msg = "unable to get policy";
            LOGGER.error(msg, e);
            if (tx != null) {
                LOGGER.debug("rolling back transaction");
                tx.rollback();
            }
            throw new DBManagerException(msg, e);
        }

        if (ret == null) {
            String groupName = "null";
            if (deviceGroup != null) {
                groupName = deviceGroup.getName();
            }
            final String msg = String.format("unable to find policy for appraiser '%s'"
                    + " for device group '%s'", appraiser.getName(), groupName);
            LOGGER.debug(msg);
        }

        return ret;
    }

    /**
     * Sets the <code>Policy</code> to use for an <code>Appraiser</code> and
     * <code>DeviceGroup</code> pair. This updates the database to reflect this
     * change so that when this class is loaded it should read that property.
     *
     * @param appraiser
     *            appraiser
     * @param deviceGroup
     *            device group
     * @param policy
     *            policy
     */
    @Override
    public final void setPolicy(final Appraiser appraiser,
            final DeviceGroup deviceGroup, final Policy policy) {
        Preconditions.checkNotNull(appraiser, "Cannot set policy on null appraiser");
        Preconditions.checkNotNull(deviceGroup, "Cannot set policy on null device group");

        final SessionFactory factory = getFactory();
        Transaction tx = null;
        Session session = factory.getCurrentSession();
        try {
            tx = session.beginTransaction();
            LOGGER.debug("Finding existing policy mapper from db where "
                    + "appraiser = {} and device group = {}", appraiser,
                    deviceGroup);
            final Criteria cr = session.createCriteria(PolicyMapper.class)
                    .add(Restrictions.eq("appraiser", appraiser))
                    .add(Restrictions.eq("deviceGroup", deviceGroup));
            final PolicyMapper mapper = (PolicyMapper) cr.uniqueResult();
            if (policy == null) {
                LOGGER.info("Policy is null, so removing policy from device group {}");
                if (mapper != null) {
                    session.delete(mapper);
                }
            } else {
                LOGGER.info("Setting policy {} on appraiser {} on device "
                    + "group {}", policy, appraiser, deviceGroup);
                if (mapper == null) {
                    session.save(new PolicyMapper(appraiser, policy,
                            deviceGroup));
                } else {
                    mapper.setPolicy(policy);
                    session.update(mapper);
                }
            }
            session.getTransaction().commit();
        } catch (Exception e) {
            final String msg = "Unable to set policy";
            LOGGER.error(msg, e);
            if (tx != null) {
                LOGGER.debug("Rolling back transaction");
                tx.rollback();
            }
            throw new PolicyManagerException(msg, e);
        }
    }

    /**
     * Count the number of <code>DeviceGroup</code>s which use the given policy.
     *
     * @param policy the Policy to investigate.
     * @return int the number of groups that are using the policy, or -1 if
     * there was an error.
     */
    public final int getGroupCountForPolicy(final Policy policy) {
        int count = 0;

        if (policy != null) {
            final SessionFactory factory = getFactory();
            Transaction tx = null;
            Session session = factory.getCurrentSession();
            try {
                tx = session.beginTransaction();
                LOGGER.debug("retrieving group use count for policy {}", policy);
                final Criteria cr = session.createCriteria(PolicyMapper.class)
                        .add(Restrictions.eq("policy", policy))
                        .setProjection(Projections.projectionList()
                                        .add(Projections.count("policy")));

                final Object result = cr.uniqueResult();
                if (result != null && result instanceof Long) {
                    count = ((Long) result).intValue();
                }
            } catch (Exception e) {
                // Log the error and return -1 to enable error handling.
                count = -1;
                final String msg =
                    "There was an error retrieving the group use count for a policy (ID: "
                            + policy.getId() + ", Name: " + policy.getName() + ").";
                LOGGER.error(msg, e);
            } finally {
                if (tx != null) {
                    tx.rollback();
                }
            }
        }

        return count;
    }
}
