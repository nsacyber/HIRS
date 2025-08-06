package hirs.utils.rim.unsignedRim.cbor.ietfCorim.comid;

import lombok.Getter;

import java.util.Map;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toMap;

/**
 * Section 5.1.4. of the IETF CoRim specification
 * <pre>
 * triples-map = non-empty&lt;{
 *      ? &amp;(reference-triples: 0) =>
 *        [ + reference-triple-record ]
 *      ? &amp;(endorsed-triples: 1) =>
 *        [ + endorsed-triple-record ]
 *      ? &amp;(identity-triples: 2) =>
 *        [ + identity-triple-record ]
 *      ? &amp;(attest-key-triples: 3) =>
 *        [ + attest-key-triple-record ]
 *      ? &amp;(dependency-triples: 4) =>
 *        [ + domain-dependency-triple-record ]
 *      ? &amp;(membership-triples: 5) =>
 *        [ + domain-membership-triple-record ]
 *      ? &amp;(coswid-triples: 6) =>
 *        [ + coswid-triple-record ]
 *      ? &amp;(conditional-endorsement-series-triples: 8) =>
 *        [ + conditional-endorsement-series-triple-record ]
 *      ? &amp;(conditional-endorsement-triples: 10) =>
 *        [ + conditional-endorsement-triple-record ]
 *      * $$triples-map-extension
 *    }&gt;
 * </pre>
 */
@Getter
public enum TriplesItems {
    /** Corresponds to reference-triples. */
    REFERENCE_TRIPLES(0, "reference-triples"),
    /** Corresponds to endorsed-triples. */
    ENDORSED_TRIPLES(1, "endorsed-triples"),
    /** Corresponds to identity-triples. */
    IDENTITY_TRIPLES(2, "identity-triples"),
    /** Corresponds to attest-key-triples. */
    ATTEST_KEY_TRIPLES(3, "attest-key-triples"),
    /** Corresponds to dependency-triples. */
    DEPENDENCY_TRIPLES(4, "dependency-triples"),
    /** Corresponds to membership-triples. */
    MEMBERSHIP_TRIPLES(5, "membership-triples"),
    /** Corresponds to coswid-triples. */
    COSWID_TRIPLES(6, "coswid-triples"),
    /** Corresponds to conditional-endorsement-series-triples. */
    CONDITIONAL_ENDORSEMENT_SERIES_TRIPLES(8, "conditional-endorsement-series-triples"),
    /** Corresponds to conditional-endorsement-triples. */
    CONDITIONAL_ENDORSEMENT_TRIPLES(10, "conditional-endorsement-triples");

    private final int index;
    private final String key;

    TriplesItems(final int index, final String key) {
        this.index = index;
        this.key = key;
    }

    private static final Map<Integer, TriplesItems> LOOKUP =
            stream(values())
                    .collect(toMap(TriplesItems::getIndex, x -> x));

    /**
     * Method to return an enum value from an integer index.
     *
     * @param index The index to reference.
     * @return The enum value, if present, or {@code null} otherwise.
     */
    public static TriplesItems fromIndex(final int index) {
        return LOOKUP.get(index);
    }
}
