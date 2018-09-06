package hirs.ima.matching;

import hirs.data.persist.IMAMeasurementRecord;
import hirs.data.persist.IMAPolicy;
import hirs.data.persist.ImaBaseline;
import hirs.data.persist.ImaIgnoreSetRecord;
import hirs.data.persist.ReportMatchStatus;
import hirs.utils.RegexFilePathMatcher;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * This class extends the base matching functionality of {@link ImaRecordMatcher} to
 * compare {@link IMAMeasurementRecord}s against a collection of {@link ImaIgnoreSetRecord}s.
 */
public class ImaIgnoreSetRecordMatcher extends ImaRecordMatcher<ImaIgnoreSetRecord> {
    private Map<ImaIgnoreSetRecord, RegexFilePathMatcher> matchers = new HashMap<>();

    /**
     * Construct a new ImaBlacklistRecordMatcher.
     *
     * @param records     the baseline records to use for matching
     * @param imaPolicy   the IMA policy to reference during matching; its partial path and path
     *                    equivalence settings influence matching behavior
     * @param imaBaseline the IMA baseline these records were sourced from; this is only used to
     */
    public ImaIgnoreSetRecordMatcher(
            final Collection<ImaIgnoreSetRecord> records,
            final IMAPolicy imaPolicy,
            final ImaBaseline imaBaseline) {
        super(records, imaPolicy, imaBaseline);

        for (ImaIgnoreSetRecord ignoreRecord : records) {
            matchers.put(ignoreRecord, new RegexFilePathMatcher(ignoreRecord.getPath()));
        }
    }

    /**
     * Returns an IMAMatchStatus indicating whether the given {@link IMAMeasurementRecord} is
     * contained within the originally provided {@link ImaIgnoreSetRecord}s.
     *
     * A measurement record's path will match this ignore set in the following cases:
     * <p>&nbsp;
     * <ul>
     *   <li>if any of of the ignore set records contain a regex that matches the given path</li>
     *   <li>if any of of the ignore set records are an exact match with the given path</li>
     *   <li>if any of of the ignore set records are the initial substring of the given path</li>
     *   <li>if any of of the ignore set records are partial paths and the given path is a full path
     *       with the same filename</li>
     *   <li>if the given path is a partial path and any of of the ignore set records are full paths
     *       with the same filename</li>
     * </ul>
     *
     * @param record the record to look up
     * @return an IMAMatchStatus indicating whether the record is a match, mismatch, or unknown to
     *         the given baseline records
     */
    public IMAMatchStatus<ImaIgnoreSetRecord> contains(final IMAMeasurementRecord record) {
        if (record == null) {
            throw new IllegalArgumentException("Cannot match on null record.");
        }

        Set<ImaIgnoreSetRecord> matchingRecords = getRelatedBaselineRecordsByPath(record);

        for (Map.Entry<ImaIgnoreSetRecord, RegexFilePathMatcher> recordMatcher
                : matchers.entrySet()) {
            if (recordMatcher.getValue().isMatch(record.getPath())) {
                matchingRecords.add(recordMatcher.getKey());
            }
        }

        if (matchingRecords.isEmpty()) {
            return new IMAMatchStatus<>(record, ReportMatchStatus.UNKNOWN, getImaBaseline());
        } else {
            return new IMAMatchStatus<>(
                    record,
                    ReportMatchStatus.MATCH,
                    matchingRecords,
                    getImaBaseline()
            );
        }
    }
}
