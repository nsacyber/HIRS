package hirs.attestationca.persist.entity;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for the class <code>Appraiser</code>.
 */
public final class AppraiserTest {

    /**
     * Tests that an <code>Appraiser</code> can be created with a valid name.
     */
    @Test
    public void testAppraiser() {
        final String name = "Test Appraiser";
        new TestAppraiser(name);
    }

    /**
     * Tests that the name is returned from <code>getName</code>.
     */
    @Test
    public void testGetName() {
        final String name = "Test Appraiser";
        final Appraiser appraiser = new TestAppraiser(name);
        assertEquals(appraiser.getName(), name);
    }

    /**
     * Tests that the name property can be set.
     */
    @Test
    public void testSetName() {
        final String originalName = "Test Appraiser";
        final Appraiser appraiser = new TestAppraiser(originalName);
        assertEquals(appraiser.getName(), originalName);
        final String newName = "Awesome Test Appraiser";
        appraiser.setName(newName);
        assertEquals(appraiser.getName(), newName);
    }

    /**
     * Tests that x.equals(null) returns false.
     */
    @Test
    public void testEqualsNull() {
        final String name = "Test Appraiser";
        final Appraiser appraiser = new TestAppraiser(name);
        assertNotEquals(null, appraiser);
    }

    /**
     * Tests that x.equals(x) for an appraiser.
     */
    @Test
    public void testEqualsReflexive() {
        final String name = "Test Appraiser";
        final Appraiser appraiser = new TestAppraiser(name);
        assertTrue(appraiser.equals(appraiser));
    }

    /**
     * Tests that x.equals(y) and y.equals(x) for an appraiser.
     */
    @Test
    public void testEqualsSymmetric() {
        final String name = "Test Appraiser";
        final Appraiser appraiser1 = new TestAppraiser(name);
        final Appraiser appraiser2 = new TestAppraiser(name);
        assertEquals(appraiser1, appraiser2);
        assertEquals(appraiser2, appraiser1);
    }

    /**
     * Tests that x.equals(y) and y.equals(z) then x.equals(z) for an appraiser.
     */
    @Test
    public void testEqualsTransitive() {
        final String name = "Test Appraiser";
        final Appraiser appraiser1 = new TestAppraiser(name);
        final Appraiser appraiser2 = new TestAppraiser(name);
        final Appraiser appraiser3 = new TestAppraiser(name);
        assertEquals(appraiser1, appraiser2);
        assertEquals(appraiser2, appraiser3);
        assertEquals(appraiser1, appraiser3);
    }

    /**
     * Tests that two appraisers are not equal if their names are different.
     */
    @Test
    public void testNotEquals() {
        final String name1 = "Test Appraiser";
        final String name2 = "Other Appraiser";
        final Appraiser appraiser1 = new TestAppraiser(name1);
        final Appraiser appraiser2 = new TestAppraiser(name2);
        assertNotEquals(appraiser1, appraiser2);
        assertNotEquals(appraiser2, appraiser1);
    }

    /**
     * Tests that if two appraisers are equal that their hash codes are equal.
     */
    @Test
    public void testHashCodeEquals() {
        final String name = "Test Appraiser";
        final Appraiser appraiser1 = new TestAppraiser(name);
        final Appraiser appraiser2 = new TestAppraiser(name);
        assertEquals(appraiser1, appraiser2);
        assertEquals(appraiser2, appraiser1);
        assertEquals(appraiser1.hashCode(), appraiser2.hashCode());
        assertEquals(appraiser2.hashCode(), appraiser1.hashCode());
    }

    /**
     * Tests that if two appraisers are not equal that their hash codes are not equal.
     */
    @Test
    public void testHashCodeNotEquals() {
        final String name1 = "Test Appraiser";
        final String name2 = "Other Appraiser";
        final Appraiser appraiser1 = new TestAppraiser(name1);
        final Appraiser appraiser2 = new TestAppraiser(name2);
        assertNotEquals(appraiser1, appraiser2);
        assertNotEquals(appraiser2, appraiser1);
        assertNotEquals(appraiser1.hashCode(), appraiser2.hashCode());
        assertNotEquals(appraiser2.hashCode(), appraiser1.hashCode());
    }

}