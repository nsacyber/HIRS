package hirs.persist.imarecord;

import hirs.FilteredRecordsList;
import hirs.data.persist.IMAMeasurementRecord;
import java.util.UUID;
import org.hibernate.Session;

/**
 * Executes the query to retrieve IMAMeasurementRecords for a Report.
 */
public class DbImaRecordQueryForReport extends DbImaRecordQuery {

    private static final String IMA_RECORD_QUERY
            = "select rec from IMAReport ima"
            + " join ima.imaRecords rec"
            + " where ima.id in"
            + " (select r.id from ReportSummary rs"
            + "  join rs.report ir"
            + "  join ir.reports r"
            + "  where ir.id = :id"
            + "  and r.class = IMAReport)";

    /**
     * Constructor setting the parameters required to execute the query.
     *
     * @param session the Session to use to execute the query
     * @param params the parameters specifying how to execute the query.
     */
    public DbImaRecordQueryForReport(final Session session, final DbImaRecordQueryParameters params) {
        super(session, params);
    }

    /**
     * Executes the query using the specified Session and DbImaRecordQueryParameters.
     *
     * @return FilteredRecordsList containing the results of the query.
     */
    @Override
    public FilteredRecordsList<IMAMeasurementRecord> query() {
        return super.query(IMA_RECORD_QUERY, UUID.fromString(getParams().getId()));
    }

}
