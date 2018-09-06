package hirs.data.persist;

import com.fasterxml.jackson.annotation.JsonIgnore;
import hirs.persist.ImaBaselineRecordManager;

import javax.persistence.Entity;
import java.util.Collection;
import java.util.Set;

/**
 * Base class for all {@link ImaBaseline}s which contain data representing 'acceptable'
 * IMA baseline entries in the form of {@link IMABaselineRecord}s.  Used in the roles
 * of whitelists and required sets in {@link IMAPolicy}.
 */
@Entity
public abstract class ImaAcceptableRecordBaseline extends ImaBaseline<IMABaselineRecord> {
    /**
     * Creates a new ImaAcceptableRecordBaseline with the given name.
     *
     * @param name a name used to uniquely identify and reference the IMA baseline
     */
    public ImaAcceptableRecordBaseline(final String name) {
        super(name);
    }

    /**
     * Default constructor necessary for Hibernate.
     */
    protected ImaAcceptableRecordBaseline() {
    }

    /**
     * Returns an unmodifiable set of IMA baseline records found in the IMA
     * baseline. The returned set only contains the baseline records from this
     * baseline.
     *
     * @return list of IMA records
     */
    @JsonIgnore
    public abstract Set<IMABaselineRecord> getBaselineRecords();

    /**
     * Retrieve the {@link IMABaselineRecord}s from this baseline that are not contained
     * in the given set of records.  In other words, the returned set is the relative complement of
     * the given records in {@link IMABaselineRecord}s.
     *
     * @param recordManager
     *            an ImaBaselineRecordManager that can be used to retrieve persisted records
     * @param foundRecords
     *            the records that shall not be included in the returned set
     * @return all of the baseline's records except the set of given records
     */
    @JsonIgnore
    public abstract Collection<IMABaselineRecord> getRecordsExcept(
            ImaBaselineRecordManager recordManager,
            Set<IMABaselineRecord> foundRecords
    );
}
