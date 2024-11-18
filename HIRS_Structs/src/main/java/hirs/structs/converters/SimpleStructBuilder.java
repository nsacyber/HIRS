package hirs.structs.converters;

import hirs.structs.elements.Struct;
import hirs.structs.elements.StructElementLength;
import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.apache.commons.lang3.reflect.FieldUtils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

/**
 * StructBuilder implementation.
 *
 * @param <T> the type of Struct to build
 */
public class SimpleStructBuilder<T extends Struct> implements StructBuilder {
    private final Class<T> clazz;
    private T struct;

    /**
     * Instantiates the builder.
     *
     * @param clazz The type of struct to build
     */
    public SimpleStructBuilder(final Class<T> clazz) {
        this.clazz = clazz;
        resetStruct();
    }

    private void resetStruct() {
        try {
            struct = ConstructorUtils.invokeConstructor(clazz);
        } catch (InstantiationException | IllegalAccessException
                 | NoSuchMethodException | InvocationTargetException e) {
            throw new StructBuilderException(
                    String.format("Unexpected error constructing new instance: %s",
                            clazz.getSimpleName(), e.getMessage()), e);
        }
    }

    /**
     * Set the specified field to the specified value.
     *
     * @param field to be set
     * @param value to initialize the field
     * @return this builder
     */
    @Override
    public SimpleStructBuilder<T> set(final String field, final String value) {
        return setField(field, value);
    }

    /**
     * Set the specified field to the specified value.
     *
     * @param field to be set
     * @param value to initialize the field
     * @return this builder
     */
    @Override
    public SimpleStructBuilder<T> set(final String field, final Number value) {
        try {
            String type = clazz.getDeclaredField(field).getType().getSimpleName();
            return switch (clazz.getDeclaredField(field).getType().getSimpleName()) {
                case "short" -> setField(field, value.shortValue());
                case "int" -> setField(field, value.intValue());
                case "byte" -> setField(field, value.byteValue());
                default -> throw new StructBuilderException(
                        String.format("Unhandled numeric field type: %s", type));
            };
        } catch (NoSuchFieldException | SecurityException e) {
            throw new StructBuilderException(
                    String.format("Unexpected error setting field: %s",
                            field, e.getMessage()), e);
        }
    }

    /**
     * Set the specified field to the specified value.
     *
     * @param field to be set
     * @param value to initialize the field
     * @return this builder
     */
    @Override
    public SimpleStructBuilder<T> set(final String field, final byte[] value) {
        setLength(field, value.length);
        return setField(field, value);
    }

    private void setLength(final String fieldName, final Number len) {
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(StructElementLength.class)) {
                StructElementLength length = field.getAnnotation(StructElementLength.class);
                if (length.fieldName().equals(fieldName)) {
                    set(field.getName(), len);
                    return;
                }
            }
        }
    }

    /**
     * Set the specified field to the specified value.
     *
     * @param field to be set
     * @param value to initialize the field
     * @return this builder
     */
    @Override
    public SimpleStructBuilder<T> set(final String field, final Struct value) {
        setLength(field, new SimpleStructConverter().convert(value).length);
        return setField(field, value);
    }

    private SimpleStructBuilder<T> setField(final String fieldName, final Object value) {
        try {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            FieldUtils.writeField(field, struct, value);
            field.setAccessible(false);
        } catch (NoSuchFieldException | SecurityException
                 | IllegalArgumentException | IllegalAccessException e) {
            throw new StructBuilderException(
                    String.format("Unexpected error setting field: %s",
                            fieldName, e.getMessage()), e);
        }
        return this;
    }

    /**
     * Return the built Struct and internally resets the builder.
     *
     * @return the Struct
     */
    @Override
    public T build() {
        T retval = struct;
        resetStruct();
        return retval;
    }
}
