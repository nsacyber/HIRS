package hirs.attestationca.persist;

import hirs.FilteredRecordsList;
import hirs.data.persist.DeviceGroup;
import hirs.data.persist.Policy;
import hirs.persist.CriteriaModifier;
import hirs.persist.DBManagerException;
import hirs.persist.DeviceGroupManager;
import hirs.persist.DeviceGroupManagerException;
import hirs.persist.PolicyMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class defines the <code>DBDeviceGroupManager</code> that is used to
 * store <code>DeviceGroup</code>s in the database.
 */
@Service
public class DBDeviceGroupManager extends DBManager<DeviceGroup> implements DeviceGroupManager {

    private static final Logger LOGGER = LogManager.getLogger(DBDeviceGroupManager.class);

    /**
     * Creates a new <code>DBDeviceGroupManager</code> and sets the
     * <code>SessionFactory</code> to the given instance.
     *
     * @param factory session factory used to access database connections
     */
    public DBDeviceGroupManager(final SessionFactory factory) {
        super(DeviceGroup.class, factory);
    }

    /**
     * Saves the <code>DeviceGroup</code> in the database. This creates a new
     * database session and saves the device group. If the
     * <code>DeviceGroup</code> had been previously saved, then a
     * <code>DeviceGroupManagerException</code> is thrown.
     *
     * @param deviceGroup
     *            device group to save
     * @return reference to saved device group
     * @throws hirs.persist.DeviceGroupManagerException
     *             if device group had been previously saved or an error occurs
     *             while trying to save it to the database
     */
    @Override
    public final DeviceGroup saveDeviceGroup(final DeviceGroup deviceGroup)
            throws DeviceGroupManagerException {
        LOGGER.debug("saving device group: {}", deviceGroup);
        try {
            return super.save(deviceGroup);
        } catch (DBManagerException e) {
            throw new DeviceGroupManagerException(e);
        }
    }

    /**
     * Updates a <code>DeviceGroup</code>. This updates the database entries to
     * reflect the new values that should be set.
     *
     * @param deviceGroup
     *            device group
     * @throws DeviceGroupManagerException
     *             if device group has not been previously saved or an error
     *             occurs while trying to save it to the database
     *
     */
    @Override
    public final void updateDeviceGroup(final DeviceGroup deviceGroup)
            throws DeviceGroupManagerException {
        LOGGER.debug("updating device group: {}", deviceGroup);
        try {
            super.update(deviceGroup);
        } catch (DBManagerException e) {
            throw new DeviceGroupManagerException(e);
        }
    }

    /**
     * Returns a set of all <code>DeviceGroup</code>s.
     *
     * @return set of <code>DeviceGroup</code>s
     * @throws DeviceGroupManagerException
     *             if unable to search the database
     */
    @Override
    public final Set<DeviceGroup> getDeviceGroupSet()
            throws DeviceGroupManagerException {
        LOGGER.debug("getting device group list");
        try {
            final List<DeviceGroup> deviceGroupList =
                    super.getList(DeviceGroup.class);
            return new HashSet<>(deviceGroupList);
        } catch (DBManagerException e) {
            throw new DeviceGroupManagerException(e);
        }
    }

    /**
     * Retrieves a <code>DeviceGroup</code> from the database. This searches the
     * database for an entry whose name matches <code>name</code>.
     *
     * @param name
     *            name of the device group
     * @return device group if found, otherwise null
     * @throws DeviceGroupManagerException
     *             if unable to search the database
     */
    @Override
    public final DeviceGroup getDeviceGroup(final String name)
            throws DeviceGroupManagerException {
        LOGGER.debug("getting device group: {}", name);
        try {
            return super.get(name);
        } catch (DBManagerException e) {
            throw new DeviceGroupManagerException(e);
        }
    }

