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
 * An object representing a {@code tag-identity-map} from Section 5.1.1 of the IETF CoRIM specification.
 * Contains unique identifying information for the CoMID.
 */
@Getter
@Setter
@JsonTypeName("tag-identity-map")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
public class TagIdentityMap {
    /** Corresponds to the {@code tag-id} field, representing a globally unique identifier for the CoMID.
     * Represents the {@code tstr} choice.*/
    @JsonProperty("tag-id (tstr)")
    private String tagIdStr;
    /** Corresponds to the {@code tag-id} field, representing a globally unique identifier for the CoMID.
     * Represents the {@code uuid-type} choice.*/
    @JsonProperty("tag-id (uuid-type)")
    private UUID tagIdUUID;
    /** Corresponds to the {@code tag-version} field. The tag version is a positive numeric value
     * corresponding to the release revision of the tag.*/
    private Integer tagVersion;

    /**
     * Parses a {@code tag-identity-map} from a given {@link CBORItem}.
     *
     * @param tagIdentityMap The {@code tag-identity-map} to process.
     */
    public TagIdentityMap(final CBORItem tagIdentityMap) {
        var list = ((CBORPairList) tagIdentityMap).getPairs();

        for (var currItem : list) {
            var currKey = (int) currItem.getKey().parse();
            var currVal = TagIdentityMapItems.fromIndex(currKey);
            if (currVal != null) {
                switch (currVal) {
                    case TAG_ID -> {
                        // Tag ID
                        var tagIdParsed = currItem.getValue().parse();
                        // $tag-id-type-choice can either be a tstr or uuid-type
                        if (tagIdParsed instanceof String) {
                            tagIdStr = (String) currItem.getValue().parse(); // tstr
                        } else {
                            // uuid-type
                            tagIdUUID = CborTagProcessor.bytesToUUID((byte[]) currItem.getValue().parse());
                        }
                    }
                    // Tag version
                    case TAG_VERSION -> {
                        tagVersion = (int) currItem.getValue().parse();
                    }
                    default -> { }
                }
            }
        }
    }
}
