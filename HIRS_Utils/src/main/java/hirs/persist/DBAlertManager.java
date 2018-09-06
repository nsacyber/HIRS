package hirs.persist;

import hirs.FilteredRecordsList;
import static org.apache.logging.log4j.LogManager.getLogger;

import hirs.data.persist.Alert;
import hirs.data.persist.Baseline;
import hirs.data.persist.Device;
import hirs.data.persist.DeviceGroup;
import hirs.data.persist.Policy;

import java.util.ArrayList;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import hirs.data.persist.Report;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Property;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.Subqueries;

/**
 * This class defines a <code>AlertManager</code> that stores policies in a
 * database.
 */
public class DBAlertManager extends DBManager<Alert> implements AlertManager {
    private static final Logger LOGGER = getLogger(DBAlertManager.class);


    /**
     * Creates a new <code>DBAlertManager</code>. The optional SessionFactory
     * parameter is used to initialize a session factory to manage all hibernate
     * sessions.
     *
     * @param factory session factory to manage connections to hibernate db
     */
    public DBAlertManager(final SessionFactory factory) {
        super(Alert.class, factory);
    }

    /**
     * Saves the <code>Alert</code> in the database and returns it.
     *
     * @param alert
     *            alert to save
     * @return <code>Alert</code> that was saved
     * @throws AlertManagerException
     *             if alert has previously been saved or an error occurs while
     *             trying to save it to the database
     */
    @Override
    public final Alert saveAlert(final Alert alert)
            throws AlertManagerException {
        LOGGER.debug("saving alert: {}", alert);
        try {
            return super.save(alert);
        } catch (DBManagerException e) {
            throw new AlertManagerException(e);
        }
    }

    /**
     * Updates all of the {@link Alert}s provided in the list.
     *
     * @param alerts                    list of alerts to be updated
     * @return                          list of updated Alerts
     * @throws AlertManagerException
     *          if unable to update the list of Alerts
     */
    @Override
    public final List<Alert> updateAlerts(final List<Alert> alerts) throws AlertManagerException {
        LOGGER.debug("updating object");
        if (alerts == null) {
            LOGGER.debug("null object argument");
            throw new NullPointerException("object");
        }

        Transaction tx = null;
        List<Alert> updatedAlerts = new ArrayList<>();
        Session session = getFactory().getCurrentSession();
        try {
            LOGGER.debug("updating object in db");
            tx = session.beginTransaction();
            for (Alert alert : alerts) {
                updatedAlerts.add((Alert) session.merge(alert));
            }
            tx.commit();
        } catch (Exception e) {
            final String msg = "unable to update alert";
            LOGGER.error(msg, e);
            if (tx != null) {
                LOGGER.debug("rolling back transaction");
                tx.rollback();
            }
            throw new AlertManagerException(msg, e);
        }

        return updatedAlerts;
    }

    /**
     * Returns a list of all <code>Alert</code>s.
     * This searches through the database for this information.
     *
     * @return list of <code>Alert</code>s
     * @throws AlertManagerException
     *             if unable to search the database
     */
    @Override
    public final List<Alert> getAlertList() throws AlertManagerException {
        LOGGER.debug("getting alert list");
        try {
            return super.getList(Alert.class);
        } catch (DBManagerException e) {
            throw new AlertManagerException(e);
        }
    }