    /**
     * Checks whether or not a {@link Policy} is currently associated with
     * a group.  The only instance at this time makes a determination whether
     * or not the provided Policy is safe for deletion.
     *
     * @param policy
     *      {@link Policy} that has been selected for deletion.
     * @return
     *      whether or not the provided policy is the member of a group
     * @throws DeviceGroupManagerException
     *             if policy is null or unable to return query {@link Policy}
     */
    @Override
    public final Set<DeviceGroup> getGroupsAssignedToPolicy(final Policy policy)
            throws DeviceGroupManagerException {

        if (policy == null) {
            LOGGER.error("policy provided was null");
            throw new DeviceGroupManagerException("policy provided was null");
        }

        final SessionFactory factory = getFactory();
        Set<DeviceGroup> groups = new HashSet<>();

        Session session = factory.getCurrentSession();
        Transaction tx = session.beginTransaction();

        try {
            LOGGER.debug("retrieving policy mapper from db where policy = {}", policy);
            CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
            CriteriaQuery<PolicyMapper> criteriaQuery = criteriaBuilder
                    .createQuery(PolicyMapper.class);
            Root<PolicyMapper> root = criteriaQuery.from(PolicyMapper.class);
            Predicate recordPredicate = criteriaBuilder.and(
                    criteriaBuilder.equal(root.get("policy"), policy));
            criteriaQuery.select(root).where(recordPredicate);
            Query<PolicyMapper> query = session.createQuery(criteriaQuery);
            List<PolicyMapper> results = query.getResultList();

            //Retrieves a list of PolicyMapper objects that are unique per group
//            List policyMapperList = session.createCriteria(PolicyMapper.class)
//                    .add(Restrictions.eq("policy", policy)).list();

            session.getTransaction().commit();

            if (results == null) {
                LOGGER.debug("no policy mapper found for policy {}", policy);

            } else {
                for (PolicyMapper policyMapper : results) {
                    groups.add(policyMapper.getDeviceGroup());
                }
            }
        } catch (Exception e) {

            final String msg = "unable to get default policy";
            LOGGER.error(msg, e);

            LOGGER.debug("rolling back transaction");
            tx.rollback();

            throw new DeviceGroupManagerException(msg, e);

        }
        return groups;
    }
    /**
     * Deletes the <code>DeviceGroup</code> from the database. This removes all
     * of the database entries that stored information with regards to the
     * <code>DeviceGroup</code>.
     * <p>
     * If the <code>DeviceGroup</code> is referenced by any other tables then
     * this will throw a <code>DeviceGroupManagerException</code>.
     *
     * @param name
     *            name of the device group
     * @return true if successfully found and deleted, false if otherwise
     * @throws DeviceGroupManagerException
     *             if unable to find the device group or delete it from the
     *             database
     */
    @Override
    public final boolean deleteDeviceGroup(final String name)
            throws DeviceGroupManagerException {
        LOGGER.debug("deleting device group: {}", name);
        try {
            return super.delete(name);
        } catch (DBManagerException e) {
            throw new DeviceGroupManagerException(e);
        }
    }

    /**
     * Returns a list of all <code>Device</code>s that are ordered by a column
     * and direction (ASC, DESC) that is provided by the user.  This method
     * helps support the server-side processing in the JQuery DataTables.
     *
     * @param columnToOrder Column to be ordered
     * @param ascending direction of sort
     * @param firstResult starting point of first result in set
     * @param maxResults total number we want returned for display in table
     * @param search string of criteria to be matched to visible columns
     *
     * @return FilteredRecordsList object with fields for DataTables
     * @throws DeviceGroupManagerException
     *          if unable to create the list
     */
    @Override
    public final FilteredRecordsList<DeviceGroup> getOrderedDeviceGroupList(
            final String columnToOrder, final boolean ascending, final int firstResult,
            final int maxResults, final String search)
            throws DeviceGroupManagerException {

        if (columnToOrder == null) {
            LOGGER.debug("null object argument");
            throw new NullPointerException("object");
        }

        //Maps object types and their ability to be searched by Hibernate
        //without modification
        Map<String, Boolean> searchableColumns = new HashMap<>();
        searchableColumns.put("name", true);
        searchableColumns.put("description", true);

        CriteriaModifier modifier = new CriteriaModifier() {
            @Override
            public void modify(final Criteria criteria) {
                //criteria.createAlias("deviceGroup", "group");
            }
        };

        try {
            LOGGER.debug("Getting baseline list");
            return super.getOrderedList(DeviceGroup.class, columnToOrder, ascending, firstResult,
                    maxResults, search, searchableColumns, modifier);
        } catch (DBManagerException e) {
            LOGGER.error(e);
            return null;
        }
    }
}
