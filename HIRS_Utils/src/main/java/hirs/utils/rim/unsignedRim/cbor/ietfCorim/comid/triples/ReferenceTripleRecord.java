package hirs.utils.rim.unsignedRim.cbor.ietfCorim.comid.triples;

import com.authlete.cbor.CBORItem;
import com.authlete.cbor.CBORItemList;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import hirs.utils.rim.unsignedRim.cbor.ietfCorim.comid.EnvironmentMap;
import hirs.utils.rim.unsignedRim.cbor.ietfCorim.comid.MeasurementMap;
import lombok.Getter;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;

/**
 * A class corresponding to a <i>reference values triple</i>, and comprises a set of reference measurements or
 * claims pertaining to a specific environment.
 * <p>
 * See Section 5.1.4.2 of the IETF CoRIM specification.
 */
@Setter
@Getter
@JsonTypeName("reference-triple-record")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
public class ReferenceTripleRecord {
    /**
     * Corresponds to the {@code ref-env} field. Identifies the target environment for reference claims.
     */
    private EnvironmentMap refEnv;
    /**
     * Corresponds to the {@code ref-claims} field. Comprises a list of <i>reference claims</i>, which are
     * measurements associated with the environment.
     */
    private List<MeasurementMap> refClaims;

    /**
     * Parses a reference triple from a given {@link CBORItem}.
     *
     * @param refTripleRecord The item containing the reference triple to be parsed.
     */
    public ReferenceTripleRecord(final CBORItem refTripleRecord) {
        var list = ((CBORItemList) ((CBORItemList) refTripleRecord).getItems().iterator().next()).getItems();

        var iterator = list.iterator();
        refEnv = new EnvironmentMap(iterator.next()); // ref-env
        refClaims = new ArrayList<>(); // ref-claims
        var currRefClaims = ((CBORItemList) iterator.next()).getItems();
        for (CBORItem currRefClaim : currRefClaims) {
            refClaims.add(new MeasurementMap(currRefClaim));
        }
    }
}
