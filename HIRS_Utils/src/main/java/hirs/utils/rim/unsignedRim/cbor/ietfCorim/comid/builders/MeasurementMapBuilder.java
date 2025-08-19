package hirs.utils.rim.unsignedRim.cbor.ietfCorim.comid.builders;

import com.authlete.cbor.CBORByteArray;
import com.authlete.cbor.CBORInteger;
import com.authlete.cbor.CBORItemList;
import com.authlete.cbor.CBORPair;
import com.authlete.cbor.CBORPairList;
import com.authlete.cbor.CBORTaggedItem;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * The configuration object for the measurement-map, described in section 5.1.4.
 */
@Getter
@Setter
public class MeasurementMapBuilder {
    private Integer mkey;
    private MeasurementValuesMapBuilder mval;
    @JsonProperty("authorized-by")
    private List<byte[]> authorizedBy;

    /**
     * Constructs a measurement-map.
     *
     * @return the constructed measurement-map
     */
    public CBORPairList build() {
        List<CBORPair> pairList = new ArrayList<>();
        if (mkey != null) {
            pairList.add(new CBORPair(new CBORInteger(0), new CBORInteger(mkey)));
        }
        pairList.add(new CBORPair(new CBORInteger(1), mval.build()));
        if (authorizedBy != null) {
            final int taggedBytesNo = 560; // Corresponds to tagged-bytes
            List<CBORTaggedItem> authByList = new ArrayList<>();
            authorizedBy.forEach(authBy -> {
                authByList.add(new CBORTaggedItem(taggedBytesNo, new CBORByteArray(authBy)));
            });
            pairList.add(new CBORPair(new CBORInteger(2), new CBORItemList(authByList)));
        }
        return new CBORPairList(pairList);
    }
}
