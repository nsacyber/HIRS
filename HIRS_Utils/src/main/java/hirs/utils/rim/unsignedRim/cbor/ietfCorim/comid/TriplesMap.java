package hirs.utils.rim.unsignedRim.cbor.ietfCorim.comid;

import com.authlete.cbor.CBORItem;
import com.authlete.cbor.CBORItemList;
import com.authlete.cbor.CBORPairList;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import hirs.utils.rim.unsignedRim.cbor.ietfCorim.comid.triples.ReferenceTripleRecord;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Defines a {@code triples-map}, which contains CoMID <i>triples</i>. Triples are records that link various
 * assertions with security features (such as cryptographic keys, etc.).
 * <p>
 * See Section 5.1.4 in the IETF CoMID specification for further details.
 */
@Getter
@Setter
@JsonTypeName("triples-map")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
public class TriplesMap {
    /** Corresponds to a list of {@code reference-triple-record} values. A <i>reference values triple</i> in
     * this list contains reference measurements or claims pertaining to a target environment.
     */
    private List<ReferenceTripleRecord> referenceTriples;

    /**
     * Parses a {@code triples-map} from a given {@link CBORItem}.
     *
     * @param triplesMap The {@code triples-map} to process.
     */
    public TriplesMap(final CBORItem triplesMap) {
        var list = ((CBORPairList) triplesMap).getPairs();

        for (var currItem : list) {
            var currKey = (int) currItem.getKey().parse();
            var currVal = TriplesItems.fromIndex(currKey);
            if (currVal != null) {
                switch (currVal) {
                    case REFERENCE_TRIPLES -> {
                        referenceTriples = new ArrayList<>();
                        var currRefTriples = ((CBORItemList) currItem.getValue()).getItems();
                        for (int j = 0; j < currRefTriples.size(); j++) {
                            // Reference triples
                            referenceTriples.add(new ReferenceTripleRecord(currItem.getValue()));
                        }
                    }
                    // Note: only REFERENCE_TRIPLES currently supported
                    default -> { }
                }
            }
        }
    }
}
