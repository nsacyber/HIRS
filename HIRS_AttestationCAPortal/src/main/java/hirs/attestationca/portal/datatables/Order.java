package hirs.attestationca.portal.datatables;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a column ordering with regards to a jQuery DataTable.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PUBLIC)
public class Order {


    /**
     * Constructor.
     * @param column the column index
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
     *
     * @return true if ascending order, false otherwise.
     */
    public boolean isAscending() {
        if (dir.equalsIgnoreCase("asc")) {
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return "Order{"
                + "column=" + column
                + ", dir='" + dir + '\''
                + '}';
    }
}

