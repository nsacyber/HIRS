package hirs.utils.rim.unsignedRim.cbor.ietfCorim.comid;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toMap;

/**
 *  Section 5.1.4.1. of the IETF CoRim specification
 *  <pre>
 *  environment-map = non-empty&lt;{
 *      ? &amp;(class: 0) => class-map
 *      ? &amp;(instance: 1) => $instance-id-type-choice
 *      ? &amp;(group: 2) => $group-id-type-choice
 *    }&gt;
 *  </pre>
 */
@Getter @AllArgsConstructor
public enum EnvironmentMapItems  {
    /** Corresponds to a class-map. */
    CLASS_MAP(0, "class-map"),
    /** Corresponds to an instance. */
    INSTANCE_ID_TYPE_CHOICE(1, "$instance-id-type-choice"),
    /** Corresponds to a group. */
    GROUP_ID_TYPE_CHOICE(2, "$group-id-type-choice");

    private final int index;
    private final String key;

    private static final Map<Integer, EnvironmentMapItems> LOOKUP =
            stream(values())
                    .collect(toMap(EnvironmentMapItems::getIndex, x -> x));

    /**
     * Method to return an enum value from an integer index.
     *
     * @param index The index to reference.
     * @return The enum value, if present, or {@code null} otherwise.
     */
    public static EnvironmentMapItems fromIndex(final int index) {
        return LOOKUP.get(index);
    }
}
