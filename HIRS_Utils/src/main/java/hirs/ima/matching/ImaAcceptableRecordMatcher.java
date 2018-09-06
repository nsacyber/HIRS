package hirs.ima.matching;

import hirs.data.persist.DigestComparisonResultType;
import hirs.data.persist.IMABaselineRecord;
import hirs.data.persist.IMAMeasurementRecord;
import hirs.data.persist.IMAPolicy;
import hirs.data.persist.ImaBaseline;
import hirs.data.persist.ReportMatchStatus;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static org.apache.logging.log4j.LogManager.getLogger;

/**
 * This class extends the base matching functionality of {@link ImaRecordMatcher} to
 * compare {@link IMAMeasurementRecord}s against a collection of {@link IMABaselineRecord}s.
 */
public class ImaAcceptableRecordMatcher extends ImaRecordMatcher<IMABaselineRecord> {
    private static final Logger LOGGER = getLogger(ImaAcceptableRecordMatcher.class);

    /**
     * Construct a new ImaAcceptableRecordMatcher.
     *
     * @param records     the baseline records to use for matching
     * @param imaPolicy   the IMA policy to reference during matching; its partial path and path
     *                    equivalence settings influence matching behavior
     * @param imaBaseline the IMA baseline these records were sourced from; this is only used to
     */
    public ImaAcceptableRecordMatcher(
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
     * @return an IMAMatchStatus indicating whether the record is a match, mismatch, or unknown to
     *         the given baseline records
     */
    @Override
    public IMAMatchStatus<IMABaselineRecord> contains(final IMAMeasurementRecord record) {
        if (record == null) {
            throw new IllegalArgumentException("Cannot match on null record.");
        }

        final Set<IMABaselineRecord> matchRecords = new HashSet<>();
        final Set<IMABaselineRecord> mismatchRecords = new HashSet<>();

        final Collection<IMABaselineRecord> matchingRecords = getRelatedBaselineRecordsByPath(
                record
        );

        for (IMABaselineRecord baselineRecord : matchingRecords) {
            compareDigestsAndPopulateMatchLists(
                    baselineRecord, record, matchRecords, mismatchRecords
            );
        }

        if (matchRecords.isEmpty() && mismatchRecords.isEmpty()) {
            return new IMAMatchStatus<>(record, ReportMatchStatus.UNKNOWN, getImaBaseline());
        }

        if (matchRecords.isEmpty()) {
            return new IMAMatchStatus<>(
                    record, ReportMatchStatus.MISMATCH, mismatchRecords, getImaBaseline()
            );
        }

        return new IMAMatchStatus<>(
                record,
                ReportMatchStatus.MATCH,
                matchRecords,
                getImaBaseline()
        );
    }


    private static void compareDigestsAndPopulateMatchLists(
            final IMABaselineRecord baselineRecord,
            final IMAMeasurementRecord measurementRecord,
            final Set<IMABaselineRecord> matchRecords,
            final Set<IMABaselineRecord> mismatchRecords) {
        DigestComparisonResultType comparison =
                measurementRecord.getHash().compare(baselineRecord.getHash());

        switch (comparison) {
            case MATCH:
                matchRecords.add(baselineRecord);
                break;
            case MISMATCH:
                mismatchRecords.add(baselineRecord);
                break;
            default:
                LOGGER.warn("{} comparison result when comparing {} with {}.",
                        comparison, baselineRecord, measurementRecord);
                break;
        }
    }
}
