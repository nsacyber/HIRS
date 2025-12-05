package hirs.attestationca.portal.datatables;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Java representation of a DataTables Column Control.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ColumnControl {
    /**
     * Search parameter.
     */
    private Search search;
}
