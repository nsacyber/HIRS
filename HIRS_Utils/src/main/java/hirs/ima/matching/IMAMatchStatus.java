package hirs.ima.matching;

import hirs.data.persist.Baseline;
import hirs.data.persist.IMAMeasurementRecord;
import hirs.data.persist.AbstractImaBaselineRecord;
import hirs.data.persist.ImaBaseline;
import hirs.data.persist.ReportMatchStatus;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * This represents the match status for when an <code>ImaBaseline</code> tests
 * if an <code>IMAMeasurementRecord</code> is contained in it. This contains the
 * <code>ReportMatchStatus</code> as well as a set of records.
 * <p>
 * The set of records has different meanings depending upon the return value.
 * If the match status return MATCH then the list contains all of the matching
 * entries. If partial path support is disabled then this can only return one
 * entry. If partial path is enabled then this can return multiple matches,
 * consider /usr/bin/gradle and /home/foo/bin/gradle as an example.
 * <p>
 * If the report status is a mismatch then this returns the set of baseline
 * records that have the same path but a different hash. This can happen if
 * multiple values are acceptable for a file. This can also happen if partial
 * path is enabled and different files have the same name but in different
 * directories.
 * <p>
 * If the report status is unknown then the set is empty.
 *
 * @param <T> the type of IMA baseline record that this class matches against
 */
public final class IMAMatchStatus<T extends AbstractImaBaselineRecord> {

    private ReportMatchStatus status;
    private IMAMeasurementRecord recordToMatch;
    private HashSet<T> baselineRecords; // these should all be from the same baseline
    private ImaBaseline baseline;

    /**
     * Convenience constructor for an UNKNOWN match status. This creates
     * a new <code>IMAMatchStatus</code> that must have a status of
     * UNKNOWN. If not UNKNOWN then an Exception will be thrown. The set of
     * records is initialized to an empty list.
     *
     * @param recordToMatch the record associated with this match status
     * @param status must be UNKNOWN
     * @param baseline the baseline from which this match originated
     */
    public IMAMatchStatus(final IMAMeasurementRecord recordToMatch,
                          final ReportMatchStatus status,
                          final ImaBaseline baseline) {
        this(recordToMatch, status, new HashSet<T>(), baseline);
    }

    /**
     * Convenience constructor for creating a match status that only has one
     * record. The record can only be null if status is UNKNOWN. In that case an
     * empty set is created. Otherwise it cannot be null and the status must be
     * either MATCH or MISMATCH.
     *
     * @param recordToMatch the record associated with this match status
     * @param status status
     * @param record record (can be null if status is UNKNOWN)
     * @param baseline the baseline from which this match originated
     */
    public IMAMatchStatus(
            final IMAMeasurementRecord recordToMatch,
            final ReportMatchStatus status,
            final T record,
            final ImaBaseline baseline) {
        this(recordToMatch, status, Collections.singleton(record), baseline);
    }

    /**
     * Creates a new <code>IMAMatchStatus</code>. If status is MATCH or
     * MISMATCH then the records set cannot be empty. Else the records set
     * must be empty.
     *
     * @param recordToMatch the record associated with this match status
     * @param status match status
     * @param baselineRecords records
     * @param baseline the baseline from which this match originated
     */
    public IMAMatchStatus(
            final IMAMeasurementRecord recordToMatch,
            final ReportMatchStatus status,
            final Set<T> baselineRecords,
            final ImaBaseline baseline) {
        if (recordToMatch == null) {
            throw new IllegalArgumentException("Cannot have a null IMAMeasurementRecord.");
        }
        if (status == null) {
            throw new IllegalArgumentException("Cannot have a null match status.");
        }
        if (baselineRecords == null) {
            throw new IllegalArgumentException("Cannot have null baseline records.");
        }
        if (baseline == null) {
            throw new IllegalArgumentException("Cannot have a null baseline.");
        }
        if (status == ReportMatchStatus.UNKNOWN && baselineRecords.size() > 0) {
            throw new IllegalArgumentException("Cannot have an unknown status with associated"
                    + "baseline records.");
        }
        if (status != ReportMatchStatus.UNKNOWN && baselineRecords.size() == 0) {
            throw new IllegalArgumentException("Cannot have a match or mismatch status without"
                    + "associated baseline records.");
        }
        this.recordToMatch = recordToMatch;
        this.status = status;
        this.baselineRecords = new HashSet<>(baselineRecords);
        this.baseline = baseline;
    }

    /**
     * Returns the match status as to whether or not the record was found.
     *
     * @return match status
     */
    public ReportMatchStatus getStatus() {
        return status;
    }

    /**
     * Returns the records associated with the match status. See the class
     * JavaDocs for a complete description of what to expect in the set.
     *
     * @return records
     */
    public Set<T> getBaselineRecords() {
        return Collections.unmodifiableSet(baselineRecords);
    }

    /**
     * Returns the record for which matches were either found or not found.
     *
     * @return the record that has been matched (or not)
     */
    public IMAMeasurementRecord getRecordToMatch() {
        return recordToMatch;
    }

    /**
     * Return the <code>Baseline</code> used to generate this match status.
     *
     * @return baseline used to generate status
     */
    public Baseline getBaseline() {
        return baseline;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        IMAMatchStatus<?> that = (IMAMatchStatus<?>) o;
        return status == that.status
                && Objects.equals(recordToMatch, that.recordToMatch)
                && Objects.equals(baselineRecords, that.baselineRecords)
                && Objects.equals(baseline, that.baseline);
    }

    @Override
    public int hashCode() {
        return Objects.hash(status, recordToMatch, baselineRecords, baseline);
    }

    @Override
    public String toString() {
        return "IMAMatchStatus{"
            + "status=" + status
            + ", recordToMatch=" + recordToMatch
            + ", baselineRecords=" + baselineRecords
            + ", baseline=" + baseline
            + '}';
    }
}
