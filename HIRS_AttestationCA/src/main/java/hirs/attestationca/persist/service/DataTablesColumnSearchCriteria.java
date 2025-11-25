package hirs.attestationca.persist.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Java representation of the DataTables Column and Column Control.
 * Portions of the `ColumnControl` object and the `Column`
 * object from the `hirs.attestationcaportal` module are used here, as these objects cannot be directly
 * referenced in the `hirs.attestationca` module. This object allows services in `attestationca` to access
 * relevant data from the DataTable in the `attestationcaportal` module.
 */

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DataTablesColumnSearchCriteria {
    /**
     * Column's name.
     */
    private String columnName;

    /**
     * Search term applied to this column
     */
    private String columnSearchTerm;

    /**
     * Logic used for search (e.g., contains, equals, startsWith, endsWith) for this column.
     */
    private String columnSearchLogic;

    /**
     * Type of search (e.g., text) used for this column.
     */
    private String columnSearchType;
}
