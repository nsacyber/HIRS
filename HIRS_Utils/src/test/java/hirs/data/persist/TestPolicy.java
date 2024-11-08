package hirs.data.persist;

import hirs.attestationca.persist.entity.Policy;
import jakarta.persistence.Entity;

/**
 * This <code>Policy</code> class is used exclusively for testing purposes.
 */
@Entity
public class TestPolicy extends Policy {

    /**
     * Creates a new <code>TestPolicy</code> with the set name.
     *
     * @param name name
     */
    public TestPolicy(final String name) {
        super(name);
    }

    /**
     * Creates a new <code>TestPolicy</code> with the set name and description.
     *
     * @param name        name
     * @param description description
     */
    public TestPolicy(final String name, final String description) {
        super(name, description);
    }

    /**
     * Default constructor necessary for Hibernate.
     */
    protected TestPolicy() {
        super();
    }
}
