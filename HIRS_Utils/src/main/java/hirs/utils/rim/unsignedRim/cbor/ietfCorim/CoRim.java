package hirs.utils.rim.unsignedRim.cbor.ietfCorim;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

import lombok.Getter;
import lombok.Setter;

/**
 * Concise Reference Integrity Manifests (CoRIM) specifies a data model for
 * Endorsements and Reference Values. CoRims are defined by IETF:
 * https://datatracker.ietf.org/doc/draft-ietf-rats-corim/
 */
@JsonTypeName("corim-map") @JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.WRAPPER_OBJECT)
public class CoRim {
    public static final int TAGGED_CORIM = 500; // CoRim notes as Reserved for
    // backward compatibility
    public static final int TAGGED_CORIM_MAP = 501;
    public static final int TAGGED_CORIM_RESERVE1 = 502;
    public static final int TAGGED_CORIM_RESERVE2 = 503;
    public static final int TAGGED_CORIM_RESERVE3 = 504;
    public static final int TAGGED_CONCISE_SWID_TAG = 505;
    public static final int TAGGED_CONCISE_MID_TAG = 506;
    public static final int TAGGED_CORIM_RESERVE4 = 507;
    public static final int TAGGED_CONCISE_TL_TAG = 508;
    public static final int TAGGED_UEID_TYPE = 550;
    public static final int TAGGED_SVN = 552;
    public static final int TAGGED_MIN_SVN = 553;
    public static final int TAGGED_PKIX_BASE64_KEY_TYPE = 554;
    public static final int TAGGED_PKIX_BASE64_CERT_TYPE = 555;
    public static final int TAGGED_THUMBPRINT_TYPE = 557;
    public static final int TAGGED_COSE_KEY_TYPE = 558;
    public static final int TAGGED_CERT_THUMBPRINT_TYPE = 559;
    public static final int TAGGED_BYTES = 560;
    public static final int TAGGED_TAGGED_CERT_THUMBPRINT_TYPE = 561;
    public static final int TAGGED_PKIX_ASN1DER_CERT_TYPE = 562;
    public static final int TAGGED_MASK_RAW_VALUE = 563;

    public static final String TAGGED_CONCISE_SWID_TAG_STR = "concise-swid-tag";
    public static final String TAGGED_CONCISE_MID_TAG_STR = "concise-mid-tag";
    public static final String TAGGED_CONCISE_TL_TAG_STR = "concise-tl-tag";

    // Corim defines a single extra option for the COSE protected header
    public static final int CORIM_META_MAP = 8;
    public static final int CORIM_EARMARKED_LOWER_BOUND = 500;
    public static final int CORIM_EARMARKED_UPPER_BOUND = 599;
    // CoRIM defined attributes found in the IETF CoRIM Specification
    @Setter @Getter
    protected String id = "";
    @Setter @Getter
    protected int corimTag = 0;
    /**
     * Hold a set of "dependent rims" with a URI and URI digest for each entry.
     */
    protected HashMap<String, byte[]> dependentRims = new HashMap<>();
    @Setter @Getter
    protected String profile = "";
    @Setter @Getter
    protected String entities = "";
    @Setter @Getter
    protected long notBefore = 0;
    @Setter @Getter
    protected String notBeforeStr = "";
    @Setter @Getter
    protected long notAfter = 0;
    @Setter @Getter
    protected String notAfterStr = "";
    @Setter @Getter
    protected String entityName = "";
    @Setter @Getter
    protected String entityRegId = "";
    @Setter @Getter
    protected String entityRole = "";

    /**
     * Default CoRim Constructor.
     */
    public CoRim() {

    }

//    /**
//     * CoRim constructor that takes in CoRIm in the form of a Byte Array.
//     * @param data: holds the CoRim data to be parsed
//     */
//    public CoRim(byte[] data) {
//
//    }

    /**
     * Determines if a given tag refers to CoRim. CoRIM Specifies 500 - 599 as
     * "EARMARKED".
     *
     * @param tag the CBOR tag value to check
     * @return true if the tag is defined by CoRim
     */
    public static boolean isCoRimTag(final int tag) {
        if ((tag >= CORIM_EARMARKED_LOWER_BOUND) & (tag <= CORIM_EARMARKED_UPPER_BOUND)) {
            return true;
        }
        return false;
    }

    /**
     * Determines if a given tag refers to CoMid. CoRIM Specifies 506 as
     * "tagged-concise-mid-tag".
     *
     * @param tag the CBOR tag value to check
     * @return true if the tag is defined by CoRim
     */
    public static boolean isCoMidTag(final int tag) {
        if (tag == TAGGED_CONCISE_MID_TAG) {
            return true;
        }
        return false;
    }

    /**
     * Determines if a given tag refers to CoSwid. CoRIM Specifies 505 as
     * "tagged-concise-swid-tag".
     *
     * @param tag the CBOR tag value to check
     * @return true if the tag is defined by CoRim
     */
    public static boolean isCoSwidTag(final int tag) {
        if (tag == TAGGED_CONCISE_SWID_TAG) {
            return true;
        }
        return false;
    }

    /**
     * Determines if a given tag refers to TL. CoRIM Specifies 505 as
     * "tagged-concise-tl-tag".
     *
     * @param tag the CBOR tag value to check
     * @return true if the tag is defined by CoRim
     */
    public static boolean isTlTag(final int tag) {
        if (tag == TAGGED_CONCISE_TL_TAG) {
            return true;
        }
        return false;
    }

    /**
     * Returns a human-readable label for a given CoRIM tag.
     *
     * @param coRimTag the integer value representing the CoRIM tag
     * @return a human-readable label describing the tag
     */
    public static String getTagLabel(final int coRimTag) {
        switch (coRimTag) {
            case TAGGED_CONCISE_SWID_TAG:
                return TAGGED_CONCISE_SWID_TAG_STR;
            case TAGGED_CONCISE_MID_TAG:
                return TAGGED_CONCISE_MID_TAG_STR;
            case TAGGED_CONCISE_TL_TAG:
                return TAGGED_CONCISE_TL_TAG_STR;
            default:
                if (coRimTag >= TAGGED_CORIM && coRimTag <= TAGGED_MASK_RAW_VALUE) {
                    return "tag reserved for CoRim (" + coRimTag + ")";
                }
                return "unknown corim tag (" + coRimTag + ")";
        }
    }

    /**
     * Returns a copy of the dependent RIMs map with cloned byte arrays.
     *
     * @return a defensive copy of the dependent RIMs
     */
    public Map<String, byte[]> getDependentRims() {
        Map<String, byte[]> copy = new HashMap<>();
        for (Map.Entry<String, byte[]> entry : dependentRims.entrySet()) {
            copy.put(entry.getKey(), entry.getValue().clone());
        }
        return copy;
    }

    /**
     * Sets the dependent RIMs using a defensive copy of the input map.
     *
     * @param newDependentRims the map to set, with values cloned to protect internal state
     */
    public void setDependentRims(final Map<String, byte[]> newDependentRims) {
        dependentRims = new HashMap<>();
        for (Map.Entry<String, byte[]> entry : newDependentRims.entrySet()) {
            dependentRims.put(entry.getKey(), entry.getValue().clone());
        }
    }
}
