package hirs.data.persist;

import hirs.attestationca.persist.entity.Policy;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * <code>PolicyTest</code> is a unit test class for the <code>Policy</code>
 * class.
 */
@Slf4j
public final class PolicyTest {

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
        log.debug("testPolicy test started");
        final String name = "myPolicy";
        new TestPolicy(name);
    }

    /**
     * Tests that <code>Policy</code> constructor throws
     * <code>NullPointerException</code> with null name.
     */
    @Test
    public void testPolicyNullName() {
        log.debug("testPolicyNullName test started");
        Assertions.assertThrows(NullPointerException.class, () -> new TestPolicy(null));
    }

    /**
     * Tests that <code>getName()</code> returns the name.
     */
    @Test
    public void testGetName() {
        log.debug("testGetName test started");
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
        log.debug("testPolicyNullDescription test started");
        final String name = "myPolicy";
        Assertions.assertThrows(NullPointerException.class, () -> new TestPolicy(name, null));
    }

    /**
     * Tests that <code>getDescription()()</code> returns the description.
     */
    @Test
    public void testGetDescription() {
        log.debug("testGetDescription test started");
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
        log.debug("testEquals test started");
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
        log.debug("testNotEquals test started");
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
        log.debug("testHashCode test started");
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
        log.debug("testHashCodeEquals test started");
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
        log.debug("testHashCodeNotEquals test started");
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
        log.debug("setName test started");
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
        log.debug("setDescription test started");
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
        log.debug("setNameNull test started");
        final String name = "myPolicy";
        Policy p = new TestPolicy(name);
        Assertions.assertThrows(NullPointerException.class, () -> p.setName(null));
    }

    /**
     * Tests that a description cannot be null for a <code>Policy</code>.
     */
    @Test
    public void setDescriptionNull() {
        log.debug("setDescriptionNull test started");
        final String name = "myPolicy";
        final String description = "myDescription";
        Policy p = new TestPolicy(name, description);
        Assertions.assertThrows(NullPointerException.class, () -> p.setDescription(null));
    }
}
