package hirs.attestationca.portal.datatables;


import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Represents a DataTables column's column control search parameter.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class ColumnControlSearch {

    /**
     * Search value.
     */
    @NotNull
    private String value = "";

    /**
     * Logic used for search (e.g., contains, equals, startsWith)
     */
    @NotNull
    private Logic logic = Logic.CONTAINS;

    /**
     * Type of search (e.g., text)
     */
    @NotNull
    private String type = "text";
}