    /**
     * Returns a list of all <code>Alert</code>s that relate to the provided <code>Report</code>
     * ID. If the given reportId is null or an empty, alerts for all reports are provided. The
     * Alerts are ordered by a column and direction (ASC, DESC) that is provided by the user.
     * This method helps support the server-side processing in the JQuery DataTables. Alerts with a
     * non-null archivedTime are not included in the returned list.
     *
     * @param reportId ID of the Report to return Alerts from, empty or null for all Alerts
     * @param columnToOrder Column to be ordered
     * @param ascending direction of sort
     * @param firstResult starting point of first result in set
     * @param maxResults total number we want returned for display in table
     * @param search string of criteria to be matched to visible columns
     * @param listType enumeration indicating if the returned list conatins resolved or
     *                 unresolved alerts
     * @param searchableColumns Map of String and boolean values with column
     *      headers and whether they are to.  Boolean is true if field provides
     *      a typical String that can be searched by Hibernate without
     *      transformation.
     * @param beginDate the earliest date of any alert returned from this method. Can be null.
     * @param endDate the latest date of any alert returned from this method. Can be null.
     * @return FilteredRecordsList object with fields for DataTables
     * @throws AlertManagerException
     *          if unable to create the list
     */
    @Override
    @SuppressWarnings("checkstyle:parameternumber")
    public final FilteredRecordsList<Alert> getOrderedAlertList(
            final String reportId, final String columnToOrder, final boolean ascending,
            final int firstResult, final int maxResults, final String search,
            final AlertListType listType,
            final Map<String, Boolean> searchableColumns, final Date beginDate, final Date endDate)
            throws AlertManagerException {

        if (columnToOrder == null) {
            LOGGER.debug("null object argument");
            throw new NullPointerException("object");
        }

        // verify that the report ID is a legit UUID
        if (StringUtils.isNotEmpty(reportId)) {
            try {
                UUID.fromString(reportId);
            } catch (IllegalArgumentException iae) {
                throw new AlertManagerException(reportId + " is not a valid UUID", iae);
            }
        }

        final FilteredRecordsList<Alert> alerts;

        // check that the alert is not archived and that it is in the specified report
        CriteriaModifier modifier = new CriteriaModifier() {
            @Override
            public void modify(final Criteria criteria) {
                if (listType == AlertListType.RESOLVED_ALERTS) {
                    criteria.add(Restrictions.isNotNull("archivedTime"));
                } else {
                    criteria.add(Restrictions.isNull("archivedTime"));
                }

                // filter by date, if specified
                if (null != beginDate) {
                    criteria.add(Restrictions.ge("createTime", beginDate));
                }

                if (null != endDate) {
                    criteria.add(Restrictions.le("createTime", endDate));
                }

                if (StringUtils.isNotEmpty(reportId)) {
                    // creating a separate criteria associated with the report field is necessary
                    // for this to work in HSQL and MySQL to avoid column ambiguity.
                    Criteria reportCriteria = criteria.createCriteria("report");
                    reportCriteria.add(Restrictions.eq("id", UUID.fromString(reportId)));
                }
            }
        };

        try {
            LOGGER.debug("querying db for alerts");
            alerts = super.getOrderedList(Alert.class, columnToOrder, ascending, firstResult,
                    maxResults, search, searchableColumns, modifier);
        } catch (DBManagerException e) {
            throw new AlertManagerException(e);
        }
        return alerts;
    }

    /**
     * Retrieves the <code>Alert</code> from the database. This searches the
     * database for an entry whose name matches <code>name</code>. It then
     * reconstructs a <code>Alert</code> object from the database entry
     *
     * @param id
     *            id of the <code>Alert</code>
     * @return alert if found, otherwise null.
     * @throws AlertManagerException
     *             if unable to search the database or recreate the
     *             <code>Alert</code>
     */
    @Override
    public final Alert getAlert(final UUID id) throws AlertManagerException {
        LOGGER.debug("getting alert: {}", id);
        try {
            return super.get(id);
        } catch (DBManagerException e) {
            throw new AlertManagerException(e);
        }
    }

    /**
     * Retrieves all {@link Alert}s associated with the provided {@link Policy}.
     *
     * @param policy                  policy that is being evaluated
     * @return                        list of all alerts associated with {@link Policy}
     * @throws AlertManagerException
     *      If there is a query error
     */
    @Override
    public final List<Alert> getAlertsForPolicy(final Policy policy)
            throws AlertManagerException {
        LOGGER.debug("");

        if (policy == null) {
            throw new NullPointerException("policy was null");
        }

        List<Alert> alerts = new ArrayList<>();
        Transaction tx = null;
        Session session = getFactory().getCurrentSession();
        try {
            tx = session.beginTransaction();

            // query hibernate to count alerts with the given deviceName and null archivedTime
            Criteria criteria = session.createCriteria(Alert.class);
            criteria.add(Restrictions.eq("policyId", policy.getId()));
            criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);

            List list = criteria.list();
            for (Object o : list) {
                if (o instanceof Alert) {
                    alerts.add((Alert) o);
                }
            }
            tx.commit();

        } catch (HibernateException e) {
            final String msg = "unable to query alerts table";
            LOGGER.error(msg, e);
            if (tx != null) {
                LOGGER.debug("rolling back transaction");
                tx.rollback();
            }
            throw e;
        }

