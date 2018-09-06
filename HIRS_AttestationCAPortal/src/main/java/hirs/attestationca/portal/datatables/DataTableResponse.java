package hirs.attestationca.portal.datatables;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import hirs.FilteredRecordsList;

/**
 * A Wrapper for Data Table JSON responses. Allows Spring to serialize a data object with additional
 * meta data required by data tables.
 *
 * @param <T> the type of object that is being wrapped.
 */
public final class DataTableResponse<T> {

    private List<T> data = new LinkedList<T>();
    private int draw;
    private long recordsTotal;
    private long recordsFiltered;

    /**
     * Default constructor.
     */
    public DataTableResponse() {

    }

    /**
     * Builds a data table response using a FilteredRecordList.
     * @param recordList the filtered recordd list
     * @param inputQuery the data table input (used for draw)
     */
    public DataTableResponse(final FilteredRecordsList<T> recordList,
                             final DataTableInput inputQuery) {
        this(recordList, inputQuery.getDraw(),
                recordList.getRecordsTotal(), recordList.getRecordsFiltered());
    }

    /**
     * Constructs a data table response using the specified data with the data table specific
     * information.
     *
     * @param data that is to be displayed by data table
     * @param draw the originating draw request ID (usually from a web request)
     * @param recordsTotal total number of records inside the data
     * @param recordsFiltered number of records excluded from the request
     */
    public DataTableResponse(final List<T> data, final int draw, final long recordsTotal,
            final long recordsFiltered) {
        setData(data);
        this.draw = draw;
        this.recordsTotal = recordsTotal;
        this.recordsFiltered = recordsFiltered;
    }

    /**
     * Gets the data table data.
     * @return the data
     */
    public List<T> getData() {
        return Collections.unmodifiableList(data);
    }

    /**
     * Sets the data table data.
     * @param data the data
     */
    public void setData(final List<T> data) {
        this.data.clear();
        this.data.addAll(data);
    }

    /**
     * Gets the table draw index of the table.
     * @return the draw index
     */
    public int getDraw() {
        return draw;
    }

    /**
     * Sets the table draw index of the table.
     * @param draw the draw index
     */
    public void setDraw(final int draw) {
        this.draw = draw;
    }

    /**
     * Gets the total records.
     * @return the total records count
     */
    public long getRecordsTotal() {
        return recordsTotal;
    }

    /**
     * Sets the total record count.
     * @param recordsTotal the total records count
     */
    public void setRecordsTotal(final long recordsTotal) {
        this.recordsTotal = recordsTotal;
    }

    /**
     * Gets the total filtered record count.
     * @return the total filtered record count
     */
    public long getRecordsFiltered() {
        return recordsFiltered;
    }

    /**
     * Sets the total filtered record count.
     * @param recordsFiltered the total filtered record count
     */
    public void setRecordsFiltered(final long recordsFiltered) {
        this.recordsFiltered = recordsFiltered;
    }
}
