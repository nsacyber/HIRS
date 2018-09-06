package hirs.data.persist;

import javax.persistence.Entity;

/**
 * This <code>Policy</code> class is used exclusively for testing purposes.
 */
@Entity
public class TestPolicy2 extends Policy {

    /**
     * Creates a new <code>TestPolicy</code> with the set name.
     *
     * @param name name
     */
    public TestPolicy2(final String name) {
        super(name);
    }

    /**
     * Default constructor necesessary for Hibernate.
     */
    protected TestPolicy2() {
        super();
    }
}
