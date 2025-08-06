package hirs.utils.rim.unsignedRim.cbor.ietfCorim.comid;

import lombok.Getter;

import java.util.Map;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toMap;

/**
 * Section 7.2 of the IETF CoRim specification
 * <pre>
 *  entity-map&lt;role-type-choice, extension-socket&gt; = {
 *      &amp;(entity-name: 0) => $entity-name-type-choice
 *      ? &amp;(reg-id: 1) => uri
 *      &amp;(role: 2) => [ + role-type-choice ]
 *      * extension-socket
 *    }
 * </pre>
 */
@Getter
public enum EntityItems {
    /** Corresponds to an entity-name. */
    ENTITY_NAME(0, "entity-name"),
    /** Corresponds to a reg-id. */
    REG_ID(1, "reg-id"),
    /** Corresponds to a role. */
    ROLE(2, "role");

    private final int index;
    private final String key;

    EntityItems(final int index, final String key) {
        this.index = index;
        this.key = key;
    }

    private static final Map<Integer, EntityItems> LOOKUP =
            stream(values())
                    .collect(toMap(EntityItems::getIndex, x -> x));

    /**
     * Method to return an enum value from an integer index.
     *
     * @param index The index to reference.
     * @return The enum value, if present, or {@code null} otherwise.
     */
    public static EntityItems fromIndex(final int index) {
        return LOOKUP.get(index);
    }
}
