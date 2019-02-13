package hirs.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.bouncycastle.asn1.x500.X500Name;

/**
 * Simple utility class to house some functional methods.
 */
public final class Functional {

    private static final String SEPARATOR_COMMA = ",";
    private static final String SEPARATOR_PLUS = "+";

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

    /**
     * This method can be used to compare the distinguished names given from
     * certificates.  This compare uses X500Name class in bouncy castle, which
     * compares the RDNs and not the string itself.  The method will check
     * for '+' and replace them, X500Name doesn't do this.
     *
     * @param nameValue1 first general name to be used
     * @param nameValue2 second general name to be used
     * @return true if the values match based on the RDNs, false if not
     */
    public static boolean x500NameCompare(final String nameValue1, final String nameValue2) {
        if (nameValue1 == null || nameValue2 == null) {
            throw new IllegalArgumentException("Provided DN string is null.");
        }

        X500Name x500Name1 = new X500Name(nameValue1.replace(SEPARATOR_PLUS, SEPARATOR_COMMA));
        X500Name x500Name2 = new X500Name(nameValue2.replace(SEPARATOR_PLUS, SEPARATOR_COMMA));

        return x500Name1.equals(x500Name2);
    }
}
