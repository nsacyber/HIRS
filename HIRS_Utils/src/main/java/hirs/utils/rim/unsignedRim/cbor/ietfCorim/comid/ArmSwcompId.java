package hirs.utils.rim.unsignedRim.cbor.ietfCorim.comid;

import com.authlete.cbor.CBORItem;
import com.authlete.cbor.CBORPairList;
import com.authlete.cbor.CBORTaggedItem;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents an {@code arm-swcomp-id} object. Contained in the IETF Arm CCA Endorsements specification.
 *
 * @see <a href="https://datatracker.ietf.org/doc/draft-ydb-rats-cca-endorsements/">
 * IETF Arm CCA Endorsements Specification</a>
 */
@Getter
@Setter
@JsonTypeName("arm-swcomp-id")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
public class ArmSwcompId {
    @JsonProperty("arm.measurement-type")
    private String measurementType;
    @JsonProperty("arm.version")
    private String version;
    @JsonProperty("arm.signer-id")
    private byte[] signerId;

    /**
     * Parses an {@code ArmSwcompId} from a given {@link CBORItem}.
     *
     * @param mkeyItem The item containing the record to be parsed.
     */
    public ArmSwcompId(final CBORItem mkeyItem) {
        CBORTaggedItem mkeyTaggedItem = (CBORTaggedItem) mkeyItem;
        var list = ((CBORPairList) mkeyTaggedItem.getTagContent()).getPairs();

        for (var currItem : list) {
            var currKey = (int) currItem.getKey().parse();
            var currType = ArmSwcompIdItems.fromIndex(currKey);
            if (currType != null) {
                switch (currType) {
                    case MEASUREMENT_TYPE -> {
                        measurementType = (String) currItem.getValue().parse();
                    }
                    case VERSION -> {
                        version = (String) currItem.getValue().parse();
                    }
                    case SIGNER_ID -> {
                        signerId = (byte[]) currItem.getValue().parse();
                    }
                    default -> { }
                }
            }
        }
    }
}

