package hirs.data.persist;

import com.google.common.base.Preconditions;
import hirs.ima.matching.BatchImaMatchStatus;
import hirs.ima.matching.IMAMatchStatus;
import hirs.ima.matching.ImaAcceptableHashRecordMatcher;
import hirs.ima.matching.ImaAcceptablePathAndHashRecordMatcher;
import hirs.ima.matching.ImaRecordMatcher;
import hirs.persist.ImaBaselineRecordManager;
import hirs.utils.Callback;
import org.hibernate.Criteria;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class defines the basis of operation for a baseline that supports querying
 * a persistence layer for its component {@link IMABaselineRecord}s.  A QueryableRecordImaBaseline
 * uses this functionality to implement its <code>contains</code> method.
 */
public abstract class QueryableRecordImaBaseline extends ImaAcceptableRecordBaseline {
    /**
     * Constructor used to initialize an <code>QueryableRecordImaBaseline</code> with a name.
     *
     * @param name the name of the new baseline
     */
    public QueryableRecordImaBaseline(final String name) {
        super(name);
    }

    /**
     * Default constructor necessary for Hibernate.
     */
    protected QueryableRecordImaBaseline() {
        super();
    }

    /**
     * Check membership of the given {@link IMAMeasurementRecord}s in this baseline.
     *
     * @param records the records to attempt to match
     * @param recordManager the {@link ImaBaselineRecordManager} to query
     * @param imaPolicy the IMA policy to use while determining if a baseline contains the records
     *
     * @return a collection of {@link IMAMatchStatus}es reflecting the results
     */
    @Override
    public final BatchImaMatchStatus<IMABaselineRecord> contains(
            final Collection<IMAMeasurementRecord> records,
            final ImaBaselineRecordManager recordManager,
            final IMAPolicy imaPolicy) {
        Preconditions.checkArgument(records != null, "records cannot be null");
        Preconditions.checkArgument(recordManager != null, "record manager cannot be null");

        final Collection<String> pathsToFind = new HashSet<>();
        for (IMAMeasurementRecord record : records) {
            if (record != null) {
                pathsToFind.addAll(ImaRecordMatcher.getMatchingPaths(imaPolicy, record.getPath()));
            }
        }

        Collection<IMABaselineRecord> retrievedRecords = recordManager.iterateOverBaselineRecords(
                this, new Callback<IMABaselineRecord, IMABaselineRecord>() {
                    @Override
                    public IMABaselineRecord call(final IMABaselineRecord baselineRecord) {
                        if (pathsToFind.contains(baselineRecord.getPath())) {
                            return baselineRecord;
                        } else if (imaPolicy.isPartialPathEnable()
                                && pathsToFind.contains(baselineRecord.getPartialPath())) {
                            return baselineRecord;
                        }
                        return null;
                    }
        });

        return new ImaAcceptablePathAndHashRecordMatcher(retrievedRecords, imaPolicy, this)
                .batchMatch(records);
    }

    /**
     * Check membership of the given {@link IMAMeasurementRecord}s in this baseline.
     *
     * @param records the records to attempt to match
     * @param recordManager the {@link ImaBaselineRecordManager} to query
     * @param imaPolicy the IMA policy to use while determining if a baseline contains the records
     *
     * @return a collection of {@link IMAMatchStatus}es reflecting the results
     */
    @Override
    public final BatchImaMatchStatus<IMABaselineRecord> containsHashes(
            final Collection<IMAMeasurementRecord> records,
            final ImaBaselineRecordManager recordManager,
            final IMAPolicy imaPolicy) {
        Preconditions.checkArgument(records != null, "records cannot be null");
        Preconditions.checkArgument(recordManager != null, "record manager cannot be null");

        final Set<Digest> hashesToFind = records.stream()
                .filter(Objects::nonNull)
                .map(IMAMeasurementRecord::getHash)
                .collect(Collectors.toSet());

        Collection<IMABaselineRecord> retrievedRecords = recordManager.iterateOverBaselineRecords(
                this, new Callback<IMABaselineRecord, IMABaselineRecord>() {
                    @Override
                    public IMABaselineRecord call(final IMABaselineRecord baselineRecord) {
                        if (hashesToFind.contains(baselineRecord.getHash())) {
                            return baselineRecord;
                        }
                        return null;
                    }
                });

        return new ImaAcceptableHashRecordMatcher(retrievedRecords, imaPolicy, this)
                .batchMatch(records);
    }

    @Override
    public final Collection<IMABaselineRecord> getRecordsExcept(
            final ImaBaselineRecordManager recordManager,
            final Set<IMABaselineRecord> foundRecords) {
        if (foundRecords == null) {
            throw new IllegalArgumentException("foundRecords cannot be null");
        }

        if (recordManager == null) {
            throw new IllegalArgumentException("ImaBaselineRecordManager cannot be null");
        }

        return recordManager.iterateOverBaselineRecords(
                this, new Callback<IMABaselineRecord, IMABaselineRecord>() {
            @Override
            public IMABaselineRecord call(final IMABaselineRecord param) {
                if (!foundRecords.contains(param)) {
                    return param;
                }
                return null;
            }
        });
    }

    /**
     * This method configures the provided criteria to retrieve all of its component
     * {@link IMABaselineRecord}s.  The given bucket should be used by the receiving code
     * to only retrieve {@link IMABaselineRecord}s that are in the given bucket.
     *
     * @param criteria the criteria to configure
     * @param bucket the bucket that should be configured on the criteria
     */
    public abstract void configureCriteriaForBaselineRecords(Criteria criteria, int bucket);
}
