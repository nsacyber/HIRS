package hirs.utils.rim.unsignedRim.cbor.ietfCorim.comid.builders;

import com.authlete.cbor.CBORByteArray;
import com.authlete.cbor.CBORInteger;
import com.authlete.cbor.CBORPair;
import com.authlete.cbor.CBORPairList;
import com.authlete.cbor.CBORString;
import com.authlete.cbor.CBORTaggedItem;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * The configuration object for the class-map, described in section 5.1.4.1.1.
 */
@Getter
@Setter
public class ComidClassBuilder {
    @JsonProperty("class-id")
    private byte[] classId;
    private String vendor;
    private String model;
    private Integer layer;
    private Integer index;

    /**
     * Constructs a class-map.
     *
     * @return the constructed class-map
     */
    public CBORPairList build() {
        List<CBORPair> pairList = new ArrayList<>();
        if (classId != null) {
            final int taggedBytesNo = 560;
            CBORTaggedItem classIdTagged = new CBORTaggedItem(taggedBytesNo, new CBORByteArray(classId));
            pairList.add(new CBORPair(new CBORInteger(0), classIdTagged));
        }
        if (vendor != null) {
            pairList.add(new CBORPair(new CBORInteger(1), new CBORString(vendor)));
        }
        if (model != null) {
            pairList.add(new CBORPair(new CBORInteger(2), new CBORString(model)));
        }
        if (layer != null) {
            final int layerPairNo = 3;
            pairList.add(new CBORPair(new CBORInteger(layerPairNo), new CBORInteger(layer)));
        }
        if (index != null) {
            final int indexPairNo = 4;
            pairList.add(new CBORPair(new CBORInteger(indexPairNo), new CBORInteger(index)));
        }
        return new CBORPairList(pairList);
    }
}
