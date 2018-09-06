package hirs.structs.converters;

import static org.testng.Assert.assertEquals;
import org.testng.annotations.Test;

/**
 * Tests suite for {@link SimpleStructConverter}.
 */
public class SimpleStructBuilderTest {
    private static final int NUMBER = 123;
    private static final byte[] ARRAY = new byte[] {4, 5, 6};

    /**
     * Tests {@link SimpleStructBuilder#build()}.
     * @throws java.lang.NoSuchFieldException sometimes
     * @throws java.lang.IllegalAccessException sometimes
     * @throws java.lang.IllegalArgumentException sometimes
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

        assertEquals(struct.getTestShort(), NUMBER);
        assertEquals(struct.getTestByte(), NUMBER);

        assertEquals(struct.getTestEmbeddedStruct().getEmbeddedShort(), NUMBER);
        assertEquals(struct.getTestEmbeddedStruct().getEmbedded(), ARRAY);
        assertEquals(struct.getTestEmbeddedStruct().getEmbeddedSize(), ARRAY.length);

        assertEquals(struct.getTestVariableStruct().getTestArray(), ARRAY);
        assertEquals(struct.getTestVariableStructLength(), ARRAY.length);
    }

}
