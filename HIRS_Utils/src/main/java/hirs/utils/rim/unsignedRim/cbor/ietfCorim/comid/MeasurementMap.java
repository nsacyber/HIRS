package hirs.utils.rim.unsignedRim.cbor.ietfCorim.comid;

import com.authlete.cbor.CBORItem;
import com.authlete.cbor.CBORPairList;
import com.authlete.cbor.CBORTaggedItem;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import hirs.utils.signature.cose.Cbor.CborTagProcessor;
import lombok.Getter;
import lombok.Setter;
import org.ietf.jgss.Oid;
import java.util.UUID;

/**
 * A class that defines a <i>measurement</i>, which is a broadly-defined concept that can include a variety of
 * measured values (software, firmware, etc.). See Section 5.1.4.1.4 of the IETF CoRIM specification.
 */
@Setter
@Getter
@JsonTypeName("measurement-map")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
public class MeasurementMap {
    /** Corresponds to the optional {@code mkey} (measurement key) field. If no mkey is present, the
     * measurement is considered <i>anonymous</i>.<p>Represents the {@code tagged-oid-type} choice.*/
    @JsonProperty("mkey (oid-type)")
    private Oid mkeyOid;
    /** Corresponds to the optional {@code mkey} (measurement key) field. If no mkey is present, the
     * measurement is considered <i>anonymous</i>.<p>Represents the {@code tagged-uuid-type} choice.*/
    @JsonProperty("mkey (uuid-type)")
    private UUID mkeyUUID;
    /** Corresponds to the optional {@code mkey} (measurement key) field. If no mkey is present, the
     * measurement is considered <i>anonymous</i>.<p>Represents the {@code tstr} choice.*/
    @JsonProperty("mkey (tstr)")
    private String mkeyStr;
    /** Corresponds to the optional {@code mkey} (measurement key) field. If no mkey is present, the
     * measurement is considered <i>anonymous</i>.<p>Represents the {@code uint} choice.*/
    @JsonProperty("mkey (uint)")
    private Integer mkeyInt;
    /** Corresponds to an {@link ArmSwcompId} object, which is an extension found in the IETF Arm CCA
     * Endorsements specification.*/
    @JsonProperty("mkey (Arm CCA extension)")
    private ArmSwcompId mkeyArmSwcompId;
    /** Corresponds to an {@code mval}, or measurement associated with the environment.*/
    private MeasurementValuesMap mval;

    /**
     * Parses a measurement from a given {@link CBORItem}.
     *
     * @param measurementMap The {@code measurement-map} to process.
     */
    public MeasurementMap(final CBORItem measurementMap) {
        var measurementPairs = ((CBORPairList) measurementMap).getPairs();

        for (var currPair : measurementPairs) {
            var currKey = (int) currPair.getKey().parse();
            var currVal = MeasurementMapItems.fromIndex(currKey);
            if (currVal != null) {
                switch (currVal) {
                    case MKEY -> {
                        var currMkey = currPair.getValue();

                        /* $measured-element-type-choice can be either a tagged-oid-type, tagged-uuid-type,
                         * uint, or tstr. See section 5.1.4.1.4.1 in the IETF CoRIM specification.
                         *
                         * Additionally, the IETF draft-ydb-rats-cca-endorsements extends
                         * $measured-element-type-choice to add tagged-arm-swcomp-id (handled below if
                         * present).
                         */
                        if (currMkey instanceof CBORTaggedItem) {
                            // Handle tagged item: could be extension as seen in IETF
                            // draft-ydb-rats-cca-endorsements
                            CborTagProcessor.process((CBORTaggedItem) currMkey).ifPresent(obj -> {
                                if (obj instanceof Oid) {
                                    // tagged-oid-type
                                    mkeyOid = (Oid) obj;
                                }
                                if (obj instanceof UUID) {
                                    mkeyUUID = (UUID) obj; // tagged-uuid-type
                                } else if (obj instanceof ArmSwcompId) {
                                    mkeyArmSwcompId = (ArmSwcompId) obj; // tagged-arm-swcomp-id
                                }
                            });
                        } else {
                            var parsedMkey = currMkey.parse();
                            if (parsedMkey instanceof String) {
                                mkeyStr = (String) parsedMkey; // tstr
                            } else if (parsedMkey instanceof Integer) {
                                mkeyInt = (int) parsedMkey; // uint
                            }
                        }
                    }
                    case MVAL -> {
                        mval = new MeasurementValuesMap(currPair.getValue());
                    }
                    // Note: AUTHORIZED_BY currently unsupported
                    default -> { }
                }
            }
        }
    }
}
