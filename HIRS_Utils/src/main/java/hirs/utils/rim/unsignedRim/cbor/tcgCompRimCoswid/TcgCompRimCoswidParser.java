package hirs.utils.rim.unsignedRim.cbor.tcgCompRimCoswid;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import hirs.utils.rim.unsignedRim.cbor.ietfCoswid.CoswidItems;
import hirs.utils.rim.unsignedRim.cbor.ietfCoswid.CoswidParser;
import hirs.utils.signature.cose.Cbor.CborTagProcessor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;

/**
 * Class for parsing a CBor encoded object that complies with the TCG Component Rim specification.
 * Processes TCG Defined Software-Meta attributes.
 */
public class TcgCompRimCoswidParser extends CoswidParser {

    /**
     * Holds the TCG Component RIM Coswid.
     */
    private TcgCompRimCoswid tcRim = new TcgCompRimCoswid();

    /**
     * Parses a TCG Component RIM Coswid (cbor data).
     * @param cborData the cbor bytes to parse
     * @throws IOException if an issue occur when updating cbor data
     */
    public TcgCompRimCoswidParser(final byte[] cborData) throws IOException {
        super();
        byte[] untaggedCbor = null;
        CborTagProcessor tparse = new CborTagProcessor(cborData);
        // Check for a Coswid tag and remove if found
        if (tparse.isCoswid()) {
            untaggedCbor = tparse.getContent();
        } else {
            untaggedCbor = cborData;
        }
        ObjectMapper mapper = new ObjectMapper(new CBORFactory());
        parsedData = mapper.readValue(new ByteArrayInputStream(untaggedCbor), Map.class);
        rootNode = mapper.readTree(untaggedCbor);
    }

    /**
     * Parses a byte array of a CBOR-encoded CoSWID object and populates associated member variables
     * belonging specifically to TCG Component RIM Coswid.
     * @param cborData the cbor byte string to process
     */
    protected void initTcgRimCoswidParser(final byte[] cborData) {
        tcRim.setCrimBindingSpec(rootNode.path(Integer.toString(CoswidItems.SOFTWARE_META_INT))
                .path(Integer.toString(tcRim.CRIM_BINDING_SPEC_INT)).asText());
        tcRim.setCrimBindingSpecVersion(rootNode.path(Integer.toString(CoswidItems.SOFTWARE_META_INT))
                .path(Integer.toString(tcRim.CRIM_BINDING_SPEC_VERSION_INT)).asText());
        tcRim.setCrimPayloadType(rootNode.path(Integer.toString(CoswidItems.SOFTWARE_META_INT))
                .path(Integer.toString(tcRim.CRIM_PAYLOAD_TYPE_INT)).asText());
        tcRim.setCrimComponentManufacturer(rootNode.path(Integer.toString(CoswidItems.SOFTWARE_META_INT))
                .path(Integer.toString(tcRim.CRIM_COMPONENT_MANUFACTURER_INT)).asText());
        tcRim.setCrimComponentManufacturerID(rootNode.path(Integer.toString(CoswidItems.SOFTWARE_META_INT))
                .path(Integer.toString(tcRim.CRIM_COMPONENT_MANUFACTURER_ID_INT)).asText());
    }

}
