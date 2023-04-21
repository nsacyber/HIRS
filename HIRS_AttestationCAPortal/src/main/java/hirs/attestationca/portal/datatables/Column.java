package hirs.attestationca.portal.datatables;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Java representation of a jQuery DataTables Column.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PUBLIC)
public class Column {

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
