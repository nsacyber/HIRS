package hirs.persist;

import hirs.FilteredRecordsList;
import hirs.data.bean.SimpleImaRecordBean;
import hirs.data.persist.IMAMeasurementRecord;
import hirs.data.persist.Report;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A <code>ReportManager</code> manages <code>Report</code> objects. A
 * <code>ReportManager</code> is used to store and manage reports. It has
 * support for the basic create, read, update, and delete methods.
 */
public interface ReportManager {

    /**
     * Stores a new <code>Report</code>. This stores a new <code>Report</code>
     * to be managed by the <code>ReportManager</code>. If the
     * <code>Report</code> is successfully saved then a reference to it is
     * returned.
     * @param report
     *            Report to save
     * @return reference to saved Report
     * @throws ReportManagerException
     *             if the Report has previously been saved or unexpected error
     *             occurs
     */
    Report saveReport(Report report) throws ReportManagerException;

    /**
     * Returns a list of all <code>Report</code>s managed by this manager. A
     * <code>Class</code> argument may be specified to limit which types of
     * <code>Report</code>s to return. This argument may be null to return all
     * <code>Report</code>s.
     *
     * @param clazz
     *            class type of <code>Report</code>s to return (may be null)
     * @return list of all managed <code>Report</code> objects
     * @throws ReportManagerException
     *             if unable to create the list
     */
    List<Report> getReportList(Class<? extends Report> clazz)
            throws ReportManagerException;

    /**
     * Returns a list of all Report Records that are ordered by a column
     * and direction (ASC, DESC) that is provided by the user.  This
     * method contains database interactions designed to extract specific
     * fields from the database to avoid retrieving full records in order
     * to improve query performance and reduce the size of teh data set
     * returned to the caller. This method helps support the server-side
     * processing in the JQuery DataTables.
     *
     * @param scope the scope of the search: NONE, ALL, REPORT, or DEVICE
     * @param id the id or name of the REPORT or DEVICE to search
     * @param sinceLastFullReport
     *      limits the records to those since the last full report for the device
     * @param columnToOrder Column to be ordered
     * @param ascending direction of sort
     * @param firstResult starting point of first result in set
     * @param maxResults total number we want returned for display in table
     * @param search string of criteria to be matched to visible columns
     * @param searchableColumns map containing columns that search string can
     *                          be applied to
     * @return FilteredRecordsList object with fields for DataTables
     * @throws ReportManagerException if unable to create the list
     */
    @SuppressWarnings("checkstyle:parameternumber")
    FilteredRecordsList<SimpleImaRecordBean> getOrderedRecordListWithoutRecords(
            IMARecordScope scope,
            String id,
            boolean sinceLastFullReport,
            IMARecordField columnToOrder,
            boolean ascending,
            int firstResult,
            int maxResults,
            String search,
            Map<String, Boolean> searchableColumns)
            throws ReportManagerException;

    /**
     * Returns a list of all Report Records that are ordered by a column
     * and direction (ASC, DESC) that is provided by the user.  This method
     * helps support the server-side processing in the JQuery DataTables.
     *
     * @param scope the scope of the search: NONE, ALL, REPORT, or DEVICE
     * @param id the id or name of the REPORT or DEVICE to search
     * @param sinceLastFullReport
     *      limits the records to those since the last full report for the device
     * @param columnToOrder Column to be ordered
     * @param ascending direction of sort
     * @param firstResult starting point of first result in set
     * @param maxResults total number we want returned for display in table
     * @param search string of criteria to be matched to visible columns
     * @return FilteredRecordsList object with fields for DataTables
     * @throws ReportManagerException if unable to create the list
     */
    @SuppressWarnings("checkstyle:parameternumber")
    FilteredRecordsList<IMAMeasurementRecord> getOrderedRecordList(
            IMARecordScope scope,
            String id,
            boolean sinceLastFullReport,
            IMARecordField columnToOrder,
            boolean ascending,
            int firstResult,
            int maxResults,
            String search)
            throws ReportManagerException;

    /**
     * Retrieves the <code>Report</code> identified by <code>id</code>.
     *
     * @param id id of the <code>Report</code>
     * @return <code>Report</code> whose name is <code>name</code>
     * @throws ReportManagerException if unable to retrieve the Report
     */
    Report getReport(UUID id) throws ReportManagerException;

    /**
     * Retrieves the <code>Report</code> identified by <code>id</code>.  This method
     * fully loads a Report object; any lazy fields will be recursively loaded.
     *
     * @param id id of the <code>Report</code>
     * @return <code>Report</code> whose name is <code>name</code>
     * @throws ReportManagerException if unable to retrieve the Report
     */
    Report getCompleteReport(UUID id) throws ReportManagerException;

    /**
     * Updates the contents of the <code>Report</code>.
     *
     * @param report
     *             report to be updated
     * @throws ReportManagerException
     *             if any unexpected errors occur while trying to update the report
     */
    void updateReport(Report report) throws ReportManagerException;

    /**
     * Deletes the <code>Report</code> identified by <code>id</code>. If the
     * <code>Report</code> is found and deleted then true is returned, otherwise
     * false.
     *
     * @param id
     *            id of <code>Report</code> to be deleted
     * @return true if successfully found and deleted from repo, otherwise false
     * @throws ReportManagerException
     *             if unable to delete the Report for any reason other than not
     *             found
     */
    boolean deleteReport(UUID id) throws ReportManagerException;
}
