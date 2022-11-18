package hirs.attestationca.service;

import hirs.attestationca.entity.ReportSummary;
import hirs.persist.OrderedQuery;
import hirs.persist.ReportSummaryManagerException;

import java.util.List;
import java.util.UUID;

/**
 * A <code>ReferenceDigestValue</code> manages <code>ReferenceDigestValue</code>s. A
 * <code>ReferenceDigestValue</code> is used to store and manage digest events. It has
 * support for the basic create, read, update, and delete methods.
 */
public interface ReportSummaryService extends OrderedQuery<ReportSummary> {

    /**
     * Saves the <code>ReportSummary</code> in the database and returns it.
     *
     * @param report
     *            report summary to save
     * @return <code>ReportSummary</code> that was saved
     * @throws hirs.persist.ReportSummaryManagerException
     *             if ReportSummary has previously been saved or an error
     *             occurs while trying to save it to the database
     */
    ReportSummary saveReportSummary(ReportSummary report);

    /**
     * Updates a <code>ReportSummary</code>. This updates the database entries
     * to reflect the new values that should be set.
     *
     * @param report
     *            report
     * @throws ReportSummaryManagerException
     *             if Report has not previously been saved or an error occurs
     *             while trying to save it to the database
     */
    void updateReportSummary(ReportSummary report);

    /**
     * Returns a list of all <code>ReportSummary</code>s of type
     * <code>clazz</code>. This searches through the database for this
     * information.
     *
     * @param clazz
     *            class type of <code>ReportSummary</code>s to return (may be
     *            null)
     * @return list of <code>ReportSummary</code>s
     * @throws ReportSummaryManagerException
     *             if unable to search the database
     */
    List<ReportSummary> getReportSummaryList(ReportSummary clazz);

    /**
     * Returns a list of <code>ReportSummary</code>s of type
     * <code>clazz</code> that share the provided <code>hostname</code>. This
     * searches through the database for this information.
     *
     * @param hostname
     *            hostname for the machine in which the
     *            <code>ReportSummary</code> was generated for.
     * @return list of <code>ReportSummary</code>s
     * @throws ReportSummaryManagerException
     *             if unable to search the database
     */
    List<ReportSummary> getReportSummaryListByHostname(String hostname);

    /**
     * Retrieves the <code>ReportSummary</code> identified by the
     * <code>Report</code>'s <code>id</code>. If the <code>ReportSummary</code>
     * cannot be found then null is returned.
     *
     * @param id
     *            <code>UUID</code> of the <code>Report</code>
     * @return <code>ReportSummary</code> whose <code>Report</code>'s
     *         <code>UUID</code> is <code>id</code> or null if not found
     */
    ReportSummary getReportSummaryByReportID(UUID id);
}
