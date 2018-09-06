package hirs.structs.converters;

import hirs.structs.elements.Struct;
import hirs.structs.elements.StructElements;

/**
 * Test struct that has a byte array that could be variable in length. Typically embedded in another
 * struct that has a length.
 */
@StructElements(elements = "testArray")
public class TestVariableStruct implements Struct {

    private byte[] testArray = new byte[0];

    /**
     * Getter.
     * @return value
     */
    public byte[] getTestArray() {
        return testArray;
    }
}
