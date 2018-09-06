package hirs.data.persist;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;

/**
 * A baseline (a.k.a. whitelist) contains a set of expected values for a
 * measurement comparison. In the case of a TPM a baseline for that would
 * contain the PCR IDs along with the expected hash values. This is similar for
 * IMA.
 * <p>
 * The <code>Baseline</code> class represents a baseline. This is an abstract
 * class for referencing <code>Baseline</code>s. A <code>Baseline</code> is
 * identified by its name, so the name for a <code>Baseline</code> must be
 * unique.
 */
@Entity
@Table(name = "Baseline")
@Inheritance(strategy = InheritanceType.JOINED)
@Access(AccessType.FIELD)
public abstract class Baseline extends UserDefinedEntity {

    @Column(nullable = false, name = "severity")
    @Enumerated(EnumType.STRING)
    private Alert.Severity severity = Alert.Severity.UNSPECIFIED;

    @Column(nullable = false)
    private String type;

    /**
     * Creates a new <code>Baseline</code> with the specified name.
     *
     * @param name name
     */
    public Baseline(final String name) {
        super(name);
        type = getClass().getSimpleName();
    }

    /**
     * Default empty constructor is required for Hibernate. It is protected to
     * prevent code from calling it directly.
     */
    protected Baseline() {
        super();
        type = getClass().getSimpleName();
    }

    /**
     * When baselines are serialized to be sent to the browser, this can be used
     * to determine the type of baseline.
     *
     * @return The class name for the baseline
     */
    public String getType() {
        return this.type;
    }

    /**
     * Gets the baseline severity.
     * @return the severity
     */
    public Alert.Severity getSeverity() {
        return severity;
    }

    /**
     * Sets the severity of alerts raised by this baseline.
     * @param severity The desired severity of alerts raised by this baseline
     */
    public void setSeverity(final Alert.Severity severity) {
        this.severity = severity;
    }
}
