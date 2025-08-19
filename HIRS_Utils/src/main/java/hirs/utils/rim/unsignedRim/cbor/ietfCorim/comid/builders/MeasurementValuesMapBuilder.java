package hirs.utils.rim.unsignedRim.cbor.ietfCorim.comid.builders;

import com.authlete.cbor.CBORInteger;
import com.authlete.cbor.CBORItem;
import com.authlete.cbor.CBORItemList;
import com.authlete.cbor.CBORPair;
import com.authlete.cbor.CBORPairList;
import hirs.utils.rim.unsignedRim.cbor.ietfCorim.CoRimDigest;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * The configuration object for the measurement-values-map, described in section 5.1.4.1.4.2.
 */
@Getter
@Setter
public class MeasurementValuesMapBuilder {
    /* The following variables are commented out but kept here for reference as part of the
     * measurement-values-map as described in Section 5.1.4.1.4.2 of the specification.
     */
    //VersionMapBuilder version;
    //Integer svn;
    private List<CoRimDigest> digests;
    //FlagsMap flags;
    //@JsonProperty("raw-value")
    //byte[] rawValue;
    //@JsonProperty("mac-addr")
    //byte[] macAddr;
    //@JsonProperty("ip-addr")
    //byte[] ipAddr;
    //@JsonProperty("serial-number")
    //String serialNumber;
    //byte[] ueid;
    //byte[] uuid;
    //String name;
    //List<byte[]> cryptokeys;
    //IntegrityRegistersBuilder integrityRegisters;

    /**
     * Constructs a measurement-values-map.
     *
     * @return the constructed measurement-values-map
     */
    public CBORItem build() {
        List<CBORPair> pairList = new ArrayList<>();
        if (digests != null) {
            List<CBORItem> digestItems = new ArrayList<>();
            digests.forEach(digest -> {
                digestItems.add(digest.build());
            });
            pairList.add(new CBORPair(new CBORInteger(2), new CBORItemList(digestItems)));
        }
        return new CBORPairList(pairList);
    }
}
