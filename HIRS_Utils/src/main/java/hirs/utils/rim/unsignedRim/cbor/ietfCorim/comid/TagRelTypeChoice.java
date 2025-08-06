package hirs.utils.rim.unsignedRim.cbor.ietfCorim.comid;

import lombok.Getter;

import java.util.Map;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toMap;

/**
 * Represents a typed relationship between a source CoMID tag and a target CoMID tag.
 * See section 5.1.3 in the IETF CoRIM specification.
 */
@Getter
public enum TagRelTypeChoice {
    /** The source tag provides additional information about the module described in the target tag. */
    SUPPLEMENTS(0),
    /** The source tag corrects erroneous information contained in the target tag. */
    REPLACES(1);

    private final int index;

    TagRelTypeChoice(final int index) {
        this.index = index;
    }

    private static final Map<Integer, TagRelTypeChoice> LOOKUP =
            stream(values())
                    .collect(toMap(TagRelTypeChoice::getIndex, x -> x));

    /**
     * Method to return an enum value from an integer index.
     *
     * @param index The index to reference.
     * @return The enum value, if present, or {@code null} otherwise.
     */
    public static TagRelTypeChoice fromIndex(final int index) {
        return LOOKUP.get(index);
    }
}
