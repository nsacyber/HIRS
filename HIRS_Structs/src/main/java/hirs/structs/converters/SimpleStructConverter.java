package hirs.structs.converters;

import hirs.structs.elements.Struct;
import hirs.structs.elements.StructElementLength;
import hirs.structs.elements.StructElements;
import org.apache.commons.lang3.reflect.FieldUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * Basic implementation of a {@link StructConverter}. Uses the {@link StructElements} to determine
 * the fields to be processed and in which order.
 */
public class SimpleStructConverter implements StructConverter {

    @Override
    public final byte[] convert(final Struct struct) {

        // using output stream resources, serialize the specified struct
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream)) {

            // obtain the struct elements definition
            StructElements structElements = struct.getClass().getAnnotation(StructElements.class);

            // ensure that the class is properly documented
            if (structElements == null) {
                throw new StructConversionException(String.format(
                        "%s does not have the proper @StructElements definition on the class "
                                + "type. Please see documentation on Struct interface",
                        struct.getClass().getSimpleName()));
            }

            // iterate over the defined elements
            for (String fieldName : structElements.elements()) {

                // grab the field
                Field field = struct.getClass().getDeclaredField(fieldName);

                // ensure the field is accessible
                field.setAccessible(true);

                // obtain the value from the field
                final Object value = FieldUtils.readField(field, struct);

                // process the field according to its type
                if (Struct.class.isAssignableFrom(field.getType())) {
                    if (value == null) {
                        continue;
                    }
                    dataOutputStream.write(convert((Struct) value));
                } else if (int.class.isAssignableFrom(field.getType())) {
                    dataOutputStream.writeInt((int) value);
                } else if (short.class.isAssignableFrom(field.getType())) {
                    dataOutputStream.writeShort((short) value);
                } else if (byte[].class.isAssignableFrom(field.getType())) {
                    dataOutputStream.write((byte[]) value);
                } else if (byte.class.isAssignableFrom(field.getType())) {
                    dataOutputStream.writeByte((byte) value);
                } else {
                    throw new StructConversionException(
                            "Unsupported field type for element or length: " + field.getType());
                }

                // re-enforce the access checking
                field.setAccessible(false);
            }

            // flush out the stream
            dataOutputStream.flush();

            // return the written byte array
            return byteArrayOutputStream.toByteArray();

        } catch (IOException | IllegalAccessException | NoSuchFieldException e) {
            throw new StructConversionException(
                    String.format("Unexpected error converting %s into a struct: %s",
                            struct.getClass().getSimpleName(), e.getMessage()), e);
        }

    }

    @Override
    public final <T extends Struct> T convert(final byte[] data, final Class<T> type) {
        return type.cast(convert(new ByteArrayInputStream(data), type));
    }

    /**
     * Convert the data in the ByteArrayInputStream into the specified Struct.
     *
     * @param bis   to read data from for conversion
     * @param clazz type of struct
     * @return converted Struct
     */
    private Struct convert(final ByteArrayInputStream bis, final Class<? extends Struct> clazz) {
        return convert(new DataInputStream(bis), clazz);
    }

    /**
     * Convert the data in the DataInputStream into the specified Struct.
     *
     * @param inputStream to read data from for conversion
     * @param clazz       type of struct
     * @return converted Struct
     */
    @SuppressWarnings("unchecked")
    private Struct convert(final DataInputStream inputStream, final Class<? extends Struct> clazz) {

        String currentFieldName = "";
        try {

            // map field names to a @StructElementLength
            Map<String, Integer> fieldLengths = new HashMap<>();

            StructBuilder builder = new SimpleStructBuilder(clazz);

            // obtain the struct elements definition
            StructElements structElements = clazz.getAnnotation(StructElements.class);

            // ensure that the class is properly documented
            if (structElements == null) {
                throw new StructConversionException(String.format(
                        "%s does not have the proper @StructElements definition on the class "
                                + "type. Please see documentation on Struct interface",
                        clazz.getSimpleName()));
            }


            // iterate over the defined elements
            for (String fieldName : structElements.elements()) {

                currentFieldName = fieldName;
                // grab the field
                Field field = clazz.getDeclaredField(fieldName);

                // determine if this is a length field
                boolean isLengthField = field.isAnnotationPresent(StructElementLength.class);

                Number numericValue = 0;

                if (byte.class.isAssignableFrom(field.getType())) {
                    numericValue = inputStream.readByte();
                    builder.set(fieldName, numericValue);
                } else if (short.class.isAssignableFrom(field.getType())) {
                    numericValue = inputStream.readUnsignedShort();
                    builder.set(fieldName, numericValue);
                } else if (int.class.isAssignableFrom(field.getType())) {
                    numericValue = inputStream.readInt();
                    builder.set(fieldName, numericValue);
                } else if (byte[].class.isAssignableFrom(field.getType())) {
                    byte[] value = new byte[fieldLengths.get(field.getName())];
                    inputStream.read((byte[]) value);
                    builder.set(fieldName, value);
                } else if (Struct.class.isAssignableFrom(field.getType())) {
                    // some struct fields are variable. if there is a length and it is 0, move on
                    if (fieldLengths.containsKey(field.getName())
                            && fieldLengths.get(field.getName()) == 0) {
                        continue;
                    }
                    builder.set(fieldName,
                            convert(inputStream, (Class<? extends Struct>) field.getType()));
                } else {
                    throw new StructConversionException(
                            "Unsupported field type for element or length: " + field.getType());
                }

                // check if this field has either a length or element annotation
                if (isLengthField) {
                    StructElementLength length = field.getAnnotation(StructElementLength.class);
                    fieldLengths.put(length.fieldName(), numericValue.intValue());
                }
            }

            return builder.build();
        } catch (NoSuchFieldException | IOException | NullPointerException e) {
            throw new StructConversionException(
                    "Unexpected error processing struct for field: "
                            + currentFieldName + ": " + e.getMessage(), e);
        }
    }

}
