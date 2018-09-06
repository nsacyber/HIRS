package hirs.data.persist;

import org.hibernate.annotations.Type;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import java.util.Set;
import java.util.UUID;

/**
 * A simple abstract class used in database persistence testing.
 */
@Entity
public abstract class LazyTestItemParent {
    @Id
    @Column(name = "id")
    @Type(type = "uuid-char")
    private UUID id;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<LazyTestCollectionItem> items;

    /**
     * Protected default constructor for Hibernate.
     */
    protected LazyTestItemParent() {
    }

    /**
     * Construct a new instance of this object.
     *
     * @param parentItems a Set of items to associate with this instance
     */
    public LazyTestItemParent(final Set<LazyTestCollectionItem> parentItems) {
        this.id = UUID.randomUUID();
        this.items = parentItems;
    }

    /**
     * Get the ID of this object.
     *
     * @return this object's ID
     */
    public final UUID getId() {
        return id;
    }

    /**
     * Get the Set of {@link LazyTestCollectionItem}s associated with this object.
     *
     * @return the Set of {@link LazyTestCollectionItem}s associated with this object
     */
    public final Set<LazyTestCollectionItem> getItems() {
        return items;
    }
}
