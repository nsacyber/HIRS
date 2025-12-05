package hirs.attestationca.portal.datatables;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Java representation of a DataTables Column.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Column {

    /**
     * Column's data source.
     *
     * @see https://datatables.net/reference/option/columns.data
     */
    @NotBlank
    private String data;

    /**
     * Column's name.
     *
     * @see https://datatables.net/reference/option/columns.name
     */
    @NotBlank
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
     * Column control for this specific column.
     */
    @NotNull
    private ColumnControl columnControl;
}
