package hirs.attestationca.portal.datatables;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;


/**
 * Represents a column ordering with regards to a jQuery DataTable.
 */
public class Order {

    /**
     * Constructor.
     */
    public Order() {
    }

    /**
     * Constructor.
     * @param column the column index
     * @param dir the order direction
     */
    public Order(final int column, final String dir) {
        this.column = column;
        this.dir = dir;
    }

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
     * Gets the column index.
     * @return the column index
     */
    public int getColumn() {
        return column;
    }

    /**
     * Sets the column index.
     * @param column the column index
     */
    public void setColumn(final int column) {
        this.column = column;
    }

    /**
     * Gets the direction order.
     * @return the direction order
     */
    public String getDir() {
        return dir;
    }

    /**
     * Sets the direction order.
     * @param dir the direction order
     */
    public void setDir(final String dir) {
        this.dir = dir;
    }

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
