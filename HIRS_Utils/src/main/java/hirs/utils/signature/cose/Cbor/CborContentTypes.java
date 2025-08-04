package hirs.utils.signature.cose.Cbor;

import hirs.utils.rim.unsignedRim.GenericRim;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * Implements multipart content-format CoAP types as specified in  RFC 7252, which are used for building
 * CBOR protected headers. These are defined in the IANA CoAP Content-Formats registry.
 */
@Getter
public enum CborContentTypes {
    /** Cose sign1 type. */
    COSE_SIGN1(18, "application/cose; cose-type=\"cose-sign1\""),
    /** Coswid type. */
    SWID_CBOR(258, "application/swid+cbor"),
    /** Corim type. */
    RIM_CBOR(9999, "application/rim+cbor");
    private final int contentId;
    private final String contentType;
    private static final Map<Integer, CborContentTypes> ID_MAP = new HashMap<>();

    static {
        for (CborContentTypes cType : values()) {
            ID_MAP.put(cType.getContentId(), cType);
        }
    }
    /**
     * Sets the cbor contents types.
     * @param contentId
     * @param contentType
     */
    CborContentTypes(final int contentId, final String contentType) {
        this.contentId = contentId;
        this.contentType = contentType;
    }
    /**
     * Searches the content-type array for a match to a content-type value.
     *
     * @param contentId id of content-type
     * @return the corresponding content-type from the IANA reference page
     */
    public static CborContentTypes getContentTypeFromId(final int contentId) {
        return ID_MAP.get(contentId);
    }
    /**
     * Matches a RIM type specified in {@link GenericRim} to a content-type value.
     *
     * @param rimType the type to match
     * @return the corresponding content-type from the IANA reference page
     */
    public static CborContentTypes getContentTypeFromRimType(final String rimType) {
        switch (rimType) {
            case GenericRim.RIMTYPE_COSWID, GenericRim.RIMTYPE_COMP_COSWID -> {
                return SWID_CBOR;
            }
            case GenericRim.RIMTYPE_CORIM_COMID, GenericRim.RIMTYPE_CORIM_COSWID -> {
                return RIM_CBOR;
            }
            default -> {
                return COSE_SIGN1;
            }
        }
    }
}
