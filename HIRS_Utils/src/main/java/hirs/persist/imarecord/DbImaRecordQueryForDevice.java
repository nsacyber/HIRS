package hirs.persist.imarecord;

import hirs.FilteredRecordsList;
import hirs.data.persist.IMAMeasurementRecord;
import org.hibernate.Session;

/**
 * Executes the query to retrieve IMAMeasurementRecords for a Device.
 */
public class DbImaRecordQueryForDevice extends DbImaRecordQuery {

    private static final String IMA_RECORD_QUERY
            = "select rec from IMAReport ima"
            + " join ima.imaRecords rec"
            + " where ima.id in"
            + " (select r.id from ReportSummary rs"
            + "  join rs.report.reports r"
            + "  where lower(rs.clientHostname) = lower(:id)"
            + "  and r.class = IMAReport)";

    /**
     * Constructor setting the parameters required to execute the query.
     *
     * @param session the Session to use to execute the query
     * @param params the parameters specifying how to execute the query.
     */
    public DbImaRecordQueryForDevice(
            final Session session, final DbImaRecordQueryParameters params) {
        super(session, params);
    }

    /**
     * Executes the query using the specified Session and DbImaRecordQueryParameters.
     *
     * @return FilteredRecordsList containing the results of the query.
     */
    @Override
    public FilteredRecordsList<IMAMeasurementRecord> query() {
        return super.query(IMA_RECORD_QUERY, getParams().getId());
    }

}
