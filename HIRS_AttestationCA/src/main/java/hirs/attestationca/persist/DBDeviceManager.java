package hirs.attestationca.persist;

import hirs.FilteredRecordsList;
import hirs.data.persist.Device;
import hirs.data.persist.DeviceGroup;
import hirs.persist.CriteriaModifier;
import hirs.persist.DBManagerException;
import hirs.persist.DeviceManager;
import hirs.persist.DeviceManagerException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class defines a <code>DeviceManager</code> that stores the devices
 * in a database.
 */
@Service
public class DBDeviceManager extends DBManager<Device> implements
        DeviceManager {

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Creates a new <code>DBDeviceManager</code> that uses the default
     * database. The default database is used to store all of the
     * <code>Device</code>s.
     *
     * @param sessionFactory session factory used to access database connections
     */
    public DBDeviceManager(final SessionFactory sessionFactory) {
        super(Device.class, sessionFactory);
    }

    /**
     * Saves the <code>Device</code> in the database. This creates a new
     * database session and saves the device. If the <code>Device</code> had
     * previously been saved then a <code>DeviceManagerException</code> is
     * thrown.
     *
     * @param device
     *            device to save
     * @return reference to saved device
     * @throws hirs.persist.DeviceManagerException
     *             if device has previously been saved or an error occurs
     *             while trying to save it to the database
     */
    @Override
    public final Device saveDevice(final Device device)
            throws DeviceManagerException {
        LOGGER.debug("saving device: {}", device);
        try {
            return super.save(device);
        } catch (DBManagerException e) {
            throw new DeviceManagerException(e);
        }
    }

    /**
     * Updates a <code>Device</code>. This updates the database entries to
     * reflect the new values that should be set.
     *
     * @param device
     *            device
     * @throws DeviceManagerException
     *             if device has not previously been saved or an error occurs
     *             while trying to save it to the database
     */
    @Override
    public final void updateDevice(final Device device)
            throws DeviceManagerException {
        LOGGER.debug("updating device: {}", device);
        try {
            super.update(device);
        } catch (DBManagerException e) {
            throw new DeviceManagerException(e);
        }
    }

    /**
     * Updates list of <code>Device</code>s. This updates the database entries
     * to reflect the new values that should be set.  Commonly used when
     * deleting a DeviceGroup.
     *
     * @param deviceList
     *            list of devices that should be updated in single transaction
     * @throws DeviceManagerException
     *             if device has not previously been saved or an error occurs
     *             while trying to save it to the database
     */
    @Override
    public final void updateDeviceList(final Set<Device> deviceList)
            throws DeviceManagerException {
        LOGGER.debug("updating all devices in list");

        Session session = getFactory().getCurrentSession();
        Transaction tx = session.beginTransaction();
        try {
            for (final Device device : deviceList) {
                session.merge(device);
            }
            session.getTransaction().commit();
        } catch (Exception e) {
            final String msg = "unable to update all devices in list";
            LOGGER.error(msg, e);
            LOGGER.debug("rolling back transaction");
            tx.rollback();
            throw new DBManagerException(msg, e);
        }
    }

    /**
     * Returns a list of all <code>Devices</code>. This searches through
     * the database for this information.
     *
     * @return list of <code>Devices</code>
     * @throws DeviceManagerException
     *             if unable to search the database
     */
    @Override
    public final Set<Device> getDeviceList() throws DeviceManagerException {
        LOGGER.debug("getting device list");

        try {
            final List<Device> devices = super.getList(Device.class);
            return new HashSet<>(devices);
        } catch (DBManagerException e) {
            throw new DeviceManagerException(e);
        }
    }

    /**
     * Returns a list of all <code>Device</code> names. This searches through
     * the database for this information.
     *
     * @return list of <code>Device</code> names
     * @throws DeviceManagerException
     *             if unable to search the database
     */
    @Override
    public final List<String> getDeviceNameList()
            throws DeviceManagerException {
        LOGGER.debug("getting device list");
        List<String> deviceNames = new LinkedList<>();
        try {
            final List<Device> devices = super.getList(Device.class);
            for (Device b : devices) {
                deviceNames.add(b.getName());
            }
        } catch (DBManagerException e) {
            throw new DeviceManagerException(e);
        }
        return deviceNames;
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
     * @throws DeviceManagerException
     *          if unable to create the list
     */
    @Override
    public final FilteredRecordsList<Device> getOrderedDeviceList(
            final String columnToOrder, final boolean ascending, final int firstResult,
            final int maxResults, final String search)
            throws DeviceManagerException {

        if (columnToOrder == null) {
            LOGGER.debug("null object argument");
            throw new NullPointerException("object");
        }

        //Maps object types and their ability to be searched by Hibernate
        //without modification
        Map<String, Boolean> searchableColumns = new HashMap<>();
        searchableColumns.put("name", true);
        searchableColumns.put("group.name", true);
        searchableColumns.put("last_report_timestamp", false);

        CriteriaModifier modifier = new CriteriaModifier() {
            @Override
            public void modify(final Criteria criteria) {
                criteria.createAlias("deviceGroup", "group");
            }
        };

        try {
            LOGGER.debug("Getting baseline list");
            return super.getOrderedList(Device.class, columnToOrder, ascending, firstResult,
                    maxResults, search, searchableColumns, modifier);
        } catch (DBManagerException e) {
            LOGGER.error(e);
            return null;
        }
    }
    /**
     * Retrieves the <code>Device</code> from the database. This searches the
     * database for an entry whose name matches <code>name</code>. It then
     * reconstructs a <code>Device</code> object from the database entry
     *
     * @param name
     *            name of the device
     * @return device if found, otherwise null.
     * @throws DeviceManagerException
     *             if unable to search the database or recreate the
     *             <code>Device</code>
     */
    @Override
    public final Device getDevice(final String name)
            throws DeviceManagerException {
        LOGGER.debug("getting device: {}", name);
        try {
            return super.get(name);
        } catch (DBManagerException e) {
            throw new DeviceManagerException(e);
        }
    }

    /**
     * Used to produce a list of all <code>Device</code>s associated with the Default Group.
     *
     * @return list of Devices that are part of the Default Group
     * @throws DeviceManagerException
     *      if unable to find the device or delete it from the database
     */
    @Override
    public final List<Device> getDefaultDevices() throws DeviceManagerException {
        Transaction tx = null;

        Session session = getFactory().getCurrentSession();
        List<Device> devices = new ArrayList<>();
        try {
            LOGGER.debug("retrieving defaults devices from db");
            tx = session.beginTransaction();

            CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
            CriteriaQuery<Device> criteriaQuery = criteriaBuilder.createQuery(Device.class);
            Root<Device> root = criteriaQuery.from(Device.class);
            root.join("group.name", JoinType.LEFT).alias("group");
            Predicate recordPredicate = criteriaBuilder
                    .and(criteriaBuilder.equal(root.get("group.name"), DeviceGroup.DEFAULT_GROUP));
            criteriaQuery.select(root).where(recordPredicate).distinct(true);
            Query<Device> query = session.createQuery(criteriaQuery);
            List<Device> results = query.getResultList();
            if (results != null) {
                devices.addAll(results);
            }
//            List list = session.createCriteria(Device.class).createAlias("deviceGroup", "group")
//                    .add(Restrictions.eq("group.name", DeviceGroup.DEFAULT_GROUP))
//                    .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY)
//                    .list();

            tx.commit();
        } catch (HibernateException e) {
            final String msg = "unable to retrieve default devices";
            LOGGER.error(msg, e);
            if (tx != null) {
                LOGGER.debug("rolling back transaction");
                tx.rollback();
            }
            throw e;
        }

        return devices;
    }

    /**
     * Deletes the <code>Device</code> from the database. This removes all
     * of the database entries that stored information with regards to the
     * <code>Device</code> with a foreign key relationship.
     *
     * @param name of the device to be deleted
     * @return true if successfully found and deleted, false if otherwise
     * @throws hirs.persist.DeviceGroupManagerException
     *             if unable to find the device group or delete it from the
     *             database
     */
    @Override
    public final boolean deleteDevice(final String name)
            throws DeviceManagerException {
        LOGGER.debug("deleting device: {}", name);
        try {
            return super.delete(name);
        } catch (DBManagerException e) {
            throw new DeviceManagerException(e);
        }
    }

}
