package hirs.utils.rim.unsignedRim.cbor.ietfCorim.comid;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toMap;

/**
 * Defines a {@code $comid-role-type-choice} as described in Section 5.1.2 of the IETF CoRIM specification.
 */
@Getter @AllArgsConstructor
public enum ComidRoleTypeChoice {
    /** Creator of the CoMID tag. */
    TAG_CREATOR(0),
    /** Original maker of the module described by the CoMID tag. */
    CREATOR(1),
    /** An entity making changes to the module described by the CoMID tag. */
    MAINTAINER(2);

    private final int index;

    private static final Map<Integer, ComidRoleTypeChoice> LOOKUP =
            stream(values())
                    .collect(toMap(ComidRoleTypeChoice::getIndex, x -> x));

    /**
     * Method to return an enum value from an integer index.
     *
     * @param index The index to reference.
     * @return The enum value, if present, or {@code null} otherwise.
     */
    public static ComidRoleTypeChoice fromIndex(final int index) {
        return LOOKUP.get(index);
    }
}
