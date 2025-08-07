package hirs.utils.rim.unsignedRim.cbor.ietfCoswid;

import lombok.Getter;
import lombok.Setter;

/**
 * Corresponds to the {@code version-scheme} definition defined in Section 4.1 of RFC 9393.
 * <p>
 * Describes the format of
 * the version pertaining to a measured environment, used in version-map.
 * @see <a href="https://datatracker.ietf.org/doc/rfc9393/">IETF CoSWID Specification</a>
 */
@Getter
@Setter
public class CoswidVersionScheme {
    /** Corresponds to various choices for the {@code $version-scheme} type that are defined in RFC 9393. See
     * {@link CoswidVersionSchemeType} for details.*/
    private CoswidVersionSchemeType coswidVersionSchemeType;
    /** Corresponds to the {@code int} choice for the {@code $version-scheme} type.*/
    private Integer versionSchemeInt;
    /** Corresponds to the {@code text} choice for the {@code $version-scheme} type.*/
    private String versionSchemeText;

    /**
     * Creates an object corresponding to a {@code $version-scheme}. To create, either a valid index
     * corresponding to the table found in Section 4.1 of RFC 9393 is used, or a text string (user-defined).
     *
     * @param object The object to be used during the version scheme creation.
     */
    public CoswidVersionScheme(final Object object) {
        if (object instanceof Integer) {
            var index = CoswidVersionSchemeType.fromIndex((int) object);
            if (index != null) {
                coswidVersionSchemeType = index;
            } else {
                versionSchemeInt = (int) object;
            }
        } else {
            versionSchemeText = (String) object;
        }
    }
}
