package hirs.persist;

import hirs.FilteredRecordsList;
import hirs.data.bean.SimpleImaRecordBean;
import hirs.data.persist.IMAMeasurementRecord;
import hirs.data.persist.IMAReport;
import hirs.data.persist.IntegrityReport;
import hirs.data.persist.Report;
import hirs.data.persist.ReportSummary;
import hirs.persist.imarecord.DbImaRecordQueryForDevice;
import hirs.persist.imarecord.DbImaRecordQueryForDeviceSinceLastFullReport;
import hirs.persist.imarecord.DbImaRecordQueryForNone;
import hirs.persist.imarecord.DbImaRecordQueryForReport;
import hirs.persist.imarecord.DbImaRecordQueryParameters;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.criterion.Conjunction;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.transform.Transformers;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.apache.logging.log4j.LogManager.getLogger;
import static org.hibernate.criterion.Restrictions.ilike;

/**
 * This class defines a <code>ReportManager</code> that stores the reports in a
 * database.
 */
public class DBReportManager extends DBManager<Report> implements ReportManager {
    private static final Logger LOGGER = getLogger(DBReportManager.class);

    /**
     * Creates a new <code>DBReportManager</code> that uses the provided sessionFactory
     * to interact with a database.
     *
     * @param sessionFactory session factory used to access database connections
     */
    public DBReportManager(final SessionFactory sessionFactory) {
        super(Report.class, sessionFactory);
    }

    /**
     * Saves the <code>Report</code> in the database and returns it.
     *
     * @param report
     *            report to save
     * @return <code>Report</code> that was saved
     * @throws DBManagerException
     *             if Report has previously been saved or an error occurs while
     *             trying to save it to the database
     */
    @Override
    public final Report saveReport(final Report report)
            throws DBManagerException {
        LOGGER.debug("Saving report: {}", report);
        try {
            return super.save(report);
        } catch (DBManagerException e) {
            throw new ReportManagerException(e);
        }
    }

    /**
     * Returns a list of all <code>Report</code>s of type <code>clazz</code>.
     * This searches through the database for this information.
     *
     * All Reports will be returned without measurement records as they are
     * lazily loaded for performance.  If the records of a report are necessary,
     * a method will need to be written to return the records inside of a
     * transaction.
     *
     * @param clazz
     *            class type of <code>Report</code>s to return (may be null)
     * @return list of <code>Report</code>s
     * @throws ReportManagerException
     *             if unable to search the database
     */
    @Override
    public final List<Report> getReportList(final Class<?
            extends Report> clazz)
            throws ReportManagerException {
        LOGGER.debug("getting report list");
        try {
            return super.getList(clazz);
        } catch (DBManagerException e) {
            throw new ReportManagerException(e);
        }
    }

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
     * @param sinceLastFullReport limits the records to those since the last full report for the
     * device
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
    @Override
    @SuppressWarnings("checkstyle:parameternumber")
    public final FilteredRecordsList<SimpleImaRecordBean>
    getOrderedRecordListWithoutRecords(
            final IMARecordScope scope,
            final String id,
            final boolean sinceLastFullReport,
            final IMARecordField columnToOrder,
            final boolean ascending,
            final int firstResult,
            final int maxResults,
            final String search,
            final Map<String, Boolean> searchableColumns)
            throws ReportManagerException {

        // check columnToOrder
        if (columnToOrder == null) {
            final String msg = "columnToOrder cannot be null";
            LOGGER.error(msg);
            throw new IllegalArgumentException(msg);
        }

        // check scope
        if (scope == null) {
            throw new IllegalArgumentException("IMARecordScope cannot be null");
        }

        switch (scope) {
            case NONE:
                // Returns an empty FilteredRecordsList to make DataTables
                // display "No Data".
                return new FilteredRecordsList<>();
            case REPORT:
                return getImaRecordsForReport(id, columnToOrder, ascending,
                        firstResult,
                        maxResults, search, searchableColumns);
            case DEVICE:
                return getImaRecordsForDevice(id, sinceLastFullReport,
                        columnToOrder, ascending, firstResult,
                        maxResults, search, searchableColumns);
            default:
                throw new UnsupportedOperationException(
                        "IMARecordScope " + scope + " is not supported");
        }
    }

