package hirs.data.persist.policy;

import hirs.data.persist.UserDefinedEntity;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;

/**
 * The <code>Policy</code> class represents a policy. This is an abstract class
 * for representing the rules for which an <code>Appraiser</code> should
 * evaluate a <code>Report</code>. A typical <code>Policy</code> will contain a
 * <code>Baseline</code> at the very least. A <code>Policy</code> is identified
 * by its name, so the name for a <code>Policy</code> must be unique.
 */
@Entity
@Table(name = "Policy")
@Inheritance(strategy = InheritanceType.JOINED)
@Access(AccessType.FIELD)
public abstract class Policy extends UserDefinedEntity {

    /**
     * Creates a new <code>Policy</code> with the specified name.
     *
     * @param name
     *            name
     */
    public Policy(final String name) {
        super(name);
    }

    /**
     * Creates a new <code>Policy</code> with the specified name and
     * description.
     *
     * @param name
     *          name (required)
     * @param description
     *          description (may be null)
     */
    public Policy(final String name, final String description) {
        super(name, description);
    }

    /**
     * Default empty constructor is required for Hibernate. It is protected to
     * prevent code from calling it directly.
     */
    protected Policy() {
        super();
    }

    /**
     * Returns true if this object has been persisted.  Used in determining whether
     * an Appraiser should request the full Policy (and baselines) for appraisal
     *
     * @return true if this object has been persisted; false otherwise
     */
    public final boolean isPersisted() {
        return getId() != null;
    }

    /**
     * When {@link Policy} are serialized to be sent to the browser, this can be used
     * to determine the type of {@link Policy}.
     *
     * @return The class name for the {@link Policy}
     */
    public String getType() {
        return this.getClass().getSimpleName();
    }
}
