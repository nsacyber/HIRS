package hirs.utils;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Collection of tests for CollectionHelper.
 */
public class CollectionHelperTest {
    private static final Collection<Object> OBJ_COLLECTION_OF_STRINGS =
            Arrays.asList("one", "two", "three");
    private static final int EXPECTED_SIZE = 3;

    /**
     * Tests that a <code>Collection<Object></code> can have its objects safely casted into a
     * <code>Set<String></code>.
     */
    @Test
    public void testGetSet() {
        Set<String> casted = CollectionHelper.getHashSetOf(String.class, OBJ_COLLECTION_OF_STRINGS);
        Assert.assertEquals(casted.size(), EXPECTED_SIZE);
    }

    /**
     * Tests that a <code>Collection<Object></code> can have its objects safely casted into a
     * <code>List<String></code>.
     */
    @Test
    public void testGetList() {
        List<String> casted = CollectionHelper.getArrayListOf(
                String.class, OBJ_COLLECTION_OF_STRINGS
        );
        Assert.assertEquals(casted.size(), EXPECTED_SIZE);
    }

    /**
     * Tests that an empty <code>Collection<Object></code> can be reconstructed as an empty
     * <code>Collection<String></code>.
     */
    @Test
    public void testGetCollectionEmpty() {
        Collection<String> casted = CollectionHelper.getCollectionOf(
                String.class, Collections.EMPTY_LIST
        );
        Assert.assertEquals(casted, Collections.EMPTY_LIST);
    }

    /**
     * Test that a collection of Strings won't be created from a <code>Collection<Object></code>
     * that does not entirely consist of Strings.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testHomogenousCollection() {
        Collection<Object> notAllStrings = Arrays.asList("one", "two", new HashMap<>());
        CollectionHelper.getCollectionOf(String.class, notAllStrings);
    }

    /**
     * Tests that a custom Collection can be provided to
     * <code>CollectionHelper.getCollectionOf</code>, which will safely cast all objects into
     * that given collection.
     */
    @Test
    public void testGetCollectionCustom() {
        TreeSet<String> casted = CollectionHelper.getCollectionOf(
                String.class, OBJ_COLLECTION_OF_STRINGS, new TreeSet<>()
        );
        Assert.assertEquals(casted.size(), EXPECTED_SIZE);
        Assert.assertEquals(casted.getClass(), TreeSet.class);
    }

    /**
     * Tests that a custom non-empty Collection, when provided to
     * <code>CollectionHelper.getCollectionOf</code>, will throw an IllegalArgumentException.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testGetCollectionCustomOnNonemptyResultCollection() {
        CollectionHelper.getCollectionOf(
                String.class, OBJ_COLLECTION_OF_STRINGS, Collections.singletonList("test")
        );
    }
}
