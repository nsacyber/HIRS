package hirs.utils.rim.unsignedRim.cbor.ietfCorim.comid;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toMap;

/**
 *  Converts CoMID indices to strings per the CoRIM spec.
 *
 *  <pre>
 *  concise-mid-tag = {
 *      ? &amp;(language: 0) => text
 *      &amp;(tag-identity: 1) => tag-identity-map
 *      ? &amp;(entities: 2) => [ + comid-entity-map ]
 *      ? &amp;(linked-tags: 3) => [ + linked-tag-map ]
 *      &amp;(triples: 4) => triples-map
 *      * $$concise-mid-tag-extension
 *    }
 *  </pre>
 */
@Getter @AllArgsConstructor
public enum ComidItems {
    /** Corresponds to language. */
    LANGUAGE(0, "language"),
    /** Corresponds to tag-identity. */
    TAG_ID(1, "tag-identity"),
    /** Corresponds to entities. */
    COMID_ENTITY_MAP(2, "entities"),
    /** Corresponds to linked-tags. */
    LINKED_TAG_MAP(3, "linked-tags"),
    /** Corresponds to triples. */
    TRIPLES_MAP(4, "triples");

    private final int index;
    private final String key;

    private static final Map<Integer, ComidItems> LOOKUP =
            stream(values())
                    .collect(toMap(ComidItems::getIndex, x -> x));

    /**
     * Method to return an enum value from an integer index.
     *
     * @param index The index to reference.
     * @return The enum value, if present, or {@code null} otherwise.
     */
    public static ComidItems fromIndex(final int index) {
        return LOOKUP.get(index);
    }
}