    /**
     * Returns a list of all Report Records that are ordered by a column
     * and direction (ASC, DESC) that is provided by the user.  This method
     * helps support the server-side processing in the JQuery DataTables.
     *
     * @param scope               the scope of the search: NONE, ALL, REPORT, or DEVICE
     * @param id                  the id or name of the REPORT or DEVICE to search
     * @param sinceLastFullReport limits the records to those since the last full report for the
     *                            device
     * @param columnToOrder       Column to be ordered
     * @param ascending           direction of sort
     * @param firstResult         starting point of first result in set
     * @param maxResults          total number we want returned for display in table
     * @param search              string of criteria to be matched to visible columns
     * @return FilteredRecordsList object with fields for DataTables
     * @throws ReportManagerException if unable to create the list
     */
    @Override
    @SuppressWarnings("checkstyle:parameternumber")
    public final FilteredRecordsList<IMAMeasurementRecord> getOrderedRecordList(
            final IMARecordScope scope,
            final String id,
            final boolean sinceLastFullReport,
            final IMARecordField columnToOrder,
            final boolean ascending,
            final int firstResult,
            final int maxResults,
            final String search)
            throws ReportManagerException {

        // check columnToOrder
        if (columnToOrder == null) {
            final String msg = "columnToOrder cannot be null";
            LOGGER.error(msg);
            throw new IllegalArgumentException(msg);
        }

        // check scope
        if (scope == null) {
            throw new IllegalArgumentException("IMARecordScope cannot be null");
        }

        Transaction tx = null;
        Session session = getFactory().getCurrentSession();
        try {
            tx = session.beginTransaction();

            final DbImaRecordQueryParameters params
                    = new DbImaRecordQueryParameters(id, columnToOrder, ascending,
                            firstResult, maxResults, search);

            switch (scope) {
                case NONE:
                    return new DbImaRecordQueryForNone().query();
                case REPORT:
                    return new DbImaRecordQueryForReport(session, params).query();
                case DEVICE:
                    if (sinceLastFullReport) {
                        DbImaRecordQueryForDeviceSinceLastFullReport q
                                = new DbImaRecordQueryForDeviceSinceLastFullReport(session, params);
                        return q.query();
                    } else {
                        return new DbImaRecordQueryForDevice(session, params).query();
                    }
                default:
                    throw new UnsupportedOperationException(
                            "IMARecordScope " + scope + " is not supported");
            }

        } catch (Exception ex) {
            String msg = "Error executing IMA Record query for " + scope + " scope.";
            if (scope == IMARecordScope.DEVICE) {
                msg += "Since last full report = " + sinceLastFullReport;
            }
            LOGGER.error(msg, ex);
            throw ex;
        } finally {
            if (tx != null) {
                tx.rollback();
            }
        }

    }

    /**
     * Retrieves the <code>Report</code> from the database. This searches the
     * database for an entry whose id matches <code>id</code>. It then
     * reconstructs a <code>Report</code> object from the database entry.
     *
     * Note: <code>IMAMeasurementRecords</code> are lazily loaded so the object
     * returned will not contain them for performance purposes.  If the whole
     * report needs to be retrieved a method will need to be written to return
     * the records inside of a transaction.
     *
     * @param id  id of the report
     * @return report
     * @throws ReportManagerException
     *             if unable to search the database or recreate the <code>Report</code>
     */
    @Override
    public final Report getReport(final UUID id) throws ReportManagerException {
        LOGGER.debug("getting report: {}", id);
        try {
            return super.get(id);
        } catch (DBManagerException e) {
            throw new ReportManagerException(e);
        }
    }

