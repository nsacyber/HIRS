package hirs.attestationca.portal.datatables;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Represents a jQuery DataTables search parameter.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Search {

    /**
     * Global search value. To be applied to all columns which have searchable as true.
     */
    @NotNull
    private String value = "";
    
    /**
     * true if the global filter should be treated as a regular expression for advanced searching,
     * false otherwise. Note that normally server-side processing scripts will not perform regular
     * expression searching for performance reasons on large data sets,
     * but it is technically possible and at the discretion of your script.
     */
    @NotNull
    private boolean regex;

    /**
     * Constructor for a non-regex search.
     *
     * @param value the search value
     */
    public Search(final String value) {
        this(value, false);
    }
}

