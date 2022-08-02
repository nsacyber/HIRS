package hirs.persist;

import static org.apache.logging.log4j.LogManager.getLogger;
import hirs.appraiser.Appraiser;
import hirs.data.persist.policy.Policy;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.apache.logging.log4j.Logger;

/**
 * Maps an <code>Appraiser</code> to its default <code>Policy</code>. This class
 * is used by the <code>DBPolicyManager</code> to record this relationship.
 */
@Entity
@Table(name = "DefaultPolicy", uniqueConstraints = {@UniqueConstraint(
        columnNames = {"Appraiser_ID", "Device_Group_ID" }) })
public final class PolicyMapper {

    private static final Logger LOGGER = getLogger(PolicyMapper.class);

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "PolicyMapperID")
    private Long id;
    @ManyToOne
    @JoinColumn(nullable = false, name = "Appraiser_ID")
    private final Appraiser appraiser;
    @ManyToOne
    @JoinColumn(nullable = true, name = "Policy_ID")
    private Policy policy;
    @ManyToOne
    @JoinColumn(nullable = true, name = "Device_Group_ID")
    private DeviceGroup deviceGroup;

    /**
     * Creates a new <code>DefaultPolicyMapper</code>. This maps the default
     * policy <code>policy</code> to the <code>Appraiser</code>.
     *
     * @param appraiser appraiser
     * @param policy default policy
     */
    public PolicyMapper(final Appraiser appraiser, final Policy policy) {
        if (appraiser == null) {
            LOGGER.error("creating default policy mapper with null appraiser");
            throw new NullPointerException("appraiser");
        }
        this.appraiser = appraiser;
        setPolicy(policy);
    }

    /**
     * Creates a new <code>PolicyMapper</code>. This maps the policy
     * <code>policy</code> to the <code>Appraiser</code> and
     * <code>DeviceGroup</code>.
     *
     * @param appraiser
     *            appraiser
     * @param policy
     *            policy
     * @param deviceGroup
     *            deviceGroup
     */
    public PolicyMapper(final Appraiser appraiser, final Policy policy,
            final DeviceGroup deviceGroup) {
        if (appraiser == null) {
            LOGGER.error("creating default policy mapper with null appraiser");
            throw new NullPointerException("appraiser");
        }
        this.appraiser = appraiser;
        setPolicy(policy);
        this.deviceGroup = deviceGroup;
    }

    /**
     * Default constructor needed by Hibernate.
     */
    protected PolicyMapper() {
        this.appraiser = null;
        this.policy = null;
    }

    /**
     * Returns the <code>Appraiser</code>.
     *
     * @return appraiser
     */
    public Appraiser getAppraiser() {
        return appraiser;
    }

    /**
     * Returns the policy associated with the appraiser.
     *
     * @return policy
     */
    public Policy getPolicy() {
        return policy;
    }

    /**
     * Sets the policy associated with the appraiser.
     *
     * @param policy policy
     */
    public void setPolicy(final Policy policy) {
        if (policy == null) {
            LOGGER.error("creating default policy mapper with null policy");
            throw new NullPointerException("policy");
        }
        this.policy = policy;
    }

    /**
     * Sets the device group associated with this mapping.
     *
     * @param deviceGroup
     *            device group
     */
    public void setDeviceGroup(final DeviceGroup deviceGroup) {
        this.deviceGroup = deviceGroup;
    }

    /**
     * Retrieves the device group associated with this mapping.
     *
     * @return deviceGroup
     */
    public DeviceGroup getDeviceGroup() {
        return this.deviceGroup;
    }

}