    @Override
    public final Report getCompleteReport(final UUID  id) throws ReportManagerException {
        LOGGER.debug("getting full report: {}", id);
        try {
            return super.getAndLoadLazyFields(id, true);
        } catch (DBManagerException e) {
            throw new ReportManagerException(e);
        }
    }

    /**
     * Updates a <code>Report</code>. This updates the database entries
     * to reflect the new values that should be set.
     *
     * @param report
     *             report to be updated
     * @throws ReportManagerException
     *             if Report an error occurs while updating the report or
     *             while trying to save it to the database
     */
    @Override
    public void updateReport(final Report report) throws ReportManagerException {
        LOGGER.debug("updating report: {}", report);
        try {
            super.update(report);
        } catch (DBManagerException e) {
            throw new ReportManagerException(e);
        }
    }

    /**
     * Deletes the <code>Report</code> from the database. This removes all of
     * the database entries that stored information with regards to the this
     * <code>Report</code>.
     * <p>
     * If the <code>Report</code> is referenced by any other tables then this
     * will throw a <code>ReportManagerException</code>.
     *
     * @param id
     *            id of the <code>Report</code> to delete
     * @return true if successfully found and deleted the <code>Report</code>
     * @throws ReportManagerException
     *             if unable to find the baseline or delete it from the
     *             database
     */
    @Override
    public final boolean deleteReport(final UUID id)
            throws ReportManagerException {
        LOGGER.debug("deleting baseline: {}", id);
        try {
            return super.delete(id);
        } catch (DBManagerException e) {
            throw new ReportManagerException(e);
        }
    }

    private FilteredRecordsList<SimpleImaRecordBean>
    getImaRecordsForReport(
            final String id,
            final IMARecordField columnToOrder,
            final boolean ascending,
            final int firstResult,
            final int maxResults,
            final String search,
            final Map<String, Boolean> searchableColumns) {
        IntegrityReport report = (IntegrityReport)
                getReport(UUID.fromString(id));
        IMAReport imaReport = report.extractReport(IMAReport.class);

        Transaction tx = null;
        Session session = getFactory().getCurrentSession();
        final FilteredRecordsList<SimpleImaRecordBean> imaRecords =
                new FilteredRecordsList<SimpleImaRecordBean>();

        Long filteredResultCount = Long.valueOf(0);

        try {
            tx = session.beginTransaction();
            LOGGER.debug("retrieving ima record list without records for "
                    + "report with id {}", id);
            // The first query gets the total number of IMA
            // measurement records associated with report id.
            Criteria cr = session.createCriteria(
                    IMAMeasurementRecord.class)
                    .add(Restrictions.eq("report.id", imaReport.getId()))
                    .setProjection(Projections.countDistinct("id"));
            Long totalResultCount = (Long) cr.uniqueResult();

            // This second query finds the number of IMA
            // measurement records matching the filter
            cr = session.createCriteria(IMAMeasurementRecord.class)
                    .add(Restrictions.eq("report.id", imaReport.getId()))
                    .setProjection(Projections.countDistinct("id"));

            // Filter using the search terms provided by the user
            Conjunction and = Restrictions.conjunction();
            if (totalResultCount != 0) {
                and = buildImaRecordSearchFilter(search, searchableColumns);
                cr.add(and);
                filteredResultCount = (Long) cr.uniqueResult();
            }

            if (filteredResultCount != 0) {
                // The third query builds a list from the filters,
                // limits, and sorting options and retrieves all ima measurement
                // records associated with a particular report id.
                cr = session.createCriteria(IMAMeasurementRecord.class)
                        .add(and)
                        .add(Restrictions.eq("report.id", imaReport.getId()))
                        .setProjection(Projections.projectionList()
                                .add(Projections.property("hash"), "hash")
                                .add(Projections.property("path"), "path")
                        )
                        .setResultTransformer(Transformers.aliasToBean(
                                SimpleImaRecordBean.class))
                        .setFirstResult(firstResult)
                        .setMaxResults(maxResults);

                if (ascending) {
                    cr.addOrder(Order.asc(columnToOrder.getHQL()));
                } else {
                    cr.addOrder(Order.desc(columnToOrder.getHQL()));
                }

                // Perform the query and add all baselines to the list
                List list = cr.list();
                for (Object o : list) {
                    if (o instanceof SimpleImaRecordBean) {
                        imaRecords.add((SimpleImaRecordBean) o);
                    }
                }
            }

            // Update meta data for the Data Table.
            imaRecords.setRecordsTotal(totalResultCount);
            imaRecords.setRecordsFiltered(filteredResultCount);
            tx.commit();
        } catch (HibernateException e) {
            final String msg = "Error getting the SimpleBaselineBean list";
            LOGGER.error(msg, e);
            if (tx != null) {
                LOGGER.debug("rolling back transaction");
                tx.rollback();
            }
            throw e;
        } finally {
            if (session != null && session.isConnected()) {
                session.close();
            }
        }
        return imaRecords;
    }

