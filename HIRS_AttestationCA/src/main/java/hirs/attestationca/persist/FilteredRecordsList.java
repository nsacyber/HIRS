package hirs.attestationca.persist;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;

/**
 * FilteredRecordsList is an object designed to hold the results from multiple
 * queries necessary to populate the JQuery Datatables.  The members include
 * the total number of records in the entity, the number of records returned
 * after filtering through the search bar, and the records themselves.
 *
 * @param <T> Class accepts generic for the list of data records.
 */
@Data
@EqualsAndHashCode(callSuper=false)
public class FilteredRecordsList<T> extends ArrayList<T> {

    private long recordsTotal, recordsFiltered;
}
