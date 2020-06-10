package hirs.ima.matching;

import com.google.common.base.Preconditions;
import hirs.data.persist.IMAMeasurementRecord;
import hirs.data.persist.baseline.AbstractImaBaselineRecord;
import hirs.data.persist.enums.ReportMatchStatus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * This class holds the results of the appraisal of a batch of {@link IMAMeasurementRecord}s against
 * one or many {@link hirs.data.persist.baseline.ImaBaseline}s.
 *
 * @param <T> the type of IMA baseline record that an instance of this class matches against
 */
public class BatchImaMatchStatus<T extends AbstractImaBaselineRecord> {
    // mapping from measurement record to match statuses, one per baseline
    private Map<IMAMeasurementRecord, Set<IMAMatchStatus<T>>> matchStatuses = new HashMap<>();

    /**
     * Create an empty instance of this class.
     */
    public BatchImaMatchStatus() {

    }

    /**
     * Given a set of {@link IMAMatchStatus}es that result from appraising one or many measurement
     * records against a baseline.
     *
     * @param matchStatuses the results of matching measurement records against a baseline
     */
    public BatchImaMatchStatus(final Collection<IMAMatchStatus<T>> matchStatuses) {
        if (matchStatuses == null) {
            throw new IllegalArgumentException("Cannot construct from null matches");
        }

        for (IMAMatchStatus<T> match : matchStatuses) {
            add(match);
        }
    }

    /**
     * Add the result of a match to this instance.
     *
     * @param status the match status to add
     */
    public void add(final IMAMatchStatus<T> status) {
        if (status == null) {
            throw new IllegalArgumentException("Cannot add a null match status");
        }

        IMAMeasurementRecord measurementRecord = status.getRecordToMatch();

        if (!matchStatuses.containsKey(measurementRecord)) {
            matchStatuses.put(measurementRecord, new HashSet<IMAMatchStatus<T>>());
        }

        for (IMAMatchStatus<T> existingMatchStatus : matchStatuses.get(measurementRecord)) {
            if (existingMatchStatus.getBaseline().equals(status.getBaseline())) {
                throw new IllegalArgumentException(String.format(
                        "A conflicting match result exists: %s",
                        existingMatchStatus.toString())
                );
            }
        }
        matchStatuses.get(measurementRecord).add(status);
    }

    /**
     * Retrieve the {@link IMAMatchStatus} that results from checking whether an
     * {@link IMAMeasurementRecord} was in a baseline.
     *
     * @param record the record whose match should be returned
     * @return the relevant {@link IMAMatchStatus}
     */
    public Set<IMAMatchStatus<T>> getIMAMatchStatuses(final IMAMeasurementRecord record) {
        Set<IMAMatchStatus<T>> imaMatchStatusSet = matchStatuses.get(record);
        if (imaMatchStatusSet == null) {
            throw new IllegalArgumentException("No match status stored for this record.");
        }
        return imaMatchStatusSet;
    }

    /**
     * Retrieve the {@link IMAMatchStatus} that results from checking whether an
     * {@link IMAMeasurementRecord} was in a baseline.
     *
     * @param record the record whose match should be returned
     * @return the relevant {@link IMAMatchStatus}, or null if no match was attempted for the record
     */
    public Set<IMAMatchStatus<T>> getMatchingIMAMatchStatuses(final IMAMeasurementRecord record) {
        Preconditions.checkArgument(contains(record), "No match status stored for this record.");
        HashSet<IMAMatchStatus<T>> matchingStatuses = new HashSet<>();
        for (IMAMatchStatus<T> matchStatus : getIMAMatchStatuses(record)) {
            if (matchStatus.getStatus().equals(ReportMatchStatus.MATCH)) {
                matchingStatuses.add(matchStatus);
            }
        }
        return matchingStatuses;
    }

