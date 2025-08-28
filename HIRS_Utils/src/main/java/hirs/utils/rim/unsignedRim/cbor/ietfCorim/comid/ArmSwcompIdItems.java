package hirs.utils.rim.unsignedRim.cbor.ietfCorim.comid;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toMap;

/**
 * Section 3.1.3.1 of the IETF draft-ydb-rats-cca-endorsements specification.
 * <pre>
 * arm-swcomp-id = {
 *   arm.measurement-type => text
 *   arm.version => text
 *   arm.signer-id => arm.hash-type
 * }
 * </pre>
 */
@Getter @AllArgsConstructor
public enum ArmSwcompIdItems {
    /** The role of this software component. */
    MEASUREMENT_TYPE(1, "arm.measurement-type"),
    /** The issued software version. */
    VERSION(4, "arm.version"),
    /** Uniquely identifies the signer of the software component. */
    SIGNER_ID(5, "arm.signer-id");

    private final int index;
    private final String key;

    private static final Map<Integer, ArmSwcompIdItems> LOOKUP =
            stream(values())
                    .collect(toMap(ArmSwcompIdItems::getIndex, x -> x));

    /**
     * Method to return an enum value from an integer index.
     *
     * @param index The index to reference.
     * @return The enum value, if present, or {@code null} otherwise.
     */
    public static ArmSwcompIdItems fromIndex(final int index) {
        return LOOKUP.get(index);
    }
}
