package hirs.utils.rim.unsignedRim.cbor.ietfCorim;

import java.util.ArrayList;
import java.util.List;

import com.authlete.cbor.CBORInteger;
import com.authlete.cbor.CBORItemList;
import com.authlete.cbor.CBORPair;
import com.authlete.cbor.CBORPairList;
import com.authlete.cbor.CBORString;
import com.authlete.cbor.CBORTaggedItem;
import com.authlete.cbor.CBORizer;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

/**
 * Class pertaining to a {@code corim-entity-map}. Defined in Section 4.1.5 of
 * the IETF CoRIM specification.
 */

public class CoRimEntityMap {
    @JsonProperty("entity-name") @Getter @Setter
    private String entityName;
    @JsonProperty("reg-id") @Getter @Setter
    private String regId;
    private List<Integer> role;
    static final int CORIM_REG_ID_TAG = 32;

    /**
     * Builds a corim-entity-map CBOR representation.
     *
     * @return The CBOR representation of a corim-entity-map.
     */
    public CBORPairList build() {
        final List<CBORPair> pairs = new ArrayList<>();
        pairs.add(new CBORPair(new CBORInteger(0), new CBORString(entityName))); // entity-name
        final CBORTaggedItem regIdURI = new CBORTaggedItem(CORIM_REG_ID_TAG,
                new CBORString(regId)); // reg-id (URI tag)
        pairs.add(new CBORPair(new CBORInteger(1), regIdURI)); // reg-id
        final CBORItemList roleList = (CBORItemList) new CBORizer().cborize(role);
        pairs.add(new CBORPair(new CBORInteger(2), roleList)); // reg-id
        return new CBORPairList(pairs);
    }

    /**
     * Returns a copy of the role list.
     *
     * @return a defensive copy of the role list
     */
    public List<Integer> getRole() {
        return new ArrayList<>(role);
    }

    /**
     * Sets the role list using a defensive copy.
     *
     * @param role the list to set
     */
    public void setRole(final List<Integer> role) {
        this.role = new ArrayList<>(role);
    }
}
