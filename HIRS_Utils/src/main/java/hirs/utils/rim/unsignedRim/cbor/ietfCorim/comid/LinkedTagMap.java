package hirs.utils.rim.unsignedRim.cbor.ietfCorim.comid;

import com.authlete.cbor.CBORItem;
import com.authlete.cbor.CBORPairList;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import hirs.utils.signature.cose.Cbor.CborTagProcessor;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;
/**
 * A class that defines a <i>linked tag map</i>, representing a typed relationship between the source CoMID
 * tag and the target CoMID tag. See Section 5.1.3 of the IETF CoRIM specification.
 */
@Getter
@Setter
@JsonTypeName("linked-tag-map")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
public class LinkedTagMap {
    /** Corresponds to the {@code linked-tag-id} field and defines a unique identifier for the target tag.
     * Represents the {@code tstr} choice. */
    @JsonProperty("linked-tag-id (tstr)")
    private String linkedTagIdStr;
    /** Corresponds to the {@code linked-tag-id} field and defines a unique identifier for the target tag.
     * Represents the {@code uuid-type} choice. */
    @JsonProperty("linked-tag-id (uuid-type)")
    private UUID linkedTagIdUUID;
    /** Corresponds to the optional {@code tag-version} field. Corresponds to versioning information for the
     * {@code tag-id}.*/
    private TagRelTypeChoice tagRelTypeChoice;

    /**
     * Parses a linked tag map from a given {@link CBORItem}.
     *
     * @param linkedTagMap The {@code linked-tag-map} to process.
     */
    public LinkedTagMap(final CBORItem linkedTagMap) {

        var list = ((CBORPairList) linkedTagMap).getPairs();

        for (var cborPair : list) {
            var currKey = (int) cborPair.getKey().parse();
            var currVal = LinkedTagMapItems.fromIndex(currKey);
            if (currVal != null) {
                switch (currVal) {
                    case LINKED_TAG_ID -> {
                        // Linked tag ID
                        var tagIdParsed = list.get(currKey).getValue().parse();
                        // $tag-id-type-choice can either be a tstr or uuid-type
                        if (tagIdParsed instanceof String) {
                            linkedTagIdStr = (String) tagIdParsed; // tstr
                        } else {
                            // uuid-type
                            linkedTagIdUUID = CborTagProcessor.
                                    bytesToUUID((byte[]) tagIdParsed);
                        }
                    }
                    case TAG_REL -> {
                        // Tag rel
                        int tagVal = (int) list.get(currKey).getValue().parse();
                        var currTagRelType = TagRelTypeChoice.fromIndex(tagVal);
                        if (currTagRelType != null) {
                            tagRelTypeChoice = currTagRelType;
                        }
                    }
                    default -> { }
                }
            }
        }
    }
}
