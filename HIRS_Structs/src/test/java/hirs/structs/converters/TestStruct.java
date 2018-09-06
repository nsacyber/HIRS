package hirs.structs.converters;

import hirs.structs.elements.Struct;
import hirs.structs.elements.StructElementLength;
import hirs.structs.elements.StructElements;

/**
 * A Struct class designed to fully test the design of the converter being tested.
 */
@StructElements(
        elements = { "testShort", "testEmbeddedStruct", "testByte", "testVariableStructLength",
                "testVariableStruct" })
public class TestStruct implements Struct {

    private static final short TEST_SHORT = 0x5;

    private static final byte TEST_BYTE = 0x6;

    private static final int HASH_CODE = 31;

    private short testShort = TEST_SHORT;

    private TestEmbeddedStruct testEmbeddedStruct = new TestEmbeddedStruct();

    private byte testByte = TEST_BYTE;

    @StructElementLength(fieldName = "testVariableStruct")
    private int testVariableStructLength = 0;

    private TestVariableStruct testVariableStruct = new TestVariableStruct();

    @Override
    public final boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TestStruct that = (TestStruct) o;

        return testByte == that.testByte && testShort == that.testShort
                && testEmbeddedStruct.equals(that.testEmbeddedStruct);
    }

    @Override
    public final int hashCode() {
        int result = testShort;
        result = HASH_CODE * result + testEmbeddedStruct.hashCode();
        result = HASH_CODE * result + (int) testByte;
        return result;
    }

    /**
     * Getter.
     * @return value
     */
    public byte getTestByte() {
        return testByte;
    }

    /**
     * Getter.
     * @return value
     */
    public short getTestShort() {
        return testShort;
    }

    /**
     * Getter.
     * @return value
     */
    public TestEmbeddedStruct getTestEmbeddedStruct() {
        return testEmbeddedStruct;
    }

    /**
     * Getter.
     * @return value
     */
    public TestVariableStruct getTestVariableStruct() {
        return testVariableStruct;
    }

    /**
     * Getter.
     * @return value
     */
    public int getTestVariableStructLength() {
        return testVariableStructLength;
    }
}
