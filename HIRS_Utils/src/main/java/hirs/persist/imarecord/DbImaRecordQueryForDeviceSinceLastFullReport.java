package hirs.persist.imarecord;

import hirs.FilteredRecordsList;
import hirs.data.persist.IMAMeasurementRecord;
import org.hibernate.Session;

/**
 * Executes the query to retrieve IMAMeasurementRecords for a Device since the last full IMA Report.
 */
public class DbImaRecordQueryForDeviceSinceLastFullReport extends DbImaRecordQuery {

    private static final String IMA_RECORD_QUERY
            = "select rec from IMAReport ima"
            + " join ima.imaRecords rec"
            + " where ima.id in"
            + " (select ima.id from ReportSummary rs"
            + "  join rs.report.reports ima"
            + "  where lower(rs.clientHostname) = lower(:id)"
            + "  and ima.class = IMAReport)"
            + " and ima.createTime >="
            + " (select max(ima.createTime) from IMAReport ima"
            + "  where ima.index = 0"
            + "  and ima.id in"
            + "  (select r.id from ReportSummary rs"
            + "   join rs.report.reports r"
            + "   where lower(rs.clientHostname) = lower(:id)"
            + "   and r.class = IMAReport))";

    /**
     * Constructor setting the parameters required to execute the query.
     *
     * @param session the Session to use to execute the query
     * @param params the parameters specifying how to execute the query.
     */
    public DbImaRecordQueryForDeviceSinceLastFullReport(
            final Session session, final DbImaRecordQueryParameters params) {
        super(session, params);
    }

    @Override
    public FilteredRecordsList<IMAMeasurementRecord> query() {
        return super.query(IMA_RECORD_QUERY, getParams().getId());
    }

}
