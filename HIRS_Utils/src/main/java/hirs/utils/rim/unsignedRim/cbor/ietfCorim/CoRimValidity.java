package hirs.utils.rim.unsignedRim.cbor.ietfCorim;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.authlete.cbor.CBORInteger;
import com.authlete.cbor.CBORLong;
import com.authlete.cbor.CBORPair;
import com.authlete.cbor.CBORPairList;
import com.authlete.cbor.CBORTaggedItem;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Class pertaining to a {@code validity-map}. Defined in Section 7.3 of the IETF CoRIM specification.
 */
public class CoRimValidity {
    @JsonProperty("not-before")
    private Date notBefore;
    @JsonProperty("not-after")
    private Date notAfter;

    /**
     * Builds a CBOR representation of the validity map.
     *
     * @return The CBOR object representing the validity map.
     */
    public CBORPairList build() {
        final List<CBORPair> pairs = new ArrayList<>();
        if (notBefore != null) {
            final long notBeforeInt = notBefore.getTime();
            pairs.add(new CBORPair(new CBORInteger(0), new CBORTaggedItem(1, new CBORLong(notBeforeInt))));
        }
        final long notAfterInt = notAfter.getTime() / 1000L; // Convert to epoch time(seconds)
        pairs.add(new CBORPair(new CBORInteger(1), new CBORTaggedItem(1, new CBORLong(notAfterInt))));
        return new CBORPairList(pairs);
    }
}
