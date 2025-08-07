package hirs.utils.rim.unsignedRim.cbor.tcgCompRimCoswid;

import com.fasterxml.jackson.databind.ObjectMapper;
import hirs.utils.rim.unsignedRim.cbor.ietfCoswid.CoswidConfig;
import hirs.utils.rim.unsignedRim.cbor.ietfCoswid.CoswidItems;
import lombok.Getter;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

/**
 * Class to read in a HIRS specific json based TCG Component RIM Configuration file.
 * This is intended to work specifically with the TcgComponentRim class
 */
public class TcgCompRimCoswidConfig extends CoswidConfig {
    //JsonNode rootNode = null;
    // Attributes from the TCG Component RIM Binding foe SWID and COswid specification
    //  Not found in the Coswid or Swid specifications
    // Link attributes (rename of existing attributes)
    @Getter
    private String bindingSpec = null;
    @Getter
    private String bindingSpecVersion = null;
    @Getter
    private String payloadType = null;
    @Getter
    private String persistentId = null;
    @Getter
    private String componentManufacturerStr = null;
    @Getter
    private String componentManufacturerID = null;
    @Getter
    private String componentLocator = null;
    @Getter
    private String firmwareVersion = null;
    @Getter
    private String supportRimType = null;
    @Getter
    private String supportRimFormat = null;
    @Getter
    private String supportRimUriGlobal = null;
    @Getter
    private String spdmMeasurementBlock = null;
    @Getter
    private String spdmVersion = null;
    @Getter
    private String spdmMeasurementBlockIndex = null;
    @Getter
    private String spdmMeasurementSpec = null;
    @Getter
    private String spdmMeasurementValueType = null;
    @Getter
    private String spdmMeasurementHash = null;
    @Getter
    private String spdmMeasurementRawData = null;

    /**
     * Constructor for the TCG Component Rim Coswid Config.
     * @param filename TcgComponentRimConfig config created from a json file.
     */
    public TcgCompRimCoswidConfig(final String filename) throws IOException {
        super();
        try {
            String errMsg = "";
            // Create an ObjectMapper instance
            ObjectMapper mapper = new ObjectMapper();
            // Read the JSON file
            File jsonFile = new File(filename);
            byte[] data = Files.readAllBytes(jsonFile.toPath());
            //ObjectMapper mapper = mapper.readValue(new ByteArrayInputStream(data), Map.class);
            Map<String, Object> parsedData = mapper.readValue(new ByteArrayInputStream(data), Map.class);
            //System.out.println(parsedData);
            // parse the data
            rootNode = mapper.readTree(data);
            // Check if config file is valid
            TcgCompRimCoswidValidator configValidator = new TcgCompRimCoswidValidator();
            if (!configValidator.isValid(rootNode)) {
                errMsg = "Config file has issues:";
                errMsg += "There are " + configValidator.getInvalidFieldCount() + " invalid field(s) : "
                        + configValidator.getInvalidFields();
                throw new IOException(errMsg);
            }

            // Set Coswid and Swid defined Variables
            super.init(rootNode);

            // Set TCG Component RIM specific variables
            bindingSpec = rootNode.path(CoswidItems.SOFTWARE_META_STR)
                    .path(TcgCompRimCoswid.CRIM_BINDING_SPEC_STR).asText();
            // (bindingSpecVersion not in the RIM Component spec)
            bindingSpecVersion = rootNode.path(CoswidItems.SOFTWARE_META_STR)
                    .path(TcgCompRimCoswid.CRIM_BINDING_SPEC_VERSION_STR).asText();
            payloadType = rootNode.path(CoswidItems.SOFTWARE_META_STR)
                    .path(TcgCompRimCoswid.CRIM_PAYLOAD_TYPE_STR).asText();
            persistentId = rootNode.path(CoswidItems.SOFTWARE_META_STR)
                    .path(CoswidItems.PERSISTENT_ID_STR).asText();
            componentManufacturerStr = rootNode.path(CoswidItems.SOFTWARE_META_STR)
                    .path(TcgCompRimCoswid.CRIM_COMPONENT_MANUFACTURER_STR).asText();
            componentManufacturerID = rootNode.path(CoswidItems.SOFTWARE_META_STR)
                    .path(TcgCompRimCoswid.CRIM_COMPONENT_MANUFACTURER_ID_STR).asText();
            // Payload attributes
            supportRimType = rootNode.path(CoswidItems.PAYLOAD_STR)
                    .path(TcgCompRimCoswid.CRIM_SUPPORT_RIM_TYPE_STR).asText();
            supportRimFormat = rootNode.path(CoswidItems.PAYLOAD_STR)
                    .path(TcgCompRimCoswid.CRIM_SUPPORT_RIM_FORMAT_STR).asText();
            supportRimUriGlobal = rootNode.path(CoswidItems.PAYLOAD_STR)
                    .path(TcgCompRimCoswid.CRIM_SUPPORT_RIM_URI_GLOBAL_STR).asText();
            // SPDM attributes
            spdmMeasurementBlock = rootNode.path(CoswidItems.PAYLOAD_STR)
                    .path(TcgCompRimCoswid.CRIM_SPDM_MEASUREMENT_BLOCK_STR).asText();
            spdmVersion = rootNode.path(CoswidItems.PAYLOAD_STR)
                    .path(TcgCompRimCoswid.CRIM_SPDM_VERSION_STR).asText();
            spdmMeasurementHash = rootNode.path(CoswidItems.PAYLOAD_STR)
                    .path(TcgCompRimCoswid.CRIM_SPDM_MEASUREMENT_HASH_STR).asText();
            spdmMeasurementBlockIndex = rootNode.path(CoswidItems.PAYLOAD_STR)
                    .path(TcgCompRimCoswid.CRIM_SPDM_MEASUREMENT_BLOCK_STR).asText();
            spdmMeasurementSpec = rootNode.path(CoswidItems.PAYLOAD_STR)
                    .path(TcgCompRimCoswid.CRIM_SPDM_MEASUREMENT_STR).asText();
            spdmMeasurementValueType = rootNode.path(CoswidItems.PAYLOAD_STR)
                    .path(TcgCompRimCoswid.CRIM_SPDM_MEASUREMENT_VALUE_TYPE_STR).asText();
            spdmMeasurementHash = rootNode.path(CoswidItems.PAYLOAD_STR)
                    .path(TcgCompRimCoswid.CRIM_SPDM_MEASUREMENT_HASH_STR).asText();
            spdmMeasurementRawData = rootNode.path(CoswidItems.PAYLOAD_STR)
                    .path(TcgCompRimCoswid.CRIM_SPDM_MEASUREMENT_RAW_DATA_STR).asText();
        } catch (Exception e) {
            throw new RuntimeException("Error processing TCG Component RIM configuration file "
                    + filename + ": "  + e.getMessage(), e);
        }
    }
}
