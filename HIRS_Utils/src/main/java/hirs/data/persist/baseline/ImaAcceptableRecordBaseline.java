package hirs.data.persist.baseline;

import com.fasterxml.jackson.annotation.JsonIgnore;
import hirs.data.persist.IMAMeasurementRecord;
import hirs.data.persist.IMAPolicy;
import hirs.ima.matching.BatchImaMatchStatus;
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
     * Similar to contains, but only considers the hash value and does not consider
     * the path as relevant to matching at all.
     *
     * Each type of baseline specifies its own
     * 'contains' algorithm for deciding whether the given measurements are
     * considered as matches, mismatches, or unknowns to the baseline.  The 'contains' method
     * of ImaAcceptableRecordBaselines that is normally used to judge measurement records
     * against baseline records considers both paths and hashes; this method offers an
     * additional mechanism for finding matching baseline records solely based
     * on matching hash values.
     *
     * @param records
     *            measurement records to find in this baseline
     * @param recordManager
     *            an ImaBaselineRecordManager that can be used to retrieve persisted records
     * @param imaPolicy
     *            the IMA policy to use while determining if a baseline contains the given records
     *
     * @return batch match status for the measurement records, according only to hashes
     */
    @JsonIgnore
    public abstract BatchImaMatchStatus<IMABaselineRecord> containsHashes(
            Collection<IMAMeasurementRecord> records,
            ImaBaselineRecordManager recordManager,
            IMAPolicy imaPolicy
    );

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