    @SuppressWarnings("checkstyle:parameternumber")
    private FilteredRecordsList<SimpleImaRecordBean>
    getImaRecordsForDevice(final String id,
             final boolean sinceLastFullReport,
             final IMARecordField columnToOrder,
             final boolean ascending,
             final int firstResult,
             final int maxResults,
             final String search,
             final Map<String, Boolean> searchableColumns) {
        List<UUID> imaReportIds;

        if (sinceLastFullReport) {
            imaReportIds = getImaIdsOfRecentBootCycle(id);
        } else {
            imaReportIds = getImaIdsOfDevice(id);
        }

        Transaction tx = null;
        Session session = getFactory().getCurrentSession();
        final FilteredRecordsList<SimpleImaRecordBean> imaRecords =
                new FilteredRecordsList<SimpleImaRecordBean>();

        Long totalResultCount = Long.valueOf(0);
        Long filteredResultCount = Long.valueOf(0);

        try {
            tx = session.beginTransaction();
            LOGGER.debug("retrieving ima record list without records for "
                    + "device with id {}", id);
            // The first query gets the total number of IMA
            // measurement records associated with ima report ids.
            Criteria cr = session.createCriteria(
                    IMAMeasurementRecord.class)
                    .add(Restrictions.in("report.id", imaReportIds))
                    .setProjection(Projections.countDistinct("id"));
            totalResultCount = (Long) cr.uniqueResult();

            // This second query finds the number of IMA
            // measurement records matching the filter
            cr = session.createCriteria(IMAMeasurementRecord.class)
                    .add(Restrictions.in("report.id", imaReportIds))
                    .setProjection(Projections.countDistinct("id"));

            // Filter using the search terms provided by the user
            Conjunction and = Restrictions.conjunction();
            if (totalResultCount != 0) {
                and = buildImaRecordSearchFilter(search, searchableColumns);
                cr.add(and);
                filteredResultCount = (Long) cr.uniqueResult();
            }

            if (filteredResultCount != 0) {
                // The third query builds a list from the filters,
                // limits, and sorting options


                cr = session.createCriteria(IMAMeasurementRecord.class)
                        .add(and)
                        .add(Restrictions.in("report.id", imaReportIds))
                        .setProjection(Projections.projectionList()
                                .add(Projections.property("hash"), "hash")
                                .add(Projections.property("path"), "path")
                        )
                        .setResultTransformer(Transformers.aliasToBean(
                                SimpleImaRecordBean.class))
                        .setFirstResult(firstResult)
                        .setMaxResults(maxResults);

                if (ascending) {
                    cr.addOrder(Order.asc(columnToOrder.getHQL()));
                } else {
                    cr.addOrder(Order.desc(columnToOrder.getHQL()));
                }

                // Perform the query and add all baselines to the list
                List list = cr.list();
                for (Object o : list) {
                    if (o instanceof SimpleImaRecordBean) {
                        imaRecords.add((SimpleImaRecordBean) o);
                    }
                }
            }

            // Update meta data for the Data Table.
            imaRecords.setRecordsTotal(totalResultCount);
            imaRecords.setRecordsFiltered(filteredResultCount);
            tx.commit();
        } catch (HibernateException e) {
            final String msg = "Error getting the " + SimpleImaRecordBean.class.getSimpleName()
                    + " list";
            LOGGER.error(msg, e);
            if (tx != null) {
                LOGGER.debug("rolling back transaction");
                tx.rollback();
            }
            throw e;
        } finally {
            if (session != null && session.isConnected()) {
                session.close();
            }
        }
        return imaRecords;
    }

