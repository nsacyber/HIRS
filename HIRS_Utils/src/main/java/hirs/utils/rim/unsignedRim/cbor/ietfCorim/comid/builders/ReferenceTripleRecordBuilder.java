package hirs.utils.rim.unsignedRim.cbor.ietfCorim.comid.builders;

import com.authlete.cbor.CBORItem;
import com.authlete.cbor.CBORItemList;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * The configuration object for the reference triple record, described in section 5.1.4.
 */
public class ReferenceTripleRecordBuilder {
    @JsonProperty("ref-env")
    private EnvironmentMapBuilder refEnv;
    @JsonProperty("ref-claims")
    private List<MeasurementMapBuilder> refClaims;

    /**
     * Constructs a reference triple record.
     *
     * @return the constructed reference triple record
     */
    public CBORItemList build() {
        List<CBORItem> itemList = new ArrayList<>();
        itemList.add(refEnv.build());
        List<CBORItem> claimsList = new ArrayList<>();
        refClaims.forEach(claim -> {
            claimsList.add(claim.build());
        });
        itemList.add(new CBORItemList(claimsList));
        return new CBORItemList(itemList);
    }
}
