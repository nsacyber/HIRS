package hirs.data.persist;

import hirs.attestationca.persist.entity.Policy;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * <code>PolicyTest</code> is a unit test class for the <code>Policy</code>
 * class.
 */
public final class PolicyTest {

    private static final Logger LOGGER = LogManager.getLogger(PolicyTest.class);

    /**
     * Empty constructor that does nothing.
     */
    public PolicyTest() {
        /* do nothing */
    }

    /**
     * Tests <code>Policy</code> constructor with valid name.
     */
    @Test
    public void testPolicy() {
        LOGGER.debug("testPolicy test started");
        final String name = "myPolicy";
        new TestPolicy(name);
    }

    /**
     * Tests that <code>Policy</code> constructor throws
     * <code>NullPointerException</code> with null name.
     */
    @Test
    public void testPolicyNullName() {
        LOGGER.debug("testPolicyNullName test started");
        Assertions.assertThrows(NullPointerException.class, () -> new TestPolicy(null));
    }

    /**
     * Tests that <code>getName()</code> returns the name.
     */
    @Test
    public void testGetName() {
        LOGGER.debug("testGetName test started");
        final String name = "myPolicy";
        Policy p = new TestPolicy(name);
        Assertions.assertEquals(name, p.getName());
    }

    /**
     * Tests that <code>Policy</code> constructor throws
     * <code>NullPointerException</code> with null name.
     */
    @Test
    public void testPolicyNullDescription() {
        LOGGER.debug("testPolicyNullDescription test started");
        final String name = "myPolicy";
        Assertions.assertThrows(NullPointerException.class, () -> new TestPolicy(name, null));
    }

    /**
     * Tests that <code>getDescription()()</code> returns the description.
     */
    @Test
    public void testGetDescription() {
        LOGGER.debug("testGetDescription test started");
        final String name = "myPolicy";
        final String description = "myDescription";
        Policy p = new TestPolicy(name, description);
        Assertions.assertEquals(description, p.getDescription());
    }

    /**
     * Tests that two <code>Policy</code> objects are equal if they have the
     * same name.
     */
    @Test
    public void testEquals() {
        LOGGER.debug("testEquals test started");
        final String name1 = "myPolicy";
        Policy p1 = new TestPolicy(name1);
        final String name2 = "myPolicy";
        Policy p2 = new TestPolicy(name2);
        Assertions.assertEquals(p2, p1);
        Assertions.assertEquals(p2, p1);
        Assertions.assertEquals(p2, p1);
        Assertions.assertEquals(p2, p1);
    }

    /**
     * Tests that two <code>Policy</code> objects are not equal if the names are
     * different.
     */
    @Test
    public void testNotEquals() {
        LOGGER.debug("testNotEquals test started");
        final String name1 = "myPolicy1";
        Policy p1 = new TestPolicy(name1);
        final String name2 = "myPolicy2";
        Policy p2 = new TestPolicy(name2);
        Assertions.assertNotEquals(p2, p1);
        Assertions.assertNotEquals(p2, p1);
    }

    /**
     * Tests that hash code is that of the name.
     */
    @Test
    public void testHashCode() {
        LOGGER.debug("testHashCode test started");
        final String name = "myPolicy";
        Policy p = new TestPolicy(name);
        Assertions.assertEquals(p.hashCode(), name.hashCode());
    }

    /**
     * Tests that the hash code of two <code>Policy</code> objects are the same
     * if the names are the same.
     */
    @Test
    public void testHashCodeEquals() {
        LOGGER.debug("testHashCodeEquals test started");
        final String name1 = "myPolicy";
        Policy p1 = new TestPolicy(name1);
        final String name2 = "myPolicy";
        Policy p2 = new TestPolicy(name2);
        Assertions.assertEquals(p2.hashCode(), p1.hashCode());
    }

    /**
     * Tests that the hash codes of two <code>Policy</code> objects are
     * different if they have different names.
     */
    @Test
    public void testHashCodeNotEquals() {
        LOGGER.debug("testHashCodeNotEquals test started");
        final String name1 = "myPolicy1";
        Policy p1 = new TestPolicy(name1);
        final String name2 = "myPolicy2";
        Policy p2 = new TestPolicy(name2);
        Assertions.assertNotEquals(p2.hashCode(), p1.hashCode());
    }

    /**
     * Tests that the name can be set for a <code>Policy</code>.
     */
    @Test
    public void setName() {
        LOGGER.debug("setName test started");
        final String name = "myPolicy";
        Policy p = new TestPolicy(name);
        final String newName = "newPolicy";
        p.setName(newName);
        Assertions.assertEquals(p.getName(), newName);
        Assertions.assertEquals(p.hashCode(), newName.hashCode());
    }

    /**
     * Tests that the description can be set for a <code>Policy</code>.
     */
    @Test
    public void setDescription() {
        LOGGER.debug("setDescription test started");
        final String name = "myPolicy";
        final String description = "myDescription";
        Policy p = new TestPolicy(name, description);
        final String newDescription = "newDescription";
        p.setDescription(newDescription);
        Assertions.assertEquals(p.getDescription(), newDescription);
    }

    /**
     * Tests that a name cannot be null for a <code>Policy</code>.
     */
    @Test
    public void setNameNull() {
        LOGGER.debug("setNameNull test started");
        final String name = "myPolicy";
        Policy p = new TestPolicy(name);
        Assertions.assertThrows(NullPointerException.class, () -> p.setName(null));
    }

    /**
     * Tests that a description cannot be null for a <code>Policy</code>.
     */
    @Test
    public void setDescriptionNull() {
        LOGGER.debug("setDescriptionNull test started");
        final String name = "myPolicy";
        final String description = "myDescription";
        Policy p = new TestPolicy(name, description);
        Assertions.assertThrows(NullPointerException.class, () -> p.setDescription(null));
    }
}