    /**
     * Method retrieves all ima report ids associated with the latest full ima
     * report (as determined by a starting index of 0 and the most recent
     * report create time) of provided device id.
     * @param id of device
     * @return list of IMA report ids
     */
    private List<UUID> getImaIdsOfRecentBootCycle(final String id) {

        ReportSummaryManager reportSummaryManager =
                new DBReportSummaryManager(getFactory());
        List<ReportSummary> reportSummary = reportSummaryManager
                .getReportSummaryListByHostname(id);

        List<IMAReport> imaReports = new ArrayList<>();
        for (ReportSummary summary : reportSummary) {
            IntegrityReport integrityReport = (IntegrityReport) summary
                    .getReport();
            imaReports.add(integrityReport.extractReport(IMAReport.class));
        }

        String bootCycleId = "";
        Date createTime = null;
        List<UUID> imaReportIds = new ArrayList<>();

        // Retrieve most recent IMAReport where index is 0.
        for (IMAReport report : imaReports) {
            if (createTime == null) {
                createTime = report.getCreateTime();
                bootCycleId = report.getBootcycleId();
            } else if (createTime.before(report.getCreateTime())
                    && report.getIndex() == 0) {
                createTime = report.getCreateTime();
                bootCycleId = report.getBootcycleId();
            }
        }

        // Retrieve all IMAReports corresponding to bootCycleID.
        for (IMAReport report : imaReports) {
            if (report.getBootcycleId().equals(bootCycleId)) {
                imaReportIds.add(report.getId());
            }
        }
        return imaReportIds;
    }

    /**
     * Method retrieves all ima report ids corresponding to provided
     * device id.
     * device.
     * @param id of device
     * @return list of IMA report ids
     */
    private List<UUID> getImaIdsOfDevice(final String id) {

        ReportSummaryManager reportSummaryManager =
                new DBReportSummaryManager(getFactory());
        List<ReportSummary> reportSummary = reportSummaryManager
                .getReportSummaryListByHostname(id);

        List<UUID> imaReportIds = new ArrayList<>();
        for (ReportSummary summary : reportSummary) {
            IntegrityReport integrityReport = (IntegrityReport) summary
                    .getReport();
            imaReportIds.add(integrityReport.extractReport(IMAReport.class)
                    .getId());
        }
        return imaReportIds;
    }

    private Conjunction buildImaRecordSearchFilter(final String search, final
    Map<String, Boolean> searchableColumns) {
        // Search for all words in all searchable columns
        Conjunction and = Restrictions.conjunction();
        String[] searchWords = StringUtils.split(search);
        for (String word : searchWords) {
            // Every word must be in at least one column
            Disjunction or = Restrictions.disjunction();
            for (Map.Entry<String, Boolean> entry
                    : searchableColumns.entrySet()) {
                if (entry.getValue()) {
                    if (entry.getKey().equals("digest")) {
                        or.add(ilikeHex("digest", word));
                    } else {
                        or.add(ilike(entry.getKey(), word,
                                MatchMode.ANYWHERE));
                    }
                } else {
                    or.add(ilikeCast(entry.getKey(), word));
                }
            }
            and.add(or);
        }
        return and;
    }
}
