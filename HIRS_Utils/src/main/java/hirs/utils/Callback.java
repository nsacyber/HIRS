package hirs.utils;

/**
 * A simple class that allows functionality to be encapsulated and executed in other contexts.
 * See {@link hirs.persist.ImaBaselineRecordManager#iterateOverBaselineRecords} for an
 * example.
 *
 * @param <T> the parameter type of the callback
 * @param <V> the return type of the callback
 */
public abstract class Callback<T, V> {
    /**
     * Call the given code.
     *
     * @param param the parameter that the callback is expecting of type &lt;T&gt;
     * @return a value of type &lt;V&gt;
     */
    public abstract V call(T param);
}
