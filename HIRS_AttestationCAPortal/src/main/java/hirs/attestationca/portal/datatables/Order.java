package hirs.attestationca.portal.datatables;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Java representation of a Datatable's column's Ordering Status.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    /**
     * Index of the column to which ordering is applied.
     */
    @NotNull
    @Min(0)
    private int column;

    /**
     * The sorting direction for this column. Allowed values are "asc" for ascending or "desc" for descending.
     */
    @NotNull
    @Pattern(regexp = "(desc|asc)")
    private String dir;

    /**
     * The name of the column to which ordering is applied.
     */
    @NotNull
    private String name;
}

