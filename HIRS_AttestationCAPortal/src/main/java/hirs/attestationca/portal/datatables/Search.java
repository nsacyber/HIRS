package hirs.attestationca.portal.datatables;

import javax.validation.constraints.NotNull;

/**
 * Represents a jQuery DataTables search parameter.
 */
public class Search {

    /**
     * Default Constructor.
     */
    public Search() {
    }

    /**
     * Constructor for a non-regex search.
     * @param value the search value
     */
    public Search(final String value) {
        this(value, false);
    }

    /**
     * Constructor.
     * @param value the search value
     * @param regex the search regex
     */
    public Search(final String value, final boolean regex) {
        this.value = value;
        this.regex = regex;
    }

    /**
     * Global search value. To be applied to all columns which have searchable as true.
     */
    @NotNull
    private String value = "";

    /**
     * true if the global filter should be treated as a regular expression for advanced searching,
     * false otherwise. Note that normally server-side processing scripts will not perform regular
     * expression searching for performance reasons on large data sets,
     * but it is technically possible and at the discretion of your script.
     */
    @NotNull
    private boolean regex;


    /**
     *
     * @return the global search value, applied to all columns.
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets the global search value.
     * @param value the global search value
     */
    public void setValue(final String value) {
        this.value = value;
    }

    /**
     *
     * @return true if search should be treated as a regex, false otherwise
     */
    public boolean isRegex() {
        return regex;
    }

    /**
     * Sets the regex flag.
     * @param regex true if the search should be treated as a regex, false otherwise
     */
    public void setRegex(final boolean regex) {
        this.regex = regex;
    }

    @Override
    public String toString() {
        return "Search{"
                + "value='" + value + '\''
                + ", regex=" + regex
                + '}';
    }
}