        return alerts;
    }

    /**
     * Retrieves all {@link Alert}s associated with the provided {@link Baseline}.
     *
     * @param baseline                  baseline that is being evaluated
     * @return                          list of all alerts associated with {@link Baseline}
     * @throws AlertManagerException
     *      If there is a query error
     */
    @Override
    public final List<Alert> getAlertsForBaseline(final Baseline baseline)
            throws AlertManagerException {
        LOGGER.debug("");

        if (baseline == null) {
            throw new NullPointerException("baseline was null");
        }

        List<Alert> alerts = new ArrayList<>();
        Transaction tx = null;
        Session session = getFactory().getCurrentSession();
        try {
            tx = session.beginTransaction();

            // query hibernate to retrieve alerts with the given baseline id
            Criteria criteria = session.createCriteria(Alert.class);
            criteria.add(Restrictions.eq("baselineId", baseline.getId()));
            criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
            List list = criteria.list();
            for (Object o : list) {
                if (o instanceof Alert) {
                    alerts.add((Alert) o);
                }
            }
            tx.commit();

        } catch (HibernateException e) {
            final String msg = "unable to query alerts table";
            LOGGER.error(msg, e);
            if (tx != null) {
                LOGGER.debug("rolling back transaction");
                tx.rollback();
            }
            throw e;
        }

        return alerts;
    }

    /**
     * Retrieves the total number of Unresolved {@link Alert}s associated with the provided
     * {@link Baseline}.
     *
     * @param baseline                  baseline that is being evaluated
     * @return                          number of unresolved alerts associated with Baseline
     * @throws AlertManagerException
     *      If there is a query error
     */
    @Override
    public final long getTotalAlertsForBaseline(final Baseline baseline)
            throws AlertManagerException {
        LOGGER.debug("");

        if (baseline == null) {
            throw new NullPointerException("baseline was null");
        }

        Transaction tx = null;
        Session session = getFactory().getCurrentSession();
        try {
            tx = session.beginTransaction();

            // query hibernate to count alerts with the given deviceName and null archivedTime
            Criteria criteria = session.createCriteria(Alert.class);
            criteria.add(Restrictions.isNull("archivedTime"));
            criteria.add(Restrictions.eq("baselineId", baseline.getId()));
            criteria.setProjection(Projections.rowCount()).uniqueResult();
            Long result = (Long) criteria.uniqueResult();
            tx.commit();
            if (result == null) {
                throw new AlertManagerException("failed to query unresolved alert count");
            } else {
                return result;
            }

        } catch (HibernateException e) {
            final String msg = "unable to query alerts table";
            LOGGER.error(msg, e);
            if (tx != null) {
                LOGGER.debug("rolling back transaction");
                tx.rollback();
            }
            throw e;
        }
    }

    private Criteria getCriteriaForTrustAlertsForDevice(final Device device,
                                                        final Report integrityReport,
                                                        final Criterion optionalCriterion,
                                                        final Session session) {
        if (null == device) {
            throw new NullPointerException("device was null");
        }

        if (null == integrityReport) {
            throw new NullPointerException("integrityReport was null");
        }

        if (null == session) {
            throw new NullPointerException("session was null");
        }

        // query hibernate to count alerts with the given deviceName and null archivedTime
        Criteria criteria = session.createCriteria(Alert.class);
        criteria.add(Restrictions.isNull("archivedTime"));
        criteria.add(Restrictions.eq("deviceName", device.getName()));

        // alerts for this report, or alerts not associated with any report (case 1 and 2)
        Criterion reportEqualsCriterion = Restrictions.eq("report", integrityReport);
        Criterion reportNullCriterion = Restrictions.isNull("report");

        // only apply the optional criterion to the disjunction if there is one.
        if (null != optionalCriterion) {
            criteria.add(Restrictions.disjunction(
                    reportEqualsCriterion, reportNullCriterion, optionalCriterion));

        } else {
            criteria.add(Restrictions.disjunction(
                    reportEqualsCriterion, reportNullCriterion));
        }
        return criteria;
    }

    /**
     * Gets the set of alerts for a device in order to determine the status of
     * the device (trusted or untrusted).
     *
     * The alerts meet one or more of these specifications:
     * <ol>
     *   <li>Have no report associated (missed periodic report alerts) for this device</li>
     *   <li>Are associated with the provided integrity report</li>
     *   <li>Match the specified criteria. e.g. leftover alerts from
     *      delta reports in the current series of delta reports).</li>
     * </ol>
     * @param device the device to query for alerts on
     * @param integrityReport the integrity report to find associated alerts with
     * @param optionalCriterion the optional additional criteria for which to query on
     * @return the set of device alerts associated with trust
     */
    @Override
    public List<Alert> getTrustAlerts(final Device device, final Report integrityReport,
                                      final Criterion optionalCriterion) {
        LOGGER.debug("getting trust alerts for {}", device);

        Transaction tx = null;
        Session session = getFactory().getCurrentSession();
        try {
            tx = session.beginTransaction();

            Criteria criteria = getCriteriaForTrustAlertsForDevice(device, integrityReport,
                    optionalCriterion, session);

            List<Alert> alerts = new ArrayList<>();
            List list = criteria.list();
            for (Object o : list) {
                if (o instanceof Alert) {
                    alerts.add((Alert) o);
                }
            }
            tx.commit();
            return alerts;
        } catch (HibernateException | NullPointerException e) {
            final String msg = "unable to query alerts table";
            LOGGER.error(msg, e);
            if (tx != null) {
                LOGGER.debug("rolling back transaction");
                tx.rollback();
            }
            throw e;
        }
    }

    /**
     * Gets the count of trust alerts for a device.  See {@link #getTrustAlerts} for more
     * information about which alerts are counted.
     *
     * @param device the device to query for alerts on
     * @param integrityReport the integrity report to find associated alerts with
     * @param optionalCriterion the optional additional criteria for which to query on
     * @return the count of alerts associated with trust
     */
    @Override
    public final int getTrustAlertCount(final Device device, final Report integrityReport,
                                    final Criterion optionalCriterion) {
        LOGGER.debug("getting trust alert count for {}", device);

        Transaction tx = null;
        Session session = getFactory().getCurrentSession();
        try {
            tx = session.beginTransaction();

            Criteria criteria = getCriteriaForTrustAlertsForDevice(device, integrityReport,
                    optionalCriterion, session);

            criteria.setProjection(Projections.rowCount()).uniqueResult();
            Long result = (Long) criteria.uniqueResult();

            tx.commit();
            return result.intValue();
        } catch (HibernateException | NullPointerException e) {
            final String msg = "unable to query alerts table";
            LOGGER.error(msg, e);
            if (tx != null) {
                LOGGER.debug("rolling back transaction");
                tx.rollback();
            }
            throw e;
        }
    }

    /**
     * Overloads the resolveAlerts method and provides a null description for the
     * alert resolution.
     *
     * @param alerts - list of Alert objects to be marked as resolved.
     * @throws AlertManagerException
     *          if unable to save the list
     */
    @Override
    public final void resolveAlerts(final List<Alert> alerts)
            throws AlertManagerException {
        resolveAlerts(alerts, null);
    }

    /**
     * Marks all Alerts that are provided as arguments as resolved.  This is used as
     * a "soft delete" method and will ensure they no longer appear in the Alert
     * table on the Portal.
     *
     * @param alerts - Alert objects to be marked as resolved
     * @param description - description of action taken.  The description can be null
     * @throws AlertManagerException
     *          if unable to save the list
     */
    @Override
    public final void resolveAlerts(final List<Alert> alerts, final String description)
            throws AlertManagerException {
        if (alerts == null) {
            String message = "list of alert objects was null";
            LOGGER.debug(message);
            throw new NullPointerException(message);
        }

        LOGGER.debug("Marking " + alerts.size() + " alerts to resolved and saving.");

        Transaction tx = null;
        Session session = getFactory().getCurrentSession();
        try {
            LOGGER.debug("saving object in db");
            tx = session.beginTransaction();
            for (Alert alert : alerts) {
                alert.archive(description);
                session.merge(alert);
                LOGGER.info("Alert {} is marked as resolved.", alert.getId());
            }
            tx.commit();
        } catch (Exception e) {
            final String msg = "unable to save alert";
            LOGGER.error(msg, e);
            if (tx != null) {
                LOGGER.debug("rolling back transaction");
                tx.rollback();
            }
            throw new DBManagerException(msg, e);
        }
    }

    /**
     * Return the count of unresolved alerts associated with the given device.
     *
     * @param device associated with unresolved alerts being counted
     * @return count of unresolved alerts
     */
    public final int countUnresolvedAlerts(final Device device) {
        if (device == null) {
            LOGGER.warn("null device found, returning 0");
            return 0;
        }

        Transaction tx = null;
        Session session = getFactory().getCurrentSession();
        try {
            LOGGER.debug("querying alerts table for unresolved alerts");
            tx = session.beginTransaction();

            // query hibernate to count alerts with the given deviceName and null archivedTime
            Criteria criteria = session.createCriteria(Alert.class);
            criteria.add(Restrictions.isNull("archivedTime"));
            criteria.add(Restrictions.eq("deviceName", device.getName()));
            criteria.setProjection(Projections.rowCount()).uniqueResult();
            Long result = (Long) criteria.uniqueResult();
            tx.commit();
            if (result == null) {
                throw new AlertManagerException("failed to query unresolved alert count");
            } else {
                return result.intValue();
            }

        } catch (HibernateException e) {
            final String msg = "unable to query alerts table";
            LOGGER.error(msg, e);
            if (tx != null) {
                LOGGER.debug("rolling back transaction");
                tx.rollback();
            }
            throw e;
        }
    }

    /**
     * Return the count of unresolved alerts associated with the given device that originate from
     * the given AlertSource.
     *
     * @param device associated with unresolved alerts being counted
     * @param source counted alerts must originate from
     * @return count of unresolved alerts
     */
    public final int countUnresolvedAlerts(final Device device, final Alert.Source source) {
        if (device == null) {
            String msg = "invalid argument - null value for device";
            LOGGER.error(msg);
            throw new IllegalArgumentException(msg);
        }
        if (source == null) {
            String msg = "invalid argument - null value for source";
            LOGGER.error(msg);
            throw new IllegalArgumentException(msg);
        }

        Transaction tx = null;
        Session session = getFactory().getCurrentSession();
        try {
            LOGGER.debug("querying alerts table for unresolved alerts");
            tx = session.beginTransaction();

            Criteria criteria = session.createCriteria(Alert.class);
            criteria.add(Restrictions.isNull("archivedTime"));
            criteria.add(Restrictions.eq("deviceName", device.getName()));
            criteria.add(Restrictions.eq("source", source));
            criteria.setProjection(Projections.rowCount()).uniqueResult();
            Long result = (Long) criteria.uniqueResult();
            tx.commit();
            if (result == null) {
                throw new AlertManagerException("failed to query unresolved alert count");
            } else {
                return result.intValue();
            }

        } catch (HibernateException e) {
            final String msg = "unable to query alerts table";
            LOGGER.error(msg, e);
            if (tx != null) {
                LOGGER.debug("rolling back transaction");
                tx.rollback();
            }
            throw e;
        }
    }

    /**
     * Count the total number of devices with at least one unresolved alert within the given group.
     *
     * @param deviceGroup to count devices from
     * @return count of devices with unresolved alerts
     */
    public final int countUnresolvedDevices(final DeviceGroup deviceGroup) {

        if (deviceGroup == null) {
            String msg = "invalid argument - null value for device group";
            LOGGER.error(msg);
            throw new IllegalArgumentException(msg);
        }

        Transaction tx = null;
        Session session = getFactory().getCurrentSession();
        try {
            LOGGER.debug("querying alerts table for unresolved devices");
            tx = session.beginTransaction();

            // first use a subquery to list the devices in the given group
            DetachedCriteria deviceQuery = DetachedCriteria.forClass(Device.class);
            deviceQuery.createAlias("deviceGroup", "g");
            deviceQuery.add(Restrictions.eq("g.name", deviceGroup.getName()));
            deviceQuery.setProjection(Property.forName("name"));

            // now query within that group for unique device names among unresolved alerts
            Criteria criteria = session.createCriteria(Alert.class);
            criteria.add(Restrictions.isNull("archivedTime"));
            criteria.add(Subqueries.propertyIn("deviceName", deviceQuery));
            criteria.setProjection(Projections.countDistinct("deviceName"));
            Long result = (Long) criteria.uniqueResult();
            tx.commit();
            if (result == null) {
                throw new AlertManagerException("failed to query unresolved alert count");
            } else {
                return result.intValue();
            }

        } catch (HibernateException e) {
            final String msg = "unable to query alerts table";
            LOGGER.error(msg, e);
            if (tx != null) {
                LOGGER.debug("rolling back transaction");
                tx.rollback();
            }
            throw e;
        }
    }
}

