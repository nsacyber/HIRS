package hirs.structs.converters;

import hirs.structs.elements.Struct;
import hirs.structs.elements.StructElementLength;
import hirs.structs.elements.StructElements;
import lombok.Getter;

import java.util.Arrays;

/**
 * A struct to be embedded within TestStruct.
 */
@Getter
@StructElements(elements = {"embeddedSize", "embeddedShort", "embedded"})
public class TestEmbeddedStruct implements Struct {

    private static final int EMBEDDED_SIZE = 10;

    private static final short EMBEDDED_SHORT = 7;

    private static final int HASH_CODE = 31;

    private static final byte[] DEFAULT_ARRAY = new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 0};

    @StructElementLength(fieldName = "embedded")
    private int embeddedSize = EMBEDDED_SIZE;

    private short embeddedShort = EMBEDDED_SHORT;

    private byte[] embedded = DEFAULT_ARRAY;

    /**
     * Returns true if the provided object is equivalent to this class.
     *
     * @param o object to compare
     * @return true if the provided object is equal to this class
     */
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TestEmbeddedStruct that = (TestEmbeddedStruct) o;

        return embeddedSize == that.embeddedSize && embeddedShort == that.embeddedShort
                && Arrays.equals(embedded, that.embedded);
    }

    @Override
    public final int hashCode() {
        int result = embeddedSize;
        result = HASH_CODE * result + Arrays.hashCode(embedded);
        return result;
    }

}
