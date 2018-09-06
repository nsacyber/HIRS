package hirs.utils;

import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

/**
 * This class contains utility methods to safely cast collections of objects to
 * collections containing more specific types.
 */
public final class CollectionHelper {
    /**
     * This is a utility class that should not be constructed.
     */
    private CollectionHelper() {

    }

    /**
     * Given a Collection of instances of a known/expected certain class but which are assigned to
     * a variable with a less specific type, return a new Collection of the objects that have been
     * checked to meet that specific type.  If the given collection is ordered, the resulting
     * list will maintain the original ordering of the elements.
     *
     * This method will throw an <code>IllegalArgumentException</code> if any one of the elements
     * of the given collection is not of the expected type.
     *
     * Example usage:
     * <code>
     * Collection<Object> strings = Arrays.asList("one", "two", "three");
     * Collection<String> casted = CollectionHelper.getHashSetOf(String.class, strings);
     * </code>
     *
     * @param clazz the type of objects that the given collection contains, and that the returned
     *              collection will contain
     * @param collection the existing collection of objects
     * @param <T> the type of objects (specified by the clazz parameter)
     * @return the resulting collection of objects
     */
    public static <T> Collection<T> getCollectionOf(
            final Class<T> clazz,
            final Collection<?> collection
    ) {
        return getCollectionOf(clazz, collection, new ArrayList<>());
    }

    /**
     * Given a Collection of instances of a known/expected certain class but which are assigned to
     * a variable with a less specific type, return a new ArrayList of the objects that have been
     * checked to meet that specific type.  If the given collection is ordered, the resulting
     * list will maintain the original ordering of the elements.
     *
     * This method will throw an <code>IllegalArgumentException</code> if any one of the elements
     * of the given collection is not of the expected type.
     *
     * Example usage:
     * <code>
     * Collection<Object> strings = Arrays.asList("one", "two", "three");
     * List<String> casted = CollectionHelper.getArrayListOf(String.class, strings);
     * </code>
     *
     * @param clazz the type of objects that the given collection contains, and that the returned
     *              collection will contain
     * @param collection the existing collection of objects
     * @param <T> the type of objects (specified by the clazz parameter)
     * @return the resulting list of objects
     */
    public static <T> ArrayList<T> getArrayListOf(
            final Class<T> clazz,
            final Collection<?> collection
    ) {
        return getCollectionOf(clazz, collection, new ArrayList<>());
    }

    /**
     * Given a Collection of instances of a known/expected certain class but which are assigned to
     * a variable with a less specific type, return a new HashSet of the objects that have been
     * checked to meet that specific type.
     *
     * This method will throw an <code>IllegalArgumentException</code> if any one of the elements
     * of the given collection is not of the expected type.
     *
     * Example usage:
     * <code>
     * Collection<Object> strings = Arrays.asList("one", "two", "three");
     * Set<String> casted = CollectionHelper.getHashSetOf(String.class, strings);
     * </code>
     *
     * @param clazz the type of objects that the given collection contains, and that the returned
     *              collection will contain
     * @param collection the existing collection of objects
     * @param <T> the type of objects (specified by the clazz parameter)
     * @return the resulting set of objects
     */
    public static <T> HashSet<T> getHashSetOf(
            final Class<T> clazz,
            final Collection<?> collection
    ) {
        return getCollectionOf(clazz, collection, new HashSet<>());
    }

    /**
     * Given a Collection of instances of a known/expected certain class but which are assigned to
     * a variable with a less specific type, return a populated Collection of the objects that have
     * been checked to meet that specific type.  This method can be used to safely obtain
     * a Collection containing the same objects, but which have been assigned a more specific type.
     * If both the original collection and result collections are types of Collection that
     * preserve the ordering of elements, the objects will maintain their order in the result
     * collection.
     *
     * The returned object will be the same object that is given in the
     * <code>resultCollection</code> parameter, but modified such that it contains the entire
     * given contents of the given <code>collection</code>.  <code>resultCollection</code> must be
     * empty, or an IllegalArgumentException will be thrown.
     *
     * This method will throw an <code>IllegalArgumentException</code> if any one of the elements
     * in the given collection is not of the expected type.
     *
     * Example usage:
     * <code>
     * Collection<Object> strings = Arrays.asList("one", "two", "three");
     * TreeSet<String> casted =
     *         CollectionHelper.getCollectionOf(String.class, strings, new TreeSet<>());
     * </code>
     *
     * @param clazz the type of objects that the given collection contains, and that the returned
     *              collection will contain
     * @param collection the existing collection of objects
     * @param resultCollection an empty collection that will be populated with the items from
     *                         <code>collection</code>
     * @param <T> the type of objects (specified by the clazz parameter)
     * @param <U> the type of the result Collection (any Collection that contains objects of type T)
     * @return the resulting set of objects
     */
    public static <T, U extends Collection<T>> U getCollectionOf(
            final Class<T> clazz,
            final Collection<?> collection,
            final U resultCollection
    ) {
        if (!CollectionUtils.isEmpty(resultCollection)) {
            throw new IllegalArgumentException("The given collection should be empty.");
        }
        for (Object obj : collection) {
            if (clazz.isInstance(obj)) {
                resultCollection.add(clazz.cast(obj));
            } else {
                throw new IllegalArgumentException(
                        String.format("Expected type %s, found type %s", clazz, obj.getClass())
                );
            }
        }
        return resultCollection;
    }
}
