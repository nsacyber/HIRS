package hirs.attestationca.portal.datatables;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a column ordering in regard to a jQuery DataTable.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {


    /**
     * Column to which ordering should be applied. This is an index reference
     * to the columns array of information that is also submitted to the server.
     */
    @NotNull
    @Min(0)
    private int column;
    /**
     * Ordering direction for this column. It will be asc or desc to indicate ascending ordering or
     * descending ordering, respectively.
     */
    @NotNull
    @Pattern(regexp = "(desc|asc)")
    private String dir;

    /**
     * Constructor.
     *
     * @param column      the column index
     * @param isAscending true if ascending order
     */
    public Order(final int column, final boolean isAscending) {
        this.column = column;
        if (isAscending) {
            this.dir = "asc";
        } else {
            this.dir = "desc";
        }
    }

    /**
     * @return true if ascending order, false otherwise.
     */
    public boolean isAscending() {
        return dir.equalsIgnoreCase("asc");
    }
}

