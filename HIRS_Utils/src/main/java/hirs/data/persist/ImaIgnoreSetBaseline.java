/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hirs.data.persist;

import com.fasterxml.jackson.annotation.JsonIgnore;

import hirs.ima.matching.BatchImaMatchStatus;
import hirs.ima.matching.IMAMatchStatus;
import hirs.ima.matching.ImaIgnoreSetRecordMatcher;
import hirs.persist.ImaBaselineRecordManager;
import hirs.utils.RegexFilePathMatcher;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;

import static org.apache.logging.log4j.LogManager.getLogger;

import org.apache.logging.log4j.Logger;

/**
 * This class holds baseline records that indicate which measurements should
 * be ignored in an IMA measurement log, as determined by their paths.
 */
@Entity
@Access(AccessType.FIELD)
public class ImaIgnoreSetBaseline extends ImaBaseline<ImaIgnoreSetRecord> {
    private static final Logger LOGGER = getLogger(ImaIgnoreSetBaseline.class);

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER,
            orphanRemoval = true, mappedBy = "baseline")
    @Access(AccessType.PROPERTY)
    @JsonIgnore
    private Set<ImaIgnoreSetRecord> imaIgnoreSetRecords;

    /**
     * Constructor used to initialize ImaIgnoreSetBaseline object. Makes an empty
     * <code>Set</code> of Strings and passes up the user provided name.
     *
     * @param name
     *            a name used to uniquely identify and reference the IMA ignore set
     */
    public ImaIgnoreSetBaseline(final String name) {
        super(name);
        imaIgnoreSetRecords = new HashSet<>();
    }

    /**
     * Default constructor necessary for Hibernate. Makes an empty
     * <code>Set</code> of Strings and passes up the user provided name.
     */
    protected ImaIgnoreSetBaseline() {
        super();
        imaIgnoreSetRecords = new HashSet<>();
    }

   /**
     * Adds an IMA ignore set record to this ignore set baseline. If the record is not already in
     * the list, it is added. If the record already exists in the list, then this method will
     * quietly ignore the request because it already exists in the list.
     *
     * @param record
     *            ignore set record to be added to the ignore set
     * @return
     *      returns true if record was added to the baseline, false if it wasn't
     *
     * @throws IllegalArgumentException if the new record has a malformed matcher pattern
     */
    public final boolean addToBaseline(final ImaIgnoreSetRecord record)
            throws IllegalArgumentException {
        validateMatcherPattern(record.getPath());
        record.setOnlyBaseline(this);
        return addOnlyToBaseline(record);
    }

    /**
     * Checks whether or not the given ignore record path can be used as a valid pattern according
     * to this ignore set's matcher.
     *
     * @throws IllegalArgumentException if an ignore set record has a path that cannot be used as a
     *          valid pattern by this ignore set's matcher
     */
    private void validateMatcherPattern(final String ignoreRecordPattern)
            throws IllegalArgumentException {

        // the exception will be thrown here if a pattern cannot be added to the matcher
        RegexFilePathMatcher matcher = new RegexFilePathMatcher("default pattern");
        matcher.setPatterns(ignoreRecordPattern);
    }

    /**
     * Remove IMA Ignore Set record from baseline.
     *
     * @param record
     *            to remove from baseline
     * @return a boolean indicated whether or not the ima ignore record was
     *         successfully removed from the list.
     */
    public final boolean removeFromBaseline(final ImaIgnoreSetRecord record) {
        LOGGER.debug("removing record {} from baseline {}", record, getName());
        if (record == null) {
            LOGGER.error("null record");
            return false;
        }

        boolean retVal = imaIgnoreSetRecords.remove(record);
        if (retVal) {
            record.setBaseline(null);
        }

        LOGGER.debug("record removed: {}", record);
        return retVal;
    }

    /**
     * Returns a BatchImaMatchStatus indicating, for each of the given records, whether a matching
     * entry is contained in this ignore set baseline.  The match status will be MATCH if one
     * of the following cases applies:
     *
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
     * Otherwise, a record's match status will be UNKNOWN.
     *
     * @param records
     *            measurement records to find in this baseline
     * @param recordManager
     *            an ImaBaselineRecordManager that can be used to retrieve persisted records
     * @param imaPolicy
     *            the IMA policy to use while determining if a baseline contains the given records
     *
     * @return a BatchImaMatchStatus containing the match status for the given records
     */
    public final BatchImaMatchStatus<ImaIgnoreSetRecord> contains(
            final Collection<IMAMeasurementRecord> records,
            final ImaBaselineRecordManager recordManager,
            final IMAPolicy imaPolicy
    ) {
        if (records == null) {
            throw new IllegalArgumentException("Records cannot be null");
        }

        if (imaPolicy == null) {
            throw new IllegalArgumentException("IMA policy cannot be null");
        }

        ImaIgnoreSetRecordMatcher recordMatcher =
                new ImaIgnoreSetRecordMatcher(imaIgnoreSetRecords, imaPolicy, this);
        List<IMAMatchStatus<ImaIgnoreSetRecord>> matchStatuses = new ArrayList<>();
        for (IMAMeasurementRecord record : records) {
            matchStatuses.add(recordMatcher.contains(record));
        }
        return new BatchImaMatchStatus<>(matchStatuses);
    }

    /**
     * Returns the set of file paths in this IMA Ignore Set in this Baseline.
     *
     * @return list of optional measurement records representing optional list
     */
    @JsonIgnore
    public final synchronized Set<ImaIgnoreSetRecord> getImaIgnoreRecords() {
        return Collections.unmodifiableSet(imaIgnoreSetRecords);
    }

    /**
     * Returns the actual <code>Set</code> that contains the IMA records.  Needed for
     * Hibernate due to the AccessType.FIELD configuration on the IMA baseline classes.
     *
     * @return IMA ignore set records
     */
    private Set<ImaIgnoreSetRecord> getImaIgnoreSetRecords() {
        return imaIgnoreSetRecords;
    }

    /**
     * Sets the IMA Ignore records.  Needed for Hibernate due to the AccessType.FIELD
     * configuration on the IMA baseline classes.
     *
     * @param imaIgnoreSetRecords IMA ignore set records to set
     */
    private void setImaIgnoreSetRecords(final Set<ImaIgnoreSetRecord> imaIgnoreSetRecords) {
        this.imaIgnoreSetRecords = imaIgnoreSetRecords;
    }

    /**
     * Adds an IMA ignore record to this IMA ignore baseline. If the record does not exist
     * then it is added. If an equal record exists, based upon
     * {@link ImaIgnoreSetRecord#equals(Object)}, then this method quietly ignores the
     * request to add the record because one already exists in the baseline.
     *
     * @param record
     *            record to add to baseline
     * @return
     *      returns true is the record was added to the list, false if not
     */
    final synchronized boolean addOnlyToBaseline(final ImaIgnoreSetRecord record) {
        if (record == null) {
            LOGGER.error("invalid parameter (NULL value) "
                    + "passed to ImaIgnoreSetBaseline.addOnlyToBaseline");
            throw new IllegalArgumentException("null ignore set record");
        }
        if (imaIgnoreSetRecords.add(record)) {
            LOGGER.info("added file path " + record.getPath()
                    + " to ImaIgnoreSetBaseline " + getName());
        } else {
            LOGGER.info("file path" + record.getPath()
                    + " already added to ImaIgnoreSetBaseline " + getName());
            return false;
        }

        return true;
    }

    /**
     * Remove IMA Ignore record from the baseline.
     *
     * @param record
     *            record to remove
     * @return a boolean indicating if the removal was successful
     */
    final boolean removeOnlyBaseline(final ImaIgnoreSetRecord record) {
        return imaIgnoreSetRecords.remove(record);
    }
}
