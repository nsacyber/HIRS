package hirs.attestationca.portal.persist.entity;


import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * An abstract archivable entity that can be given a user-defined name and description.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
@MappedSuperclass
public abstract class UserDefinedEntity extends ArchivableEntity {

    @Column(nullable = false, unique = true)
    private String name;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
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
}

