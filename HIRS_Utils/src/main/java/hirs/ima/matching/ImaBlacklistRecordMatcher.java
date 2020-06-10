package hirs.ima.matching;

import hirs.data.persist.IMAMeasurementRecord;
import hirs.data.persist.IMAPolicy;
import hirs.data.persist.baseline.ImaBaseline;
import hirs.data.persist.ImaBlacklistRecord;
import hirs.data.persist.enums.AlertType;
import hirs.data.persist.enums.ReportMatchStatus;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * This class extends the base matching functionality of {@link ImaRecordMatcher} to
 * compare {@link IMAMeasurementRecord}s against a collection of {@link ImaBlacklistRecord}s.
 */
public class ImaBlacklistRecordMatcher extends ImaRecordMatcher<ImaBlacklistRecord> {
    /**
     * Construct a new ImaBlacklistRecordMatcher.
     *
     * @param records     the baseline records to use for matching
     * @param imaPolicy   the IMA policy to reference during matching; its partial path and path
     *                    equivalence settings influence matching behavior
     * @param imaBaseline the IMA baseline these records were sourced from; this is only used to
     */
    public ImaBlacklistRecordMatcher(
            final Collection<ImaBlacklistRecord> records,
            final IMAPolicy imaPolicy,
            final ImaBaseline imaBaseline) {
        super(records, imaPolicy, imaBaseline);
    }

    /**
     * Returns an IMAMatchStatus indicating whether the given {@link IMAMeasurementRecord} is
     * contained within the originally provided {@link ImaBlacklistRecord}s.
     *
     * @param record the record to look up
     * @return an IMAMatchStatus indicating whether the record is a match, mismatch, or unknown to
     *         the given baseline records
     */
    public IMAMatchStatus<ImaBlacklistRecord> contains(final IMAMeasurementRecord record) {
        if (record == null) {
            throw new IllegalArgumentException("Cannot match on null record.");
        }

        final Set<ImaBlacklistRecord> matchesByPath = getRelatedBaselineRecordsByPath(record);
        final Set<ImaBlacklistRecord> matchesByHash = getRelatedBaselineRecordsByHash(record);
        final Set<ImaBlacklistRecord> matchingRecords = new HashSet<>();

        for (ImaBlacklistRecord blacklistRecord : matchesByPath) {
            if (blacklistRecord.getHash() == null || matchesByHash.contains(blacklistRecord)) {
                matchingRecords.add(blacklistRecord);
            }
        }

        for (ImaBlacklistRecord blacklistRecord : matchesByHash) {
            if (blacklistRecord.getPath() == null || matchesByPath.contains(blacklistRecord)) {
                matchingRecords.add(blacklistRecord);
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

    /**
     * Return the type of IMA blacklist alert that should be generated from the given list
     * of blacklist matches.  If all matches are of a single type, it will return the type of alert
     * as given by {@link ImaBlacklistRecord#getAlertMatchType()}.  Otherwise, it will return
     * <code>Alert.AlertType.IMA_BLACKLIST_MIXED_MATCH</code>.
     *
     * @param blacklistMatches the list of matches
     * @return the relevant alert type
     */
    public static AlertType getBlacklistAlertType(
            final Set<IMAMatchStatus<ImaBlacklistRecord>> blacklistMatches) {
        AlertType type = null;
        for (IMAMatchStatus<ImaBlacklistRecord> match : blacklistMatches) {
            for (ImaBlacklistRecord blacklistRecord : match.getBaselineRecords()) {
                if (type == null) {
                    type = blacklistRecord.getAlertMatchType();
                } else {
                    if (type != blacklistRecord.getAlertMatchType()) {
                        return AlertType.IMA_BLACKLIST_MIXED_MATCH;
                    }
                }
            }
        }

        return type;
    }
}
