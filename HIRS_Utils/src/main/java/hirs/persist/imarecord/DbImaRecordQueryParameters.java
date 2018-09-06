package hirs.persist.imarecord;

import hirs.persist.IMARecordField;

/**
 * Encapsulates the fields required for IMAMeasurementRecord queries.
 */
public class DbImaRecordQueryParameters {

    private final String id;
    private final IMARecordField columnToOrder;
    private final boolean ascending;
    private final int firstResult;
    private final int maxResults;
    private final String search;

    /**
     * Constructor to encapsulate all fields required for IMAMeasurementRecord queries.
     * @param id the id to use for the query.
     * @param columnToOrder the column to order.
     * @param ascending true if the results should be sorted ascending.
     * @param firstResult the index of the first result to return.
     * @param maxResults the maximum number of results to return.
     * @param search the value to use to filter the results.
     */
    public DbImaRecordQueryParameters(
            final String id,
            final IMARecordField columnToOrder,
            final boolean ascending,
            final int firstResult,
            final int maxResults,
            final String search) {
        this.id = id;
        this.columnToOrder = columnToOrder;
        this.ascending = ascending;
        this.firstResult = firstResult;
        this.maxResults = maxResults;
        this.search = search;
    }

    /**
     * Returns the id to use for the query.
     *
     * @return the id to use for the query.
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the column to order.
     *
     * @return the column to order.
     */
    public IMARecordField getColumnToOrder() {
        return columnToOrder;
    }

    /**
     * Returns true if the results should be sorted ascending.
     *
     * @return true if the results should be sorted ascending.
     */
    public boolean isAscending() {
        return ascending;
    }

    /**
     * Returns the index of the first result to return.
     *
     * @return the index of the first result to return.
     */
    public int getFirstResult() {
        return firstResult;
    }

    /**
     * Returns the maximum number of results to return.
     *
     * @return the maximum number of results to return.
     */
    public int getMaxResults() {
        return maxResults;
    }

    /**
     * Returns the value to use to filter the results.
     *
     * @return the value to use to filter the results.
     */
    public String getSearch() {
        return search;
    }

}
