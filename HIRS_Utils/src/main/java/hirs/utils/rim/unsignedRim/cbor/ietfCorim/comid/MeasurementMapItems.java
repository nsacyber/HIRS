package hirs.utils.rim.unsignedRim.cbor.ietfCorim.comid;

import lombok.Getter;

import java.util.Map;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toMap;

/**
 *  Section 5.1.4.1.4 of the IETF CORIM specification
 *
 * <pre>
 *  measurement-map = {
 *      ? &amp;(mkey: 0) => $measured-element-type-choice
 *      &amp;(mval: 1) => measurement-values-map
 *      ? &amp;(authorized-by: 2) => [ + $crypto-key-type-choice ]
 *    }
 * </pre>
 */
@Getter
public enum MeasurementMapItems {
    /** Corresponds to an mkey. */
    MKEY(0, "mkey"),
    /** Corresponds to an mval. */
    MVAL(1, "mval"),
    /** Corresponds to authorized-by. */
    AUTHORIZED_BY(2, "authorized-by");

    private final int index;
    private final String key;

    MeasurementMapItems(final int index, final String key) {
        this.index = index;
        this.key = key;
    }

    private static final Map<Integer, MeasurementMapItems> LOOKUP =
            stream(values())
                    .collect(toMap(MeasurementMapItems::getIndex, x -> x));

    /**
     * Method to return an enum value from an integer index.
     *
     * @param index The index to reference.
     * @return The enum value, if present, or {@code null} otherwise.
     */
    public static MeasurementMapItems fromIndex(final int index) {
        return LOOKUP.get(index);
    }
}
