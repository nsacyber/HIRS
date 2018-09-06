package hirs.structs.converters;

import hirs.structs.elements.Struct;

/**
 * Interface defining how to build Struct objects with a fluent interface.
 *
 * @param <T> the type of Struct to build
 */
public interface StructBuilder<T extends Struct> {
    /**
     * Set the specified field to the specified value.
     *
     * @param field to be set
     * @param value to initialize the field
     * @return this builder
     */
    StructBuilder<T> set(String field, String value);

    /**
     * Set the specified field to the specified value.
     *
     * @param field to be set
     * @param value to initialize the field
     * @return this builder
     */
    StructBuilder<T> set(String field, Number value);

    /**
     * Set the specified field to the specified value.
     *
     * @param field to be set
     * @param value to initialize the field
     * @return this builder
     */
    StructBuilder<T> set(String field, byte[] value);

    /**
     * Set the specified field to the specified value.
     *
     * @param field to be set
     * @param value to initialize the field
     * @return this builder
     */
    StructBuilder<T> set(String field, Struct value);

    /**
     * Return the built Struct.
     *
     * @return the Struct
     */
    T build();
}
