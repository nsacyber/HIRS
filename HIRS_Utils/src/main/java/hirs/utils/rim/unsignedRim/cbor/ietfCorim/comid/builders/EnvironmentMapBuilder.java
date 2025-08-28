package hirs.utils.rim.unsignedRim.cbor.ietfCorim.comid.builders;

import com.authlete.cbor.CBORByteArray;
import com.authlete.cbor.CBORInteger;
import com.authlete.cbor.CBORPair;
import com.authlete.cbor.CBORPairList;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * The configuration object for an environment-map, described in section 5.1.4.1.
 */
public class EnvironmentMapBuilder {
    @JsonProperty("class")
    private ComidClassBuilder comidClass;
    private byte[] instance;
    private byte[] group;

    /**
     * Constructs an environment-map.
     *
     * @return the constructed environment-amp
     */
    public CBORPairList build() {
        List<CBORPair> pairList = new ArrayList<>();
        if (comidClass != null) {
            pairList.add(new CBORPair(new CBORInteger(0), comidClass.build()));
        }
        if (instance != null) {
            pairList.add(new CBORPair(new CBORInteger(1), new CBORByteArray(instance)));
        }
        if (group != null) {
            pairList.add(new CBORPair(new CBORInteger(2), new CBORByteArray(group)));
        }
        return new CBORPairList(pairList);
    }
}
