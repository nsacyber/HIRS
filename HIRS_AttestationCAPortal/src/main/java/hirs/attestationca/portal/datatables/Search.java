package hirs.attestationca.portal.datatables;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Java representation of a DataTables search parameter.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Search {

    /**
     * Search value.
     */
    @NotNull
    private String value = "";

    /**
     * True if the global filter should be treated as a regular expression for advanced searching,
     * false otherwise. Note that normally server-side processing scripts will not perform regular
     * expression searching for performance reasons on large data sets,
     * but it is technically possible and at the discretion of your script.
     */
    @NotNull
    private boolean regex;

    /**
     * Logic used for search (e.g., contains, equals, startsWith, endsWith).
     */
    @NotNull
    private String logic = "";

    /**
     * Type of search (e.g., text).
     */
    @NotNull
    private String type = "";
}
