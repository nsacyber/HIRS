package hirs;

import java.util.ArrayList;

/**
 * FilteredRecordsList is an object designed to hold the results from multiple
 * queries necessary to populate the JQuery Datatables.  The members include
 * the total number of records in the entity, the number of records returned
 * after filtering through the search bar, and the records themselves.
 *
 * @param <T> Class accepts generic for the list of data records.
 */
public class FilteredRecordsList<T> extends ArrayList<T> {
    private long recordsTotal;
    private long recordsFiltered;

    /**
     * Returns the total number of records stored in the table of the entity.
     * @return recordsTotal
     */
    public final long getRecordsTotal() {
        return recordsTotal;
    }

    /**
     * Sets the total number of records (rows) that are in the table.
     * @param recordsTotal total number of rows
     */
    public final void setRecordsTotal(final long recordsTotal) {
        this.recordsTotal = recordsTotal;
    }

    /**
     * Returns the total number of records returned from the query after
     * filtering was applied.
     * @return recordsFiltered
     */
    public final long getRecordsFiltered() {
        return recordsFiltered;
    }

    /**
     * Sets the total number of records that were returned from the query after
     * filtering was applied.
     * @param recordsFiltered total number of filtered rows
     */
    public final void setRecordsFiltered(final long recordsFiltered) {
        this.recordsFiltered = recordsFiltered;
    }
}
