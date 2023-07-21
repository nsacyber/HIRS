package hirs.structs.converters;

import hirs.structs.elements.Struct;

/**
 * Interface defining how to convert {@code byte[]} into {@link Struct} and vice versa.
 */
public interface StructConverter {

    /**
     * Convert a {@link Struct} bean to it's serializable form.
     *
     * @param struct to be serialized
     * @return serialized form of the struct
     */
    byte[] convert(Struct struct);

    /**
     * Convert the given data into the specified struct type.
     *
     * @param data to be parsed
     * @param type type of data being parsed
     * @param <T> the {@link Struct} type
     * @return de-serialized struct
     */
    <T extends Struct> T convert(byte[] data, Class<T> type);
}
