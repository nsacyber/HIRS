package hirs.attestationca.persist.entity;

import hirs.utils.ArchivableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.NonNull;

/**
 * An abstract archivable entity that can be given a user-defined name and description.
 */
@Getter
@Setter
@AllArgsConstructor
@MappedSuperclass
public abstract class UserDefinedEntity extends ArchivableEntity {

    @Column(nullable = false, unique = true)
    @NonNull private String name;

    @ToString.Exclude
    @Column(nullable = false, unique = false)
    @NonNull private String description = "";

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
}

