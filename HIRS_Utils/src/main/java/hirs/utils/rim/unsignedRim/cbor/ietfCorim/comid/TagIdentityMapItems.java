package hirs.utils.rim.unsignedRim.cbor.ietfCorim.comid;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toMap;

/**
 * Section 5.1.1 of the IETF CoRIM specification.
 * <pre>
 * tag-identity-map = {
 *     &amp;(tag-id: 0) => $tag-id-type-choice
 *     ? &amp;(tag-version: 1) => tag-version-type
 * }
 * </pre>
 */
@Getter @AllArgsConstructor
public enum TagIdentityMapItems {
    /** Corresponds to tag-id. */
    TAG_ID(0, "tag-id"),
    /** Corresponds to tag-version. */
    TAG_VERSION(1, "tag-version");

    private final int index;
    private final String key;

    private static final Map<Integer, TagIdentityMapItems> LOOKUP =
            stream(values())
                    .collect(toMap(TagIdentityMapItems::getIndex, x -> x));

    /**
     * Method to return an enum value from an integer index.
     *
     * @param index The index to reference.
     * @return The enum value, if present, or {@code null} otherwise.
     */
    public static TagIdentityMapItems fromIndex(final int index) {
        return LOOKUP.get(index);
    }
}
