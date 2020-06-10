package hirs.ima.matching;

import com.google.common.base.Preconditions;
import hirs.data.persist.baseline.IMABaselineRecord;
import hirs.data.persist.IMAMeasurementRecord;
import hirs.data.persist.IMAPolicy;
import hirs.data.persist.baseline.ImaBaseline;
import hirs.data.persist.enums.ReportMatchStatus;

import java.util.Collection;
import java.util.Set;

/**
 * This class extends the base matching functionality of {@link ImaRecordMatcher} to
 * compare {@link IMAMeasurementRecord}s against a collection of {@link IMABaselineRecord}s
 * based solely on their hashes.
 */
public class ImaAcceptableHashRecordMatcher extends ImaRecordMatcher<IMABaselineRecord> {
    /**
     * Construct a new ImaAcceptablePathAndHashRecordMatcher.
     *
     * @param records     the baseline records to use for matching
     * @param imaPolicy   the IMA policy to reference during matching; its partial path and path
     *                    equivalence settings influence matching behavior
     * @param imaBaseline the IMA baseline these records were sourced from; this is only used to
     */
    public ImaAcceptableHashRecordMatcher(
            final Collection<IMABaselineRecord> records,
            final IMAPolicy imaPolicy,
            final ImaBaseline imaBaseline) {
        super(records, imaPolicy, imaBaseline);
    }

    /**
     * Returns an IMAMatchStatus indicating whether the given {@link IMAMeasurementRecord} is
     * contained within the originally provided {@link IMABaselineRecord}s.
     *
     * @param record the record to look up
     * @return an IMAMatchStatus indicating whether the record is a match or unknown to
     *         the given baseline records
     */
    @Override
    public IMAMatchStatus<IMABaselineRecord> contains(final IMAMeasurementRecord record) {
        Preconditions.checkArgument(record != null, "Cannot match on null record.");

        final Set<IMABaselineRecord> matchingRecords = getRelatedBaselineRecordsByHash(record);

        if (matchingRecords.isEmpty()) {
            return new IMAMatchStatus<>(record, ReportMatchStatus.UNKNOWN, getImaBaseline());
        }

        return new IMAMatchStatus<>(
                record,
                ReportMatchStatus.MATCH,
                matchingRecords,
                getImaBaseline()
        );
    }
}
