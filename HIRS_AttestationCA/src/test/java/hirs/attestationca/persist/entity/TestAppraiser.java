package hirs.attestationca.persist.entity;

import jakarta.persistence.Entity;

/**
 * Test class for the <code>Appraiser</code> abstract base class.
 */
@Entity
public class TestAppraiser extends Appraiser {

    /**
     * Creates a new <code>TestAppraiser</code>.
     *
     * @param name name
     */
    public TestAppraiser(final String name) {
        super(name);
    }

    /**
     * Default constructor necessary for Hibernate.
     */
    protected TestAppraiser() {
        /* do nothing */
    }
}
