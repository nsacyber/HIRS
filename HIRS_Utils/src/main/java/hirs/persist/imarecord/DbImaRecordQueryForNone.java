package hirs.persist.imarecord;

import hirs.FilteredRecordsList;
import hirs.data.persist.IMAMeasurementRecord;

/**
 * Returns an empty FilteredRecordsList when the specified scope is NONE.
 */
public class DbImaRecordQueryForNone extends DbImaRecordQuery {

    /**
     * Required constructor to satisfy abstract DbImaRecordQuery. Sets parameters to null.
     */
    public DbImaRecordQueryForNone() {
        super(null, null);
    }

    /**
     * Returns an empty FilteredRecordsList to make DataTables display "No Data".
     *
     * @return an empty FilteredRecordsList
     */
    @Override
    public FilteredRecordsList<IMAMeasurementRecord> query() {
        return new FilteredRecordsList<>();
    }

}
