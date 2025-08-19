package hirs.utils.rim.unsignedRim.cbor.ietfCorim.comid.builders;

import com.authlete.cbor.CBORInteger;
import com.authlete.cbor.CBORItem;
import com.authlete.cbor.CBORItemList;
import com.authlete.cbor.CBORPair;
import com.authlete.cbor.CBORPairList;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * The configuration object for the triples object, described in section 5.1.4.
 */
@Getter @Setter
public class TriplesMapBuilder {
    @JsonProperty("reference-triples")
    private List<ReferenceTripleRecordBuilder> referenceTripleRecordList;

    /**
     * Constructs a triples object.
     *
     * @return the constructed triples object
     */
    public CBORPairList build() {
        List<CBORPair> pairList = new ArrayList<>();
        if (referenceTripleRecordList != null) {
            List<CBORItem> itemList = new ArrayList<>();
            referenceTripleRecordList.forEach(rtr -> {
                itemList.add(rtr.build());
            });
            pairList.add(new CBORPair(new CBORInteger(0), new CBORItemList(itemList)));
        }
        return new CBORPairList(pairList);
    }
}
