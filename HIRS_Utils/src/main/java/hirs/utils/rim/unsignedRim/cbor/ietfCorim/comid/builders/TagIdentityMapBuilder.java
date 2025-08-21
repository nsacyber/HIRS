package hirs.utils.rim.unsignedRim.cbor.ietfCorim.comid.builders;

import com.authlete.cbor.CBORByteArray;
import com.authlete.cbor.CBORInteger;
import com.authlete.cbor.CBORPair;
import com.authlete.cbor.CBORPairList;
import com.fasterxml.jackson.annotation.JsonProperty;
import hirs.utils.rim.unsignedRim.cbor.ietfCorim.UUIDHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The configuration object for the tag identity map object, described in section 5.1.1.
 */
public class TagIdentityMapBuilder {
    @JsonProperty("tag-id")
    private UUID tagId;
    @JsonProperty("tag-version")
    private Integer tagVersion;

    /**
     * Constructs a tag identity map.
     *
     * @return the constructed tag identity map
     */
    public CBORPairList build() {
        List<CBORPair> pairList = new ArrayList<>();
        pairList.add(new CBORPair(new CBORInteger(0), new CBORByteArray(UUIDHelper.toBytes(tagId))));
        if (tagVersion != null) {
            pairList.add(new CBORPair(new CBORInteger(1), new CBORInteger(tagVersion)));
        }
        return new CBORPairList(pairList);
    }
}
