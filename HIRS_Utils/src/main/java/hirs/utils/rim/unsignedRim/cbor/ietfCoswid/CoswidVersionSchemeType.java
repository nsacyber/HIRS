package hirs.utils.rim.unsignedRim.cbor.ietfCoswid;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toMap;

/**
 * An enum pertaining to values contained in the Version Scheme table of Section 4.1
 * of the RFC 9393 specification.
 * Used by {@link CoswidVersionScheme}.
 * <p>
 * Valid types include:
 * <pre>{@code $version-scheme /= &(multipartnumeric: 1)
 * $version-scheme /= &(multipartnumeric-suffix: 2)
 * $version-scheme /= &(alphanumeric: 3)
 * $version-scheme /= &(decimal: 4)
 * $version-scheme /= &(semver: 16384)
 * $version-scheme /= int / text}</pre>
 */
@Getter @AllArgsConstructor
public enum CoswidVersionSchemeType {
    /** Numbers separated by dots, where the numbers are interpreted as decimal integers. */
    MULTIPARTNUMERIC(1, "multipartnumeric"),
    /** multipartnumeric with an additional textual suffix. */
    MULTIPARTNUMERIC_SUFFIX(2, "multipartnumeric-suffix"),
    /** Strictly a string, no interpretation as number. */
    ALPHANUMERIC(3, "alphanumeric"),
    /** A single decimal floating-point number. */
    DECIMAL(4, "decimal"),
    /** A semantic version as defined by SWID. */
    SEMVER(16384, "semver");

    private final int index;
    private final String key;

    /**
     * A lookup map that associates each integer index with its corresponding enum constant.
     */
    private static final Map<Integer, CoswidVersionSchemeType> LOOKUP =
            stream(values())
                    .collect(toMap(CoswidVersionSchemeType::getIndex, x -> x));

    /**
     * Method to return an enum value from an integer index.
     *
     * @param index The index to reference.
     * @return The enum value, if present, or {@code null} otherwise.
     */
    public static CoswidVersionSchemeType fromIndex(final int index) {
        return LOOKUP.get(index);
    }
}
