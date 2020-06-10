package hirs.data.persist.baseline;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Preconditions;
import hirs.data.persist.IMAMeasurementRecord;
import hirs.data.persist.IMAPolicy;
import hirs.data.persist.ImaBlacklistRecord;
import hirs.ima.matching.BatchImaMatchStatus;
import hirs.ima.matching.ImaBlacklistRecordMatcher;
import hirs.persist.ImaBaselineRecordManager;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * This class holds baseline records that represent known undesirable
 * facts, such as the existence of an IMA log entry with a certain
 * filename, or a certain hash, or both.
 */
@Entity
public class ImaBlacklistBaseline extends ImaBaseline<ImaBlacklistRecord> {
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER,
            orphanRemoval = true, mappedBy = "baseline")
    @JsonIgnore
    private Set<ImaBlacklistRecord> imaBlacklistRecords;

    /**
     * Construct a new ImaBlacklistBaseline.
     *
     * @param name the name of the new baseline
     */
    public ImaBlacklistBaseline(final String name) {
        super(name);
        imaBlacklistRecords = new LinkedHashSet<>();
    }

    /**
     * Default constructor necessary for Hibernate.
     */
    protected ImaBlacklistBaseline() {
        super();
        imaBlacklistRecords = null;
    }

    @Override
    public BatchImaMatchStatus<ImaBlacklistRecord> contains(
            final Collection<IMAMeasurementRecord> records,
            final ImaBaselineRecordManager recordManager,
            final IMAPolicy imaPolicy) {
        Preconditions.checkArgument(records != null, "Records cannot be null");
        Preconditions.checkArgument(imaPolicy != null, "IMA policy cannot be null");
        return new ImaBlacklistRecordMatcher(imaBlacklistRecords, imaPolicy, this)
                .batchMatch(records);
    }

    /**
     * Adds an {@link ImaBlacklistRecord} to this baseline.
     *
     * @param record the record to add to this baseline
     * @return true if the record was added to this baseline; false otherwise
     */
    public final boolean addToBaseline(final ImaBlacklistRecord record) {
        record.setBaseline(this);
        return imaBlacklistRecords.add(record);
    }

    /**
     * Removes an {@link ImaBlacklistRecord} to this baseline.
     *
     * @param record the record to remove from this baseline
     * @return true if the record was removed from this baseline; false otherwise
     */
    public final boolean removeFromBaseline(final ImaBlacklistRecord record) {
        record.setBaseline(null);
        return imaBlacklistRecords.remove(record);
    }

    /**
     * Associates the given records with this baseline.
     *
     * @param records the records that the baseline should contain
     */
    public final void setBaselineRecords(final Set<ImaBlacklistRecord> records) {
        Preconditions.checkNotNull(records);
        imaBlacklistRecords.clear();
        imaBlacklistRecords.addAll(records);
        for (ImaBlacklistRecord record : records) {
            record.setBaseline(this);
        }
    }

    /**
     * Returns the set of blacklist records in this baseline.
     *
     * @return the set of blacklist records contained in this baseline
     */
    @JsonIgnore
    public final Set<ImaBlacklistRecord> getRecords() {
        return Collections.unmodifiableSet(imaBlacklistRecords);
    }
}
