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
@Getter
public class TcgCompRimCoswidConfig extends CoswidConfig {
    private final String componentLocator = null;
    private final String firmwareVersion = null;
    /*
     * Attributes from the TCG Component RIM Binding for SWID and CoSWID specification.
     * Not found in the CoSWID or SWID specifications.
     * Link attributes (rename of existing attributes).
     */
    private String bindingSpec = null;
    private String bindingSpecVersion = null;
    private String payloadType = null;
    private String persistentId = null;
    private String componentManufacturerStr = null;
    private String componentManufacturerID = null;
    private String supportRimType = null;
    private String supportRimFormat = null;
    private String supportRimUriGlobal = null;
    private String spdmMeasurementBlock = null;
    private String spdmVersion = null;
    private String spdmMeasurementBlockIndex = null;
    private String spdmMeasurementSpec = null;
    private String spdmMeasurementValueType = null;
    private String spdmMeasurementHash = null;
    private String spdmMeasurementRawData = null;

    /**
     * Constructor for the TCG Component Rim Coswid Config.
     *
     * @param filename TcgComponentRimConfig config created from a json file.
     */
    public TcgCompRimCoswidConfig(final String filename) {
        super();
        try {
            String errMsg = "";
            // Create an ObjectMapper instance
            ObjectMapper mapper = new ObjectMapper();
            // Read the JSON file
            File jsonFile = new File(filename);
            byte[] data = Files.readAllBytes(jsonFile.toPath());
            Map<String, Object> parsedData = mapper.readValue(new ByteArrayInputStream(data), Map.class);
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
                    + filename + ": " + e.getMessage(), e);
        }
    }
}
