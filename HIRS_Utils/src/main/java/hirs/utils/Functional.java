package hirs.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Simple utility class to house some functional methods.
 */
public final class Functional {

    /**
     * Prevents construction of an instance of this class.
     */
    private Functional() {

    }

    /**
     * This method implements the standard functional select function to filter a given collection
     * using the given filter.  The filter is called on every item in the collection, and if the
     * filter returns True, the item will be contained in the returned list (otherwise, it will
     * not be.)  The returned list preserves the original order of the given collection if it is an
     * ordered collection.
     *
     * @param items a collection of items to filter
     * @param filter the filter to apply
     * @param <T> the type of object contained in the input and output lists
     * @return the list of items that pass the filter's test
     */
    public static <T> List<T> select(final Collection<T> items, final Callback<T, Boolean> filter) {
        List<T> selectedItems = new ArrayList<>();

        for (T item : items) {
            if (filter.call(item)) {
                selectedItems.add(item);
            }
        }

        return selectedItems;
    }
}
