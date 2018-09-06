package hirs.data.persist;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * An abstract archivable entity that can be given a user-defined name and description.
 */
@MappedSuperclass
public abstract class UserDefinedEntity extends ArchivableEntity {

    private static final Logger LOGGER = LogManager.getLogger(UserDefinedEntity.class);

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false, unique = false)
    private String description = "";

    /**
     * Default empty constructor is required for Hibernate. It is protected to
     * prevent code from calling it directly.
     */
    protected UserDefinedEntity() {
        super();
    }

    /**
     * Creates a new entity with the specified name.
     *
     * @param name name
     */
    public UserDefinedEntity(final String name) {
        this(name, "");
    }

    /**
     * Creates a new <code>Policy</code> with the specified name and description.
     *
     * @param name
     *          name (required)
     * @param description
     *          description (may be null)
     */
    public UserDefinedEntity(final String name, final String description) {
        setName(name);
        setDescription(description);
    }

    /**
     * Returns the name of this entity. Names are unique and used by users to reference them.
     *
     * @return name
     */
    public final String getName() {
        return name;
    }

    /**
     * Sets the name of this entity. This name must be unique and
     * will be used by users to retrieve and identify it.
     * .
     *
     * @param name name
     */
    public final void setName(final String name) {
        if (name == null) {
            LOGGER.debug("null name in setter");
            throw new NullPointerException("name");
        }
        LOGGER.debug("setting name: {}", name);
        this.name = name;
    }

    /**
     * Returns the description of this entity. Descriptions are not unique and
     * are specifically used to provide additional details.
     *
     * @return description description
     */
    public final String getDescription() {
        return description;
    }

    /**
     * Sets the description of this entity.  The description is not
     * required and does not need to be unique.  It is simply used to provide the
     * user with more information about this entity.
     *
     * @param description description
     */
    public final void setDescription(final String description) {
        if (description == null) {
            LOGGER.debug("null description in setter");
            throw new NullPointerException("description");
        }
        LOGGER.debug("Setting description: {}", description);
        this.description = description;
    }

    /**
     * Returns a boolean if other is equal to this. <code>UserDefinedEntity</code>s are
     * identified by their name, so this returns true if <code>other</code> is
     * an instance of <code>UserDefinedEntity</code> and its name is the same as this
     * <code>UserDefinedEntity</code>. Otherwise this returns false.
     *
     * @param other
     *            other object to test for equals
     * @return true if other is <code>Baseline</code> and has same name
     */
    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof UserDefinedEntity)) {
            return false;
        }

        final UserDefinedEntity entity = (UserDefinedEntity) other;
        return this.getName().equals(entity.getName());
    }

    /**
     * Returns the hash code for this <code>UserDefinedEntity</code>.
     * <code>UserDefinedEntity</code>s are identified by their name, so the returned hash
     * is the hash of the name.
     *
     * @return hash
     */
    @Override
    public int hashCode() {
        return name.hashCode();
    }

    /**
     * Returns a <code>String</code> representation of this class. This returns
     * the <code>UserDefinedEntity</code> name.
     *
     * @return <code>UserDefinedEntity</code> name
     */
    @Override
    public String toString() {
        return this.name;
    }
}