    /**
     * Returns true if this BatchImaMatchStatus indicates that the given record has an associated
     * {@link ReportMatchStatus}.
     *
     * @param record the IMAMeasurementRecord to consider
     * @param matchStatus the match status to find for the given record
     * @return true if the given matchStatus was found associated with the given record;
     *         false otherwise
     */
    private boolean foundMatchStatusForRecord(final IMAMeasurementRecord record,
                                              final ReportMatchStatus matchStatus) {
        Preconditions.checkArgument(contains(record), "No match status stored for this record.");

        for (IMAMatchStatus<T> status : getIMAMatchStatuses(record)) {
            if (status.getStatus().equals(matchStatus)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns true if this BatchImaMatchStatus indicates that an actual (path &amp; hash) match was
     * found for the given {@link IMAMeasurementRecord}.  Not mutually exclusive
     * with the other 'found' methods.
     *
     * @param record the IMAMeasurementRecord to consider
     * @return true if there an actual (path &amp; hash) match was found, false otherwise
     */
    public boolean foundMatch(final IMAMeasurementRecord record) {
        return foundMatchStatusForRecord(record, ReportMatchStatus.MATCH);
    }

    /**
     * Returns true if this BatchImaMatchStatus indicates that a mismatch was found
     * for the given {@link IMAMeasurementRecord} (matching path, but not hash.)  Not mutually
     * exclusive with the other 'found' methods.
     *
     * @param record the IMAMeasurementRecord to consider
     * @return true if there a mismatch was found, false otherwise
     */
    public boolean foundMismatch(final IMAMeasurementRecord record) {
        return foundMatchStatusForRecord(record, ReportMatchStatus.MISMATCH);
    }

    /**
     * Returns true if this BatchImaMatchStatus indicates that the given record produced only an
     * UNKNOWN match status, and no matches or mismatches.
     *
     * @param record the IMAMeasurementRecord to consider
     * @return true if relevant IMAMatchStatuses are UNKNOWN, false otherwise
     */
    public boolean foundOnlyUnknown(final IMAMeasurementRecord record) {
        Preconditions.checkArgument(contains(record), "No match status stored for this record.");

        for (IMAMatchStatus<T> status : getIMAMatchStatuses(record)) {
            if (!status.getStatus().equals(ReportMatchStatus.UNKNOWN)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Retrieves all IMA baseline records held in this instance whose file paths match the given
     * record.  If the second parameter is non-null, only matches with the given match status will
     * be returned.  If the second parameter is null, all matching records will be returned.
     *
     * @param record the record whose paths should be considered when retrieving IMA baseline
     *               records that have matching file paths
     * @param reportMatchStatus if non-null, only IMA baseline records that are of the given match
     *                    type will be returned (this is only useful for MATCH and MISMATCH.)  If
     *                    null, both matching and mismatching baseline records will be returned.
     * @return a set of all matching IMABaselineRecords
     */
    private Set<T> getBaselineRecords(
            final IMAMeasurementRecord record,
            final ReportMatchStatus reportMatchStatus) {
        Preconditions.checkArgument(contains(record), "No match status stored for this record.");
        HashSet<T> baselineRecords = new HashSet<>();

        for (IMAMatchStatus<T> imaMatchStatus : getIMAMatchStatuses(record)) {
            if (reportMatchStatus == null || imaMatchStatus.getStatus().equals(reportMatchStatus)) {
                baselineRecords.addAll(imaMatchStatus.getBaselineRecords());
            }
        }

        return baselineRecords;
    }

    /**
     * Returns a Set of the IMABaselineRecords that are an actual match (path &amp; hash)
     * of the given {@link IMAMeasurementRecord}.
     *
     * @param record the IMAMeasurementRecord to consider
     * @return the Set of all records that have a MATCH to the given record; may be empty
     */
    public Set<T> getMatchingBaselineRecords(final IMAMeasurementRecord record) {
        return getBaselineRecords(record, ReportMatchStatus.MATCH);
    }

    /**
     * Returns a Set of the IMABaselineRecords that are a mismatch with the given
     * {@link IMAMeasurementRecord}.
     *
     * @param record the IMAMeasurementRecord to consider
     * @return the Set of all records that have a MISMATCH to the given record; may be empty
     */
    public Set<T> getMismatchingBaselineRecords(final IMAMeasurementRecord record) {
        return getBaselineRecords(record, ReportMatchStatus.MISMATCH);
    }

    /**
     * Returns a Set of the IMABaselineRecords matched or mismatched with the given record.
     * {@link IMAMeasurementRecord}.
     *
     * @param record the IMAMeasurementRecord to consider
     * @return the Set of all records that match or mismatch the given record
     */
    public Set<T> getBaselineRecords(final IMAMeasurementRecord record) {
        return getBaselineRecords(record, null);
    }

    /**
     * Returns a Set containing all {@link IMAMeasurementRecord}s that were evaluated against a
     * baseline in this instance.
     *
     * @return the set of all IMAMeasurementRecords in this instance
     */
    public Set<IMAMeasurementRecord> getAppraisedMeasurementRecords() {
        return Collections.unmodifiableSet(matchStatuses.keySet());
    }

    /**
     * Gets a collection of all IMAMatchStatuses with a status of ReportMatchStatus.MATCH.
     *
     * @return matching IMAMatchStatuses
     */
    public Collection<IMAMatchStatus<T>> getAllMatches() {
        List<IMAMatchStatus<T>> matches = new ArrayList<>();
        for (Map.Entry<IMAMeasurementRecord, Set<IMAMatchStatus<T>>> e : matchStatuses.entrySet()) {
            for (IMAMatchStatus<T> matchStatus : e.getValue()) {
                if (matchStatus.getStatus().equals(ReportMatchStatus.MATCH)) {
                    matches.add(matchStatus);
                }
            }
        }
        return matches;
    }

    /**
     * This method indicates whether or not the given {@link IMAMeasurementRecord} has one
     * or more recorded match statuses in this instance.
     *
     * @param record the record whose presence will be determined
     * @return true if this object contains results related to the given record; false otherwise
     */
    public boolean contains(final IMAMeasurementRecord record) {
        return matchStatuses.containsKey(record);
    }

    private Set<IMAMatchStatus<T>> getMatchStatuses() {
        Set<IMAMatchStatus<T>> matchSet = new HashSet<>();
        for (Map.Entry<IMAMeasurementRecord, Set<IMAMatchStatus<T>>> e : matchStatuses.entrySet()) {
            matchSet.addAll(e.getValue());
        }
        return matchSet;
    }

    /**
     * Given another BatchImaMatchStatus, merge its match statuses into this instance.  Useful in
     * collecting match results from multiple baselines.
     *
     * @param other the other BatchImaMatchStatus whose results should be merged into this instance
     */
    public void merge(final BatchImaMatchStatus<T> other) {
        for (IMAMatchStatus<T> match : other.getMatchStatuses()) {
            add(match);
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BatchImaMatchStatus that = (BatchImaMatchStatus) o;
        return Objects.equals(matchStatuses, that.matchStatuses);
    }

    @Override
    public int hashCode() {
        return Objects.hash(matchStatuses);
    }

    @Override
    public String toString() {
        return "BatchImaMatchStatus{"
                + "matchStatuses=" + matchStatuses
                + '}';
    }
}
