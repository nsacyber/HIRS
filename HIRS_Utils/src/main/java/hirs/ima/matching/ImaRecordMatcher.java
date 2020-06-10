package hirs.ima.matching;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Multimap;
import hirs.data.persist.Digest;
import hirs.data.persist.baseline.IMABaselineRecord;
import hirs.data.persist.IMAMeasurementRecord;
import hirs.data.persist.IMAPolicy;
import hirs.data.persist.baseline.AbstractImaBaselineRecord;
import hirs.data.persist.baseline.ImaBaseline;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This class contains the logic used to match IMA measurement records against
 * IMA baseline records.  Given a collection of IMABaselineRecords, an IMAPolicy,
 * and an ImaBaseline, it is able to determine which measurement records should be considered
 * matches, mismatches, or unknown to the given set of baseline records.
 *
 * @param <T> the type of IMA baseline record that this class matches against
 */
public abstract class ImaRecordMatcher<T extends AbstractImaBaselineRecord> {
    private final IMAPolicy imaPolicy;
    private final ImaBaseline imaBaseline;
    private final Collection<T> records;

    // lookup maps
    private Multimap<String, T> pathMap = null;
    private Multimap<String, T> hashMap = null;

    /**
     * Construct a new IMARecordMatcher.
     *
     * @param records the baseline records to use for matching
     * @param imaPolicy the IMA policy to reference during matching; its partial path and path
     *                  equivalence settings influence matching behavior
     * @param imaBaseline the IMA baseline these records were sourced from; this is only used to
     *                    record references to mismatched records
     */
    public ImaRecordMatcher(
            final Collection<T> records,
            final IMAPolicy imaPolicy,
            final ImaBaseline imaBaseline) {
        this.records = records;
        this.imaPolicy = imaPolicy;
        this.imaBaseline = imaBaseline;
    }

    /**
     * Retrieve the baseline associated with this record matcher.
     *
     * @return the associated IMA baseline
     */
    protected ImaBaseline getImaBaseline() {
        return this.imaBaseline;
    }

    /**
     * Returns an IMAMatchStatus indicating whether the given {@link IMAMeasurementRecord} is
     * contained within the originally provided baseline records.
     *
     * @param record the record to look up
     * @return an IMAMatchStatus indicating whether the record is a match, mismatch, or unknown to
     *         the given baseline records
     */
    public abstract IMAMatchStatus<T> contains(IMAMeasurementRecord record);

    /**
     * Given a collection of measurement records, populate and return a BatchImaMatchStatus
     * instance containing the match results according to this ImaRecordMatcher's matching
     * behavior and the given IMA policy, baseline, and baseline records.
     *
     * @param records the measurement records to match to baseline records
     * @return a BatchImaMatchStatus containing the match status of all the given records
     */
    public BatchImaMatchStatus<T> batchMatch(final Collection<IMAMeasurementRecord> records) {
        List<IMAMatchStatus<T>> matchStatuses = new ArrayList<>();
        for (IMAMeasurementRecord record : records) {
            matchStatuses.add(contains(record));
        }
        return new BatchImaMatchStatus<>(matchStatuses);
    }

    /**
     * Gets all IMA baseline records that are related to the given IMA measurement record
     * as determined by path similarity or equivalency.  This method respects the IMA policy
     * that was given at construction time with respect to partial path matching.
     *
     * @param record the record for which all matching IMA baseline records should be returned
     * @return the resulting set of IMA baseline records
     */
    protected Set<T> getRelatedBaselineRecordsByPath(final IMAMeasurementRecord record) {
        if (record == null) {
            throw new IllegalArgumentException("Cannot match on null record.");
        }

        if (pathMap == null) {
            pathMap = createPathMap(this.records);
        }

        final Set<T> matchingRecords = new HashSet<>();
        matchingRecords.addAll(pathMap.get(record.getPath()));

        if (imaPolicy.isPartialPathEnable() && isFullPath(record.getPath())) {
            for (T matchingPartialRecord
                    : pathMap.get(IMABaselineRecord.getPartialPath(record.getPath()))) {

                // ensure that we're not about to match two unequal full paths
                if (isFullPath(matchingPartialRecord.getPath())
                        && !matchingPartialRecord.getPath().equals(record.getPath())) {
                    continue;
                } else {
                    matchingRecords.add(matchingPartialRecord);
                }
            }
        }

        return matchingRecords;
    }

    /**
     * Gets all IMA baseline records that are related to the given IMA measurement record
     * as determined by their hash values.
     *
     * @param record the record for which all matching IMA baseline records should be returned
     * @return the resulting set of IMA baseline records
     */
    protected Set<T> getRelatedBaselineRecordsByHash(final IMAMeasurementRecord record) {
        if (record == null) {
            throw new IllegalArgumentException("Cannot match on null record.");
        }

        if (hashMap == null) {
            hashMap = createHashMap(this.records);
        }

        return new HashSet<>(hashMap.get(record.getHash().toString()));
    }

    private Multimap<String, T> createPathMap(
            final Collection<T> imaBaselineRecords) {
        ImmutableListMultimap.Builder<String, T> mapBuilder =
                ImmutableListMultimap.builder();

        for (T record : imaBaselineRecords) {
            if (record.getPath() != null) {
                for (String matchingPath : getMatchingPaths(imaPolicy, record.getPath())) {
                    mapBuilder.put(matchingPath, record);
                }
            }
        }

        return mapBuilder.build();
    }

    private Multimap<String, T> createHashMap(
            final Collection<T> imaBaselineRecords) {
        ImmutableListMultimap.Builder<String, T> mapBuilder =
                ImmutableListMultimap.builder();

        for (T record : imaBaselineRecords) {
            Digest hash = record.getHash();
            if (hash != null) {
                mapBuilder.put(hash.toString(), record);
            }
        }

        return mapBuilder.build();
    }

    /**
     * Calculates all paths that should be considered as 'matching' the given path, according to
     * the given IMAPolicy, including the original path itself.  For instance, if partial paths are
     * enabled, the path's filename will be included in the returned collection.  Additionally, if
     * the IMA policy is configured with equivalent paths, these are evaluated against the given
     * path and any relevant permutations are returned as well.
     *
     * @param imaPolicy the IMAPolicy to use in calculating matching paths
     * @param targetPath the original path whose matching paths will calculated and returned
     * @return a collection of paths that this IMAPolicy would consider as matching the path
     */
    public static Collection<String> getMatchingPaths(
            final IMAPolicy imaPolicy,
            final String targetPath) {
        Multimap<String, String> pathEquivalences = imaPolicy.getPathEquivalences();
        Set<String> pathsToFind = new HashSet<>();

        pathsToFind.add(targetPath);
        for (String path : pathEquivalences.keySet()) {
            if (targetPath.startsWith(path)) {
                for (String equivalentPath : pathEquivalences.get(path)) {
                    pathsToFind.add(targetPath.replaceFirst(path, equivalentPath));
                }
            }
        }

        if (imaPolicy.isPartialPathEnable() && isFullPath(targetPath)) {
            pathsToFind.add(IMABaselineRecord.getPartialPath(targetPath));
        }

        return pathsToFind;
    }

    private static boolean isFullPath(final String path) {
        return path.startsWith("/");
    }
}
