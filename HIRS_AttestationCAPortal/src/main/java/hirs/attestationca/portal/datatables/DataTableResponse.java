package hirs.attestationca.portal.datatables;

import hirs.attestationca.persist.FilteredRecordsList;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * A Wrapper for Data Table JSON responses. Allows Spring to serialize a data object with additional
 * meta data required by data tables.
 *
 * @param <T> the type of object that is being wrapped.
 */
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PUBLIC)
public final class DataTableResponse<T> {

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private final List<T> data = new LinkedList<>();

    private int draw;

    private long recordsTotal;

    private long recordsFiltered;

    /**
     * Builds a data table response using a FilteredRecordList.
     *
     * @param recordList the filtered record list
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
     * @param data            that is to be displayed by data table
     * @param draw            the originating draw request ID (usually from a web request)
     * @param recordsTotal    total number of records inside the data
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
     *
     * @return the data
     */
    public List<T> getData() {
        return Collections.unmodifiableList(data);
    }

    /**
     * Sets the data table data.
     *
     * @param data the data
     */
    public void setData(final List<T> data) {
        this.data.clear();
        this.data.addAll(data);
    }
}
