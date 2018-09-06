package hirs.data.persist;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OrderColumn;
import javax.persistence.Transient;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


/**
 * Represents the criteria by which an <code>IMAAppraiser</code> appraises a
 * <code>Report</code>. This includes the following:
 * <p>&nbsp;
 * <ul>
 *   <li>A list of <code>ImaBlacklistBaseline</code>s to use a blacklists</li>
 *   <li>A list of <code>ImaIgnoreSetBaseline</code>'s to use as ignore sets</li>
 *   <li>A list of <code>ImaAcceptableRecordBaseline</code>s to use as whitelists</li>
 *   <li>A list of <code>ImaAcceptableRecordBaseline</code>s to use as required sets</li>
 *   <li>A flag to determine determine whether appraisal should fail on unknowns </li>
 *   <li>A flag to determine whether PCR 10 should be used to validate the hash of
 *       the <code>IMAReport</code></li>
 *   <li>A flag to determine whether delta reports should be requested</li>
 *   <li>A flag to determine whether partial path appraisal should be supported</li>
 * </ul>
 */
@Entity
public class IMAPolicy extends Policy implements HasBaselines {

    private static final Logger LOGGER = LogManager.getLogger(IMAPolicy.class);
    private static final boolean DEFAULT_UNKNOWN_POLICY = false;
    private static final boolean DEFAULT_PCR_VALIDATION = false;
    private static final boolean DEFAULT_DELTA_REPORT_POLICY = true;
    private static final boolean DEFAULT_PARTIAL_PATH_POLICY = true;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "Whitelists",
            joinColumns = {@JoinColumn(
                    name = "PolicyID", nullable = false) })
    @OrderColumn(name = "WhitelistsIndex")
    private final List<ImaAcceptableRecordBaseline> whitelists = new LinkedList<>();

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "RequiredSets",
            joinColumns = {@JoinColumn(
                    name = "PolicyID", nullable = false) })
    @OrderColumn(name = "RequiredSetsIndex")
    private final List<ImaAcceptableRecordBaseline> requiredSets = new LinkedList<>();

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "IgnoreSets",
            joinColumns = {@JoinColumn(
                    name = "PolicyID", nullable = false) })
    @OrderColumn(name = "IgnoreSetsIndex")
    private final List<ImaIgnoreSetBaseline> ignoreSets = new LinkedList<>();

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "ImaBlacklists",
            joinColumns = {@JoinColumn(
                    name = "PolicyID", nullable = false) })
    @OrderColumn(name = "ImaBlacklistsIndex")
    private final List<ImaBlacklistBaseline> blacklists = new LinkedList<>();

    @Column(nullable = false)
    private boolean failOnUnknowns = DEFAULT_UNKNOWN_POLICY;
    @Column(nullable = false)
    private boolean validatePcr = DEFAULT_PCR_VALIDATION;
    @Column(nullable = false)
    private boolean deltaReportEnable = DEFAULT_DELTA_REPORT_POLICY;
    @Column(nullable = false)
    private boolean partialPathEnable = DEFAULT_PARTIAL_PATH_POLICY;

    @Transient
    private Multimap<String, String> pathEquivalences = null;

    /**
     * Constructor used to initialize IMAPolicy object.
     *
     * @param name a name used to uniquely identify and reference the IMA policy
     */
    public IMAPolicy(final String name) {
        super(name);
    }

    /**
     * Constructor used to initialize IMAPolicy object.
     *
     * @param name        a name used to uniquely identify and reference the IMA policy
     * @param description optional description that may be provided by the user
     */
    public IMAPolicy(final String name, final String description) {
        super(name, description);
    }

    /**
     * Default constructor necessary for Hibernate.
     */
    protected IMAPolicy() {
        super();
    }

    /**
     * Deprecated method to remove all whitelists currently contained in
     * <code>this.whitelists</code> and add the given whitelist. Replaced by
     * <code>setWhitelists</code>.
     *
     * @param imaBaseline baseline that represents the new white list, can be NULL
     */
    public final void setWhitelist(final ImaAcceptableRecordBaseline imaBaseline) {
        LOGGER.debug("adding ImaBaseline {} as {} policy whitelist",
                imaBaseline, getName());
        if (imaBaseline == null) {
            this.whitelists.clear();
        } else {
            this.whitelists.clear();
            this.whitelists.add(imaBaseline);
        }
    }

    /**
     * Replace the current <code>List</code> of whitelists with a new
     * <code>List</code>. Can be empty but cannot be null. The order of the
     * <code>List</code> will be the order used for appraisal by the
     * <code>TPMAppraiser</code>.
     *
     * @param newWhitelists list of <code>ImaBaseline</code>s to use as whitelists, can be
     *                      empty but not null
     */
    public final void setWhitelists(
            final List<? extends ImaAcceptableRecordBaseline> newWhitelists
    ) {
        if (newWhitelists == null) {
            throw new PolicyException("cannot set whitelists to null");
        }
        LOGGER.debug("setting new whitelists for {}", getName());
        whitelists.clear();
        whitelists.addAll(newWhitelists);
    }

    /**
     * Deprecated method to return the first whitelist in
     * <code>this.whitelists</code>. Replaced by <code>getWhitelists</code>.
     *
     * @return the baseline representing the first whitelist, can be NULL
     */
    public final ImaAcceptableRecordBaseline getWhitelist() {
        if (this.whitelists == null || this.whitelists.isEmpty()) {
            return null;
        }
        return this.whitelists.get(0);
    }

    /**
     * Return the current list of whitelists.
     *
     * @return the unmodifiable list of whitelists, cannot be null
     */
    public final List<ImaAcceptableRecordBaseline> getWhitelists() {
        return Collections.unmodifiableList(this.whitelists);
    }

    /**
     * Deprecated method to remove all required sets currently contained in
     * <code>this.requiredSets</code> and add the given required set. Replaced
     * by <code>setRequiredSets</code>.
     *
     * @param imaBaseline baseline that represents the new required set, can be NULL
     */
    public final void setRequiredSet(final ImaAcceptableRecordBaseline imaBaseline) {
        LOGGER.debug("adding ImaBaseline {} as {} policy required set",
                imaBaseline, getName());
        if (imaBaseline == null) {
            this.requiredSets.clear();
        } else {
            this.requiredSets.clear();
            this.requiredSets.add(imaBaseline);
        }
    }

    /**
     * Replace the current <code>List</code> of required sets with a new
     * <code>List</code>. Can be empty but cannot be null. The order of the
     * <code>List</code> will be the order used for appraisal by the
     * <code>TPMAppraiser</code>.
     *
     * @param newRequiredSets list of <code>ImaBaseline</code>s to use as required sets, can
     *                        be empty but not null
     */
    public final void setRequiredSets(
            final List<? extends ImaAcceptableRecordBaseline> newRequiredSets
    ) {
        if (newRequiredSets == null) {
            throw new PolicyException("cannot set required sets to null");
        }
        LOGGER.debug("setting new required sets for {}", getName());
        this.requiredSets.clear();
        for (ImaAcceptableRecordBaseline baseline : newRequiredSets) {
            this.requiredSets.add(baseline);
        }
    }

    /**
     * Deprecated method to return the first required set in
     * <code>this.requiredSets</code>. Replaced by <code>getrequiredSets</code>.
     *
     * @return the baseline representing the first required set, can be NULL
     */
    public final ImaBaseline getRequiredSet() {
        if (this.requiredSets == null || this.requiredSets.isEmpty()) {
            return null;
        }
        return this.requiredSets.get(0);
    }

    /**
     * Return the current list of required sets.
     *
     * @return the unmodifiable list of required sets, cannot be null
     */
    public final List<ImaAcceptableRecordBaseline> getRequiredSets() {
        return Collections.unmodifiableList(this.requiredSets);
    }

    /**
     * Deprecated method to remove all ignore sets currently contained in
     * <code>this.ignoreSets</code> and add the given ignore set. Replaced
     * by <code>setIgnoreSets</code>.
     *
     * @param imaIgnoreSetBaseline set of optional records that represents the new ignore set,
     *                             can be NULL
     */
    public final void setImaIgnoreSetBaseline(final ImaIgnoreSetBaseline
                                                      imaIgnoreSetBaseline) {
        LOGGER.debug("adding optionalSetPolicy {} as {} policy ignore set",
                imaIgnoreSetBaseline, getName());
        if (imaIgnoreSetBaseline == null) {
            this.ignoreSets.clear();
        } else {
            this.ignoreSets.clear();
            this.ignoreSets.add(imaIgnoreSetBaseline);

        }
    }

    /**
     * Replace the current <code>List</code> of ignore sets with a new
     * <code>List</code>. Can be empty but cannot be null. The order of the
     * <code>List</code> will be the order used for appraisal by the
     * <code>TPMAppraiser</code>.
     *
     * @param newIgnoreSets list of <code>OptionalSetPolicy</code>'s to use as required
     *                      sets, can be empty but not null
     */
    public final void setIgnoreSets(final List<? extends ImaIgnoreSetBaseline> newIgnoreSets) {
        if (newIgnoreSets == null) {
            throw new PolicyException("cannot set ignore sets to null");
        }
        LOGGER.debug("setting new ignore sets for {}", getName());
        this.ignoreSets.clear();
        for (ImaIgnoreSetBaseline oSet : newIgnoreSets) {
            this.ignoreSets.add(oSet);
        }
    }

    /**
     * Deprecated method to return the first ignore set in
     * <code>this.ignoreSets</code>. Replaced by <code>getIgnoreSets</code>.
     *
     * @return the optional set policy representing the first ignore set, can be
     * NULL
     */
    public final ImaIgnoreSetBaseline getImaIgnoreSetBaseline() {
        if (this.ignoreSets == null || this.ignoreSets.isEmpty()) {
            return null;
        }
        return this.ignoreSets.get(0);
    }

    /**
     * Return the current list of ignore sets.
     *
     * @return the unmodifiable list of ignore sets, which cannot be null
     */
    public final List<ImaIgnoreSetBaseline> getIgnoreSets() {
        return Collections.unmodifiableList(this.ignoreSets);
    }

    /**
     * Set the IMA blacklists for this IMA policy.
     *
     * @param newBlacklists the blacklists to assign to this policy
     */
    public final void setBlacklists(final List<ImaBlacklistBaseline> newBlacklists) {
        if (newBlacklists == null) {
            throw new PolicyException("Cannot set blacklists to null");
        }
        LOGGER.debug("setting new blacklists for {}: {}", getName(), newBlacklists);
        blacklists.clear();
        blacklists.addAll(newBlacklists);
    }

    /**
     * Retrieve this policy's IMA blacklists.
     *
     * @return this policy's blacklists
     */
    public final List<ImaBlacklistBaseline> getBlacklists() {
        return Collections.unmodifiableList(this.blacklists);
    }

    /**
     * Returns the policy on whether appraisal should fail if any unknown files
     * are found. During the IMA appraisal process unknown files may be
     * encountered. This boolean indicates if an appraisal should fail for any
     * unknown files that are found.
     *
     * @return appraisal failure policy if any unknowns found
     */
    public final boolean isFailOnUnknowns() {
        return failOnUnknowns;
    }

    /**
     * Set the policy on whether appraisal should fail if any unknown files
     * are found. See {@link #isFailOnUnknowns()} for more details.
     *
     * @param failOnUnknowns appraisal failure policy if any unknowns found
     */
    public final void setFailOnUnknowns(final boolean failOnUnknowns) {
        LOGGER.debug("setting fail on unknowns policy to {}", failOnUnknowns);
        this.failOnUnknowns = failOnUnknowns;
    }

    /**
     * Returns a boolean as to whether or not the PCR hash should be validated.
     * An IMA report that has a TPM enabled has a running hash stored in a TPM,
     * typically PCR 10. If this is set to true then the running hash of the IMA
     * report is regenerated during the appraisal process and compared to the
     * PCR.
     * <p>
     * If this is enabled a <code>TPMReport</code> must be in the
     * <code>IntegrityReport</code>.
     *
     * @return the validatePcr
     */
    public final boolean isValidatePcr() {
        return validatePcr;
    }

    /**
     * Enable the validation of PCR hash. See {@link #isValidatePcr()} for a
     * complete description of this property.
     *
     * @param validatePcr the validatePcr to set
     */
    public final void setValidatePcr(final boolean validatePcr) {
        LOGGER.debug("setting validate pcr policy to {}", validatePcr);
        this.validatePcr = validatePcr;
    }

    /**
     * Returns a boolean as to whether IMA reports are enabled for
     * delta reports.
     *
     * @return deltaReportEnable
     */
    public final boolean isDeltaReportEnable() {
        return deltaReportEnable;
    }

    /**
     * Set the indicator that determines if delta reports are enabled
     * within a <code>IntegrityReport</code>.
     *
     * @param deltaReportEnable the flag for delta reports
     */
    public final void setDeltaReportEnable(final boolean deltaReportEnable) {
        LOGGER.debug("setting deltaReportEnable to {}", deltaReportEnable);
        this.deltaReportEnable = deltaReportEnable;
    }

    @Override
    public final List<Baseline> getBaselines() {
        List<Baseline> baselines = new LinkedList<>();
        baselines.addAll(getRequiredSets());
        baselines.addAll(getWhitelists());
        baselines.addAll(getBlacklists());
        baselines.addAll(getIgnoreSets());
        return baselines;
    }

    /**
     * Checks whether partial path support is enabled or not for the whitelists,
     * required sets, and ignore sets contained in this <code>Policy</code>.
     * When partial path support is enabled the full file path for records and
     * baselines is not used when calling
     * <code>ImaBaseline#contains(IMAMeasurementRecord)</code>.
     * Instead only the file name is used. For example, if a record has the file
     * path "gradle" then its path will match against baseline records with the
     * file paths "/usr/bin/gradle" and "/home/foo/bin/gradle."
     * <p>
     * This feature should be enabled when comparing against IMA reports from
     * kernels that do not have the latest IMA code. The earlier IMA code did
     * not always report the absolute file path for each record. This is true
     * for CentOS versions prior to CentOS 7.
     *
     * @return partialPathEnable
     */
    public final boolean isPartialPathEnable() {
        return partialPathEnable;
    }

    /**
     * Sets whether partial path support is enabled or not for the whitelists.
     * See {@link #isPartialPathEnable()} for more details about the flag.
     *
     * @param partialPathEnable boolean determining whether partial paths are used
     */
    public final void setPartialPathEnable(final boolean partialPathEnable) {
        LOGGER.debug("setting partialPathEnable to {}", partialPathEnable);
        this.partialPathEnable = partialPathEnable;
    }

    /**
     * Returns a Multimap representing that paths that should be considered
     * 'equivalent' while evaluating an IMA baseline contained by this policy.
     * Equivalent paths are those which could potentially hold the same files, or links
     * to those same files.  For instance, on CentOS 7, /bin is symlinked to /usr/bin, so the
     * IMA log may report equivalent entries at both /bin/file and /usr/bin/file.
     * Creating a mapping between /bin/ and /usr/bin/ allows IMA appraisal to equate the
     * two paths.
     * <p>
     * The map returned will have directories as keys, and those directories' equivalent
     * directories as its collection of values.  These relationships are bidirectional; that is,
     * if a is equivalent to b and c, b is equivalent to a, and c is also equivalent to a.
     *
     * @return a Multimap relating equivalent directories as described above
     */
    public final synchronized Multimap<String, String> getPathEquivalences() {
        // if we've already set pathEquivalences, return it
        if (pathEquivalences != null) {
            return pathEquivalences;
        }

        Multimap<String, String> equivalentPaths = HashMultimap.create();

        // define equivalences
        equivalentPaths.put("/bin/", "/usr/bin/");
        equivalentPaths.put("/lib/", "/usr/lib/");
        equivalentPaths.put("/lib64/", "/usr/lib64/");

        // populate inverse relationships
        Multimap<String, String> bidirectionalEquivalences = HashMultimap.create();
        for (Map.Entry<String, Collection<String>> equivalentPathPair
                : equivalentPaths.asMap().entrySet()) {
            String dir = equivalentPathPair.getKey();
            Collection<String> equivalentDirs = equivalentPathPair.getValue();

            bidirectionalEquivalences.putAll(dir, equivalentDirs);

            for (String equivalentDir : equivalentDirs) {
                bidirectionalEquivalences.put(equivalentDir, dir);
            }
        }

        this.pathEquivalences = bidirectionalEquivalences;
        return this.pathEquivalences;
    }
}
