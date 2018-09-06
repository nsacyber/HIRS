package hirs.data.persist;

import hirs.appraiser.Appraiser;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;

/**
 * A <code>HIRSPolicy</code> is a <code>Policy</code> that specifies the
 * sub-appraisers which a <code>HIRSAppraiser</code> should call.
 * <p>
 * When the user sets this <code>Set</code> of <code>Appraiser</code>s,
 * <code>HIRSPolicy</code> checks whether any of the <code>Appraiser</code>s
 * requires any <code>Report</code>s that will not be requested when a
 * <code>HIRSAppraiser</code> has gotten report requests from the entire set.
 * This allows the user to be notified if a <code>HIRSPolicy</code> is set with
 * any missing dependencies.
 */

@Entity
public class HIRSPolicy extends Policy {
    /**
     * Name of the default HIRS Policy.
     */
    public static final String DEFAULT_HIRS_POLICY_NAME = "Default HIRS Policy";

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "RequiredAppraisers",
        joinColumns = { @JoinColumn(name = "PolicyID", nullable = false) })
    private Set<Class<? extends Appraiser>> requiredAppraisers = new HashSet<>();

    /**
     * Constructor used to initialize a HIRSPolicy object.
     *
     * @param name
     *            a name used to uniquely identify and reference the HIRS policy
     */
    public HIRSPolicy(final String name) {
        super(name);
    }

    /**
     * Constructor used to initialize a HIRSPolicy object.
     *
     * @param name
     *            a name used to uniquely identify and reference the HIRS policy
     * @param description
     *        Optional description of the policy that can be added by the user
     */
    public HIRSPolicy(final String name, final String description) {
        super(name, description);
    }

    /**
     * Default constructor necessary for Hibernate.
     */
    protected HIRSPolicy() {
        this("temporary hibernate name", "");
    }

    /**
     * Sets this <code>HIRSPolicy</code>'s requiredAppraisers set.
     *
     * @param appraiserSet set of generic <code>Class&lt;Appraiser&gt;</code> objects
     */
    public final void setRequiredAppraisers(final Set<Class<? extends Appraiser>> appraiserSet) {
        requiredAppraisers = new HashSet<>(appraiserSet);
    }

    /**
     * Returns the <code>Set</code> of sub-appraisers which a
     * <code>HIRSAppraiser</code> should call.
     *
     * @return the set of sub-appraisers required to be called in
     * <code>HIRSAppraiser</code>
     */
    public final Set<Class<? extends Appraiser>> getRequiredAppraisers() {
        return Collections.unmodifiableSet(requiredAppraisers);
    }


    /**
     * Generates the name for the HIRS Policy.
     *
     * @param group The group related to the HIRS Policy
     * @return The name of the Policy
     */
    public static String nameFromGroup(final DeviceGroup group) {
        return "hirspolicy_" + group.getId();
    }
}
