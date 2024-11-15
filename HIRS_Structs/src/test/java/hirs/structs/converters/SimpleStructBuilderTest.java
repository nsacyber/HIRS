package hirs.structs.converters;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests suite for {@link SimpleStructConverter}.
 */
public class SimpleStructBuilderTest {
    private static final int NUMBER = 123;
    private static final byte[] ARRAY = new byte[] {4, 5, 6};

    /**
     * Tests {@link SimpleStructBuilder#build()}.
     *
     * @throws NoSuchFieldException     sometimes
     * @throws IllegalAccessException   sometimes
     * @throws IllegalArgumentException sometimes
     */
    @Test
    public final void testBuild() throws NoSuchFieldException, IllegalArgumentException,
            IllegalAccessException {
        TestStruct struct = new SimpleStructBuilder<>(TestStruct.class)
                .set("testShort", NUMBER)
                .set("testByte", NUMBER)
                .set("testEmbeddedStruct", new SimpleStructBuilder<>(TestEmbeddedStruct.class)
                        .set("embeddedShort", NUMBER)
                        .set("embedded", ARRAY)
                        .build())
                .set("testVariableStruct", new SimpleStructBuilder<>(TestVariableStruct.class)
                        .set("testArray", ARRAY)
                        .build())
                .build();

        assertEquals(NUMBER, struct.getTestShort());
        assertEquals(NUMBER, struct.getTestByte());

        assertEquals(NUMBER, struct.getTestEmbeddedStruct().getEmbeddedShort());
        assertArrayEquals(ARRAY, struct.getTestEmbeddedStruct().getEmbedded());
        assertEquals(ARRAY.length, struct.getTestEmbeddedStruct().getEmbeddedSize());

        assertArrayEquals(ARRAY, struct.getTestVariableStruct().getTestArray());
        assertEquals(ARRAY.length, struct.getTestVariableStructLength());
    }

}
