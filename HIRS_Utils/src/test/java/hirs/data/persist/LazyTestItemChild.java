package hirs.data.persist;

import javax.persistence.Column;
import javax.persistence.Entity;
import java.util.Collections;
import java.util.HashSet;

/**
 * A simple class used in database persistence testing.
 */
@Entity
public class LazyTestItemChild extends LazyTestItemParent {
    @Column
    private String name;

    @Column(nullable = false)
    private String requiredField;

    /**
     * Protected default constructor for Hibernate.
     */
    protected LazyTestItemChild() {
        super();
    }

    /**
     * Construct a new instance of this class.
     * @param name the name to give to this object
     */
    public LazyTestItemChild(final String name) {
        super(new HashSet<>(
                Collections.singletonList(new LazyTestCollectionItem("Item"))
        ));
        this.name = name;
        this.requiredField = name;
    }

    /**
     * Gets the name associated with this object.
     *
     * @return the object's name
     */
    public final String getName() {
        return name;
    }

    /**
     * Sets the required field to the value.
     * @param requiredField the new value for the field
     */
    public void setRequiredField(final String requiredField) {
        this.requiredField = requiredField;
    }

    /**
     * Gets the required field.
     * @return the value of the field
     */
    public String getRequiredField() {
        return this.requiredField;
    }
}
