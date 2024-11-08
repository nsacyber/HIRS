package hirs.structs.converters;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests suite for {@link SimpleStructConverter}.
 */
public class SimpleStructConverterTest {

    private static final byte[] EXPECTED_BYTES =
            new byte[] {0, 5, 0, 0, 0, 10, 0, 7, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 6, 0, 0, 0, 0};

    private final TestStruct testStruct = new TestStruct();

    private final StructConverter converter = new SimpleStructConverter();

    /**
     * Tests {@link SimpleStructConverter#convert(hirs.structs.elements.Struct)}.
     */
    @Test
    public final void testConvertToByteArray() {
        // convert the test struct into a serialized version.
        byte[] serializedStruct = converter.convert(testStruct);

        // assert that the returned contents are expected
        assertArrayEquals(EXPECTED_BYTES, serializedStruct);
    }

    /**
     * Tests {@link SimpleStructConverter#convert(byte[], Class)}.
     */
    @Test
    public final void testConvertToStruct() {
        // resulting struct
        TestStruct struct = converter.convert(EXPECTED_BYTES, TestStruct.class);

        // assert the resulting struct is the same as the original test struct
        assert (struct.equals(testStruct));
    }

    /**
     * Tests {@link SimpleStructConverter#convert(hirs.structs.elements.Struct)} where the
     * Struct does not have the required {@link hirs.structs.elements.StructElements}
     * annotation.
     */
    @Test
    public final void testNoElementsStructConvertToArray() {
        assertThrows(StructConversionException.class, () -> {
            converter.convert(new TestNoElementsAnnotationStruct());
        }, ".*@StructElements.*");
    }

    /**
     * Tests {@link SimpleStructConverter#convert(byte[], Class)}  where the Struct type does not
     * have the required {@link hirs.structs.elements.StructElements} annotation.
     */
    @Test
    public final void testNoElementsStructConvertToStruct() {
        assertThrows(StructConversionException.class, () -> {
            converter.convert(new byte[1], TestNoElementsAnnotationStruct.class);
        }, ".*@StructElements.*");
    }

    /**
     * Tests {@link SimpleStructConverter#convert(hirs.structs.elements.Struct)} where the
     * Struct is {@link TestInvalidDataTypeStruct}. It is expected that a conversion exception will
     * be thrown with a message indicating that a field type is unsupported.
     */
    @Test
    public final void testInvalidDataTypeStructConvertToArray() {
        assertThrows(StructConversionException.class, () -> {
            converter.convert(new TestInvalidDataTypeStruct());
        }, "Unsupported field type.*");
    }

    /**
     * Tests {@link SimpleStructConverter#convert(byte[], Class)}  where the Struct is {@link
     * TestInvalidDataTypeStruct}. It is expected that a conversion exception will be thrown with a
     * message indicating that a field type is unsupported.
     */
    @Test
    public final void testInvalidDataTypeStructConvertToStruct() {
        assertThrows(StructConversionException.class, () -> {
            converter.convert(new byte[1], TestInvalidDataTypeStruct.class);
        }, "Unsupported field type.*");

    }

}
