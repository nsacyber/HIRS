package hirs.utils;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Tests methods in the {@link FunctionalTest} utility class.
 */
public class FunctionalTest {
    private static final int ONE = 1;
    private static final int TWO = 2;
    private static final int THREE = 3;

    private static final List<Integer> EMPTY_LIST = Collections.emptyList();
    private static final List<Integer> INPUT_LIST = Arrays.asList(1, 2, 3, 4);

    private static final Callback<Integer, Boolean> ODD_FILTER = new Callback<Integer, Boolean>() {
        @Override
        public Boolean call(final Integer candidate) {
            return Math.abs(candidate) % 2 == 1;
        }
    };

    private static final Callback<Integer, Boolean> TWO_FILTER = new Callback<Integer, Boolean>() {
        @Override
        public Boolean call(final Integer candidate) {
            return candidate == 2;
        }
    };

    private static final Callback<Integer, Boolean> NONE_FILTER = new Callback<Integer, Boolean>() {
        @Override
        public Boolean call(final Integer candidate) {
            return false;
        }
    };

    /**
     * Tests that the select method returns an empty list if an empty list was given.
     */
    @Test
    public void testEmptyCollection() {
        Assert.assertEquals(EMPTY_LIST, Functional.select(EMPTY_LIST, ODD_FILTER));
    }

    /**
     * Tests that the select method returns an the correct elements from a nonempty list.
     */
    @Test
    public void testSelectMany() {
        List<Integer> results = Functional.select(INPUT_LIST, ODD_FILTER);
        Assert.assertEquals(TWO, results.size());
        Assert.assertTrue(results.contains(ONE));
        Assert.assertTrue(results.contains(THREE));
    }

    /**
     * Tests that the select method returns the correct element from a nonempty list.
     */
    @Test
    public void testSelectSingleElement() {
        List<Integer> results = Functional.select(INPUT_LIST, TWO_FILTER);
        Assert.assertEquals(ONE, results.size());
        Assert.assertTrue(results.contains(TWO));
    }

    /**
     * Tests that the select method returns an empty list if no elements were selected.
     */
    @Test
    public void testSelectNone() {
        Assert.assertEquals(EMPTY_LIST, Functional.select(INPUT_LIST, NONE_FILTER));
    }
}
