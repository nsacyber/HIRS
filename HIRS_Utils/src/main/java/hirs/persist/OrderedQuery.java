package hirs.persist;

import hirs.FilteredRecordsList;

import java.util.Map;

/**
 * Interface defining methods for getting ordered lists from a data source. Includes
 * properties for sorting, paging, and searching.
 * @param <T> the record type, T.
 */
public interface OrderedQuery<T> {

    /**
     * Returns a list of all <code>T</code>s that are ordered by a column and
     * direction (ASC, DESC) that is provided by the user. This method helps
     * support the server-side processing in the JQuery DataTables.
     *
     * @param clazz class type of <code>T</code>s to search for (may be null to
     * use Class&lt;T&gt;)
     * @param columnToOrder Column to be ordered
     * @param ascending direction of sort
     * @param firstResult starting point of first result in set
     * @param maxResults total number we want returned for display in table
     * @param search string of criteria to be matched to visible columns
     * @param searchableColumns Map of String and boolean values with column
     * headers and whether they should be searched. Boolean is true if field provides a
     * typical String that can be searched by Hibernate without transformation.
     * @return FilteredRecordsList object with query data
     * @throws DBManagerException if unable to create the list
     */
    FilteredRecordsList getOrderedList(
            Class<T> clazz, String columnToOrder,
            boolean ascending, int firstResult,
            int maxResults, String search,
            Map<String, Boolean> searchableColumns)
            throws DBManagerException;


    /**
     * Returns a list of all <code>T</code>s that are ordered by a column and
     * direction (ASC, DESC) that is provided by the user. This method helps
     * support the server-side processing in the JQuery DataTables. For entities that support
     * soft-deletes, the returned list does not contain <code>T</code>s that have been soft-deleted.
     *
     * @param clazz class type of <code>T</code>s to search for (may be null to
     * use Class&lt;T&gt;)
     * @param columnToOrder Column to be ordered
     * @param ascending direction of sort
     * @param firstResult starting point of first result in set
     * @param maxResults total number we want returned for display in table
     * @param search string of criteria to be matched to visible columns
     * @param searchableColumns Map of String and boolean values with column
     * headers and whether they should be searched. Boolean is true if field provides a
     * typical String that can be searched by Hibernate without transformation.
     * @param criteriaModifier a way to modify the criteria used in the query
     * @return FilteredRecordsList object with query data
     * @throws DBManagerException if unable to create the list
     */
    @SuppressWarnings("checkstyle:parameternumber")
    FilteredRecordsList<T> getOrderedList(
            Class<T> clazz, String columnToOrder,
            boolean ascending, int firstResult,
            int maxResults, String search,
            Map<String, Boolean> searchableColumns, CriteriaModifier criteriaModifier)
            throws DBManagerException;
}
