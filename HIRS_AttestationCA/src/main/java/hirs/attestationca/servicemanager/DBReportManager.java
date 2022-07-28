package hirs.attestationca.servicemanager;

import hirs.data.persist.Report;
import hirs.persist.DBManagerException;
import hirs.persist.ReportManager;
import hirs.persist.ReportManagerException;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.hibernate.criterion.Conjunction;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.apache.logging.log4j.LogManager.getLogger;
import static org.hibernate.criterion.Restrictions.ilike;

/**
 * This class defines a <code>ReportManager</code> that stores the reports in a
 * database.
 */
@Service
public class DBReportManager extends DBManager<Report> implements ReportManager {
    private static final Logger LOGGER = getLogger(DBReportManager.class);

    /**
     * Creates a new <code>DBReportManager</code> that uses the provided sessionFactory
     * to interact with a database.
     *
     * @param em entity manager used to access database connections
     */
    public DBReportManager(final EntityManager em) {
        super(Report.class, em);
    }

    /**
     * Saves the <code>Report</code> in the database and returns it.
     *
     * @param report
     *            report to save
     * @return <code>Report</code> that was saved
     * @throws hirs.persist.DBManagerException
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
            //super.getList(Report);
            return null;
        } catch (DBManagerException e) {
            throw new ReportManagerException(e);
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
            return false; //super.delete(id);
        } catch (DBManagerException e) {
            throw new ReportManagerException(e);
        }
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
