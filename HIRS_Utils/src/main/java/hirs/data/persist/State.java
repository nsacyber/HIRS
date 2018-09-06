package hirs.data.persist;

import java.util.Date;
import java.util.UUID;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.annotations.Type;

/**
 * The <code>State</code> class represents a state. This is an abstract class
 * for persisting states of items that interact with HIRS.
 * <p>
 * A device <code>State</code> is identified by its name, so the name for a
 * <code>State</code> must be unique. Using <code>Device</code> name
 * concatenated with the name of the specialized class that extends this class
 * ensures that every instance of this class is unique and associated with a
 * device.
 */
@Entity
@Table(name = "State")
@Inheritance(strategy = InheritanceType.JOINED)
@Access(AccessType.FIELD)
public abstract class State {
    private static final Logger LOGGER = LogManager.getLogger(State.class);

    @Id
    @Column(name = "id")
    @Type(type = "uuid-char")
    private final UUID id;

    @Column (nullable = false)
    private Date createTime;

    /**
     * Creates a new <code>State</code>.
     */
    public State() {
        id = UUID.randomUUID();
        createTime = new Date();
    }

    /**
     * Returns the ID associated with this <code>State</code>. A
     * <code>State</code> is assigned a unique ID it if has been stored in a
     * repository by <code>State</code> persistence manager logic.
     *
     * @return unique ID
     */
    public final UUID getId() {
        return id;
    }

    /**
     * Returns the creation time of this <code>State</code>.
     *
     * @return creation time
     */
    public final Date getCreateDate() {
        if (createTime == null) {
            return new Date(0);
        }
        return new Date(createTime.getTime());
    }

    /**
     * Evaluates the equality of this State against another object.
     *
     * @param o the object against which to evaluate equality
     *
     * @return true if the two objects are deemed equal, false otherwise
     */
    @Override
    public final boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null) {
            return false;
        }

        if (!(o instanceof State)) {
            return false;
        }

        State state = (State) o;
        return id.equals(state.id);
    }

    /**
     * Generates a hashcode for this object, composed of its ID and create time.
     *
     * @return the hashcode
     */
    @Override
    public final int hashCode() {
        return id.hashCode();
    }

    /**
     * Returns a <code>String</code> representation of this class. This returns
     * the <code>State</code> name.
     *
     * @return <code>State</code> name
     */
    @Override
    public String toString() {
        return String.format("State: %s", this.id);
    }
}
