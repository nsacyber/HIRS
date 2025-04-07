package hirs.attestationca.portal.datatables;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Java representation of a jQuery DataTables Column.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
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
     * @see https://datatables.net/reference/option/columns.name
     */
    private String name;

    /**
     * Flag to indicate if this column is searchable (true) or not (false).
     *
     * @see https://datatables.net/reference/option/columns.searchable
     */
    @NotNull
    private boolean searchable;

    /**
     * Flag to indicate if this column is orderable (true) or not (false).
     *
     * @see https://datatables.net/reference/option/columns.orderable
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

}
