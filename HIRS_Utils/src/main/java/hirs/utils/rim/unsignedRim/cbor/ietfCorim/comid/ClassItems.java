package hirs.utils.rim.unsignedRim.cbor.ietfCorim.comid;

import lombok.Getter;

import java.util.Map;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toMap;

/**
 * Section 5.1.4.1.1 of the IETF Specification
 *
 * <pre>
 * class-map = non-empty&lt;{
 *      ? &amp;(class-id: 0) => $class-id-type-choice
 *      ? &amp;(vendor: 1) => tstr
 *      ? &amp;(model: 2) => tstr
 *      ? &amp;(layer: 3) => uint
 *      ? &amp;(index: 4) => uint
 *    }&gt;
 * </pre>
 */
@Getter
public enum ClassItems {
    /** Corresponds to a class-id. */
    CLASS_ID(0, "class-id"),
    /** Corresponds to vendor. */
    VENDOR(1, "vendor"),
    /** Corresponds to model. */
    MODEL(2, "model"),
    /** Corresponds to a layer. */
    LAYER(3, "layer"),
    /** Corresponds to an index. */
    INDEX(4, "index");

    private final int index;
    private final String key;

    private static final Map<Integer, ClassItems> LOOKUP =
            stream(values())
                    .collect(toMap(ClassItems::getIndex, x -> x));

    ClassItems(final int index, final String key) {
        this.index = index;
        this.key = key;
    }

    /**
     * Method to return an enum value from an integer index.
     *
     * @param index The index to reference.
     * @return The enum value, if present, or {@code null} otherwise.
     */
    public static ClassItems fromIndex(final int index) {
        return LOOKUP.get(index);
    }
}
