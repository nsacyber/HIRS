package hirs.structs.converters;

import hirs.structs.elements.Struct;
import hirs.structs.elements.StructElements;

/**
 * Test Struct that has an unsupported data type.
 */
@StructElements(elements = {"testLong"})
public class TestInvalidDataTypeStruct implements Struct {

    private static final Long TEST_LONG_VALUE = 1L;

    private Long testLong = TEST_LONG_VALUE;
}
