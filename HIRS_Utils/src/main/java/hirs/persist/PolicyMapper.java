package hirs.persist;

import hirs.appraiser.Appraiser;
import hirs.data.persist.policy.Policy;
import org.apache.logging.log4j.Logger;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import static org.apache.logging.log4j.LogManager.getLogger;

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
}
