package hirs.persist;

import hirs.FilteredRecordsList;
import hirs.data.persist.Alert;
import hirs.data.persist.Baseline;
import hirs.data.persist.Device;
import hirs.data.persist.DeviceGroup;
import hirs.data.persist.Policy;
import hirs.data.persist.Report;
import org.hibernate.criterion.Criterion;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;


/**
 * An <code>AlertManager</code> manages <code>Alert</code> objects. An
 * <code>AlertManager</code> is used to store and manage alerts. It has support
 * for the basic create and read methods.  Update and delete are being discussed
 * but will not be implemented at this time.
 */
public interface AlertManager {

    /**
     * Enumeration identify the type of list to return when querying for the ordered alert list.
     */
    enum AlertListType {
        /**
         * List will contain only resolved alerts.
         */
        RESOLVED_ALERTS,
        /**
         * List will contain only unresolved alerts.
         */
        UNRESOLVED_ALERTS
    }

    /**
     * Stores a new <code>Alert</code>. This stores a new
     * <code>Alert</code> to be managed by the <code>AlertManager</code>.
     * If the <code>Alert</code> is successfully saved then a reference to it
     * is returned.
     *
     * @param alert
     *            Alert to save
     * @return reference to saved Alert
     * @throws AlertManagerException
     *             if the Alert has previously been saved or unexpected error
     *             occurs
     */
    Alert saveAlert(Alert alert) throws AlertManagerException;

    /**
     * Updates all of the {@link Alert}s provided in the list.
     *
     * @param alerts                    list of alerts to be updated
     * @return                          list of updated Alerts
     * @throws AlertManagerException
     *          if unable to update the list of Alerts
     */
    List<Alert> updateAlerts(List<Alert> alerts) throws AlertManagerException;

    /**
     * Returns a list of all <code>Alert</code>s managed by this manager.
     *
     * @return list of all managed <code>Alert</code> objects
     * @throws AlertManagerException
     *             if unable to create the list
     */
    List<Alert> getAlertList() throws AlertManagerException;

    /**
     * Retrieves the <code>Alert</code> identified by <code>name</code>. If
     * the <code>Alert</code> cannot be found then null is returned.
     *
     * @param id
     *            id of the <code>Alert</code>
     * @return <code>Alert</code> whose name is <code>name</code> or null if
     *         not found
     * @throws AlertManagerException
     *             if unable to retrieve the Alert
     */
    Alert getAlert(UUID id) throws AlertManagerException;

    /**
     * Returns a list of all <code>Alert</code>s that relate to the provided <code>Report</code>
     * ID.  The Alerts are ordered by a column and direction (ASC, DESC) that is provided by the
     * user.  The alerts can be filtered by begin and/or end date.
     * This method helps support the server-side processing in the JQuery DataTables.
     *
     * @param reportId - ID of the Report to return Alerts from
     * @param columnToOrder - Column to be ordered
     * @param ascending - direction of sort
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
    @SuppressWarnings("checkstyle:parameternumber")
    FilteredRecordsList<Alert> getOrderedAlertList(
            String reportId, String columnToOrder, boolean ascending,
            int firstResult, int maxResults, String search,
            AlertListType listType,
            Map<String, Boolean> searchableColumns, Date beginDate, Date endDate)
            throws AlertManagerException;

    /**
     * Overloads the resolveAlerts method and provides a null description for the
     * alert resolution.
     *
     * @param alerts - list of Alert objects to be marked as resolved
     * @throws AlertManagerException
     *          if unable to save the list
     */
    void resolveAlerts(List<Alert> alerts) throws AlertManagerException;

    /**
     * Marks all Alerts that are provided as arguments as resolved.  This is used as
     * a "soft delete" method and will ensure they no longer appear in the Alert
     * table on the Portal.
     *
     * @param alerts - list of Alert objects to be marked as resolved
     * @param description - description of action taken.  The description can be null
     * @throws AlertManagerException
     *          if unable to save the list
     */
    void resolveAlerts(List<Alert> alerts, String description)
            throws AlertManagerException;

    /**
     * Retrieves unresolved {@link Alert}s associated with the provided {@link Policy}.
     *
     * @param policy                  policy that is being evaluated
     * @return                        list of unresolved alerts associated with {@link Policy}
     * @throws AlertManagerException
     *      If there is a query error
     */
    List<Alert> getAlertsForPolicy(Policy policy) throws AlertManagerException;

    /**
     * Retrieves unresolved {@link Alert}s associated with the provided {@link Baseline}.
     *
     * @param baseline                  baseline that is being evaluated
     * @return                          list of unresolved alerts associated with {@link Baseline}
     * @throws AlertManagerException
     *      If there is a query error
     */
    List<Alert> getAlertsForBaseline(Baseline baseline) throws AlertManagerException;

    /**
     * Retrieves the total number of Unresolved {@link Alert}s associated with the provided
     * {@link Baseline}.
     *
     * @param baseline                  baseline that is being evaluated
     * @return                          number of unresolved alerts associated with Baseline
     * @throws AlertManagerException
     *      If there is a query error
     */
    long getTotalAlertsForBaseline(Baseline baseline) throws AlertManagerException;

    /**
     * Gets the set of alerts for a device in order to determine the status of
     * the device (trusted or untrusted).
     *
     * The alerts meet one or more of these specifications:
     * <ol>
     *  <li> Have no report associated (missed periodic report alerts) for this device </li>
     *  <li> Are associated with the provided integrity report </li>
     *  <li> Match the specified criteria. e.g. leftover alerts from
     *      delta reports in the current series of delta reports). </li>
     * </ol>
     * @param device the device to query for alerts on
     * @param integrityReport the integrity report to find associated alerts with
     * @param optionalCriterion the optional additional criteria for which to query on
     * @return the set of device alerts associated with trust
     */
    List<Alert> getTrustAlerts(Device device, Report integrityReport,
                               Criterion optionalCriterion);

    /**
     * Gets the count of trust alerts for a device.  See {@link #getTrustAlerts} for more
     * information about which alerts are counted.
     *
     * @param device the device to query for alerts on
     * @param integrityReport the integrity report to find associated alerts with
     * @param optionalCriterion the optional additional criteria for which to query on
     * @return the count of alerts associated with trust
     */
    int getTrustAlertCount(Device device, Report integrityReport,
                           Criterion optionalCriterion);

    /**
     * Return the count of unresolved alerts associated with the given device.
     *
     * @param device associated with unresolved alerts being counted
     * @return count of unresolved alerts
     */
    int countUnresolvedAlerts(Device device);

    /**
     * Return the count of unresolved alerts associated with the given device that originate from
     * the given AlertSource.
     *
     * @param device associated with unresolved alerts being counted
     * @param source counted alerts must originate from
     * @return count of unresolved alerts
     */
    int countUnresolvedAlerts(Device device, Alert.Source source);

    /**
     * Count the total number of devices with at least one unresolved alert within the given group.
     *
     * @param deviceGroup to count devices from
     * @return count of devices with unresolved alerts
     */
    int countUnresolvedDevices(DeviceGroup deviceGroup);
}
