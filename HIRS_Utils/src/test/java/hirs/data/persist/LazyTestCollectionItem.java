package hirs.data.persist;

import org.hibernate.annotations.Type;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.UUID;

/**
 * A simple class used in database persistence testing.
 */
@Entity
public class LazyTestCollectionItem {
    @Id
    @Column(name = "id")
    @Type(type = "uuid-char")
    private UUID id;

    @Column
    private String name;

    /**
     * Protected default constructor for Hibernate.
     */
    protected LazyTestCollectionItem() {
    }

    /**
     * Construct a new instance of this class.
     * @param name the item's name
     */
    public LazyTestCollectionItem(final String name) {
        this.id = UUID.randomUUID();
        this.name = name;
    }

    /**
     * Get the ID of this item.
     *
     * @return the item's ID
     */
    public final UUID getId() {
        return id;
    }

    /**
     * Get the name of this item.
     *
     * @return the item's name
     */
    public final String getName() {
        return name;
    }
}
