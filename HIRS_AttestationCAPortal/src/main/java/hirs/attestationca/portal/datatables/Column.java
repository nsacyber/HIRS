package hirs.attestationca.portal.datatables;

import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotBlank;

/**
 * Java representation of a jQuery DataTables Column.
 */
public class Column {

    /**
     * Default constructor.
     */
    public Column() {
    }

    /**
     * Constructor.
     * @param data the data
     * @param name the name
     * @param searchable true if searchable
     * @param orderable true if orderable
     * @param search the Search structure.
     */
    public Column(final String data, final String name, final boolean searchable,
                  final boolean orderable, final Search search) {
        this.data = data;
        this.name = name;
        this.searchable = searchable;
        this.orderable = orderable;
        this.search = search;
    }

    /**
     * Column's data source.
     *
     * @see http://datatables.net/reference/option/columns.data
     */
    @NotBlank
    private String data;

    /**
     * Column's name.
     *
     * @see http://datatables.net/reference/option/columns.name
     */
    private String name;

    /**
     * Flag to indicate if this column is searchable (true) or not (false).
     *
     * @see http://datatables.net/reference/option/columns.searchable
     */
    @NotNull
    private boolean searchable;

    /**
     * Flag to indicate if this column is orderable (true) or not (false).
     *
     * @see http://datatables.net/reference/option/columns.orderable
     */
    @NotNull
    private boolean orderable;

    /**
     * Search value to apply to this specific column.
     */
    @NotNull
    private Search search;

    /**
     *
     * @return the data
     */
    public String getData() {
        return data;
    }

    /**
     * Sets the data.
     * @param data the data
     */
    public void setData(final String data) {
        this.data = data;
    }

    /**
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name.
     * @param name the name
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * Gets the searchable flag.
     * @return true if searchable, false otherwise
     */
    public boolean isSearchable() {
        return searchable;
    }

    /**
     * Sets the searchable flag.
     * @param searchable true if searchable, false otherwise
     */
    public void setSearchable(final boolean searchable) {
        this.searchable = searchable;
    }


    /**
     *
     * @return true if orderable, false otherwise
     */
    public boolean isOrderable() {
        return orderable;
    }

    /**
     * Sets the orderable flag.
     * @param orderable true if orderable, false otherwise
     */
    public void setOrderable(final boolean orderable) {
        this.orderable = orderable;
    }

    /**
     *
     * @return the search
     */
    public Search getSearch() {
        return search;
    }

    /**
     * Sets the search.
     * @param search the search
     */
    public void setSearch(final Search search) {
        this.search = search;
    }

    /**
     * Set the search value to apply to this column.
     *
     * @param searchValue if any, the search value to apply
     */
    public void setSearchValue(final String searchValue) {
        this.search.setValue(searchValue);
    }

    @Override
    public String toString() {
        return "Column{"
                + "data='" + data + '\''
                + ", name='" + name + '\''
                + ", searchable=" + searchable
                + ", orderable=" + orderable
                + ", search=" + search
                + '}';
    }
}
