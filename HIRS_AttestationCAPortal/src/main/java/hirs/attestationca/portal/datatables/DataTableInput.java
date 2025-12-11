package hirs.attestationca.portal.datatables;

import io.micrometer.common.util.StringUtils;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Java representation of a Datatable input object.
 */
@Getter
@NoArgsConstructor
@ToString
public class DataTableInput {

    private static final int DEFAULT_LENGTH = 10;

    /**
     * Order in the DataTable.
     */
    @NotEmpty
    private final List<Order> order = new ArrayList<>();

    /**
     * Columns in the DataTable.
     */
    @NotEmpty
    private final List<Column> columns = new ArrayList<>();

    /**
     * Draw counter. This is used by DataTables to ensure that the Ajax returns from server-side
     * processing requests are drawn in sequence by DataTables (Ajax requests are asynchronous and
     * thus can return out of sequence). This is used as part of the draw return parameter (see
     * below).
     */
    @NotNull
    @Min(0)
    @Setter
    private int draw = 1;

    /**
     * Paging first record indicator. This is the start point in the current data set
     * (0 index based - i.e. 0 is the first record).
     */
    @NotNull
    @Min(0)
    @Setter
    private int start = 0;

    /**
     * Number of records that the table can display in the current draw. It is expected that the
     * number of records returned will be equal to this number,
     * unless the server has fewer records to return. Note that this can be -1 to indicate that
     * all records should be returned (although that
     * negates any benefits of server-side processing!)
     */
    @NotNull
    @Min(-1)
    @Setter
    private int length = DEFAULT_LENGTH;

    /**
     * Global search parameter.
     */
    @Setter
    @NotNull
    private Search search = new Search();

    /**
     * Constructor.
     *
     * @param draw    the draw counter
     * @param start   the paging start indicator
     * @param length  the number of records in current draw
     * @param search  the search parameter
     * @param order   the orderings
     * @param columns the columns of the input
     */
    public DataTableInput(final Integer draw, final Integer start, final Integer length,
                          final Search search, final List<Order> order,
                          final List<Column> columns) {
        this.draw = draw;
        this.start = start;
        this.length = length;
        this.search = search;
        this.order.addAll(order);
        this.columns.addAll(columns);
    }

    /**
     * Sets the orders.
     *
     * @param order the orders
     */
    public void setOrder(final List<Order> order) {
        this.order.clear();
        this.order.addAll(order);
    }

    /**
     * Sets the table columns.
     *
     * @param columns the columns
     */
    public void setColumns(final List<Column> columns) {
        this.columns.clear();
        this.columns.addAll(columns);
    }

    /**
     * Retrieves the column to which ordering is applied.
     *
     * @return the order object for the applied column, or null if no ordering is applied.
     */
    public Order getOrderColumn() {
        List<Order> orders = getOrder();

        // Return the first order if it exists and is associated with a valid column
        if (!CollectionUtils.isEmpty(orders) && !StringUtils.isBlank(orders.get(0).getName())) {
            return orders.get(0);
        }

        // Return null if no valid order is found
        return null;
    }
}
