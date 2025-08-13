package hirs.utils.rim.unsignedRim.cbor.ietfCoswid;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NoArgsConstructor;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.io.IOException;
import java.util.Map;

/**
 * Class to read in a HIRS specific json based RIM Configuration file.
 */
@NoArgsConstructor
public class CoswidConfig extends Coswid {
    protected JsonNode rootNode = null;
    /**
     * Constructor that takes in a filename and runs a validation on the config file.
     * @param filename name of the Json formatted configration file.
     * @throws IOException  if parsing errrors are encountered.
     */
    public CoswidConfig(final String filename) throws IOException {
        try {
            String errMsg = "";
            // Create an ObjectMapper instance
            ObjectMapper mapper = new ObjectMapper();
            // Read the JSON file
            File jsonFile = new File(filename);
            byte[] data = Files.readAllBytes(jsonFile.toPath());
            Map<String, Object> parsedData = mapper.readValue(new ByteArrayInputStream(data), Map.class);
            rootNode = mapper.readTree(data);
            CoswidConfigValidator configValidator = new CoswidConfigValidator();
            if (!configValidator.isValid(rootNode)) {
                errMsg = "Config file has issues:";
                errMsg += "There are " + configValidator.getInvalidFieldCount() + " invalid field(s) : "
                        + configValidator.getInvalidFields();
                throw new RuntimeException(errMsg);
            }
            init(rootNode);
        } catch (IOException e) {
            throw new RuntimeException("Error processing Coswid RIM configuration file named "
                    + filename + ": " + e.getMessage(), e);
        }
    }

    /**
     * Initializes Class variables based upon a JsonNode object.
     * Used by inherited classes to fill in Coswid variables from a json node
     * @param initNode
     */
    protected void init(final JsonNode initNode) {
        lang = rootNode.path(CoswidItems.LANG_STR).asText();
        softwareName = rootNode.path(CoswidItems.SOFTWARE_NAME_STR).asText();
        softwareVersion = rootNode.path(CoswidItems.SOFTWARE_VERSION_STR).asText();
        tagId = rootNode.path(CoswidItems.TAG_ID_STR).asText();
        tagVersion = rootNode.path(CoswidItems.TAG_VERSION_STR).asText();
        patch = rootNode.path(CoswidItems.PATCH_STR).asBoolean();
        supplemental = rootNode.path(CoswidItems.SUPPLEMENTAL_STR).asBoolean();
        corpus = rootNode.path(CoswidItems.CORPUS_STR).asBoolean();
        entityName = rootNode.path(CoswidItems.ENTITY_STR).path(CoswidItems.ENTITY_NAME_STR).asText();
        regId = rootNode.path(CoswidItems.ENTITY_STR).path(CoswidItems.REG_ID_STR).asText();
        role = rootNode.path(CoswidItems.ENTITY_STR).path(CoswidItems.ROLE_STR).asText();
        thumbprint = rootNode.path(CoswidItems.ENTITY_STR).path(CoswidItems.THUMBPRINT_STR).asText();
        // Link fields
        href = rootNode.path(CoswidItems.LINK_STR).path(CoswidItems.HREF_STR).asText();
        rel = rootNode.path(CoswidItems.LINK_STR).path(CoswidItems.REL_STR).asText();
        // Meta fields
        colloquialVersion = rootNode.path(CoswidItems.SOFTWARE_META_STR)
                .path(CoswidItems.COLLOQUIAL_VERSION_STR).asText();
        edition = rootNode.path(CoswidItems.SOFTWARE_META_STR).path(CoswidItems.EDITION_STR).asText();
        revision = rootNode.path(CoswidItems.SOFTWARE_META_STR).path(CoswidItems.REVISION_STR).asText();
        product = rootNode.path(CoswidItems.SOFTWARE_META_STR).path(CoswidItems.PRODUCT_STR).asText();
        // Coswid defined attributes
        description = rootNode.path(CoswidItems.SOFTWARE_META_STR)
                .path(CoswidItems.DESCRIPTION_STR).asText();
        persistentId = rootNode.path(CoswidItems.SOFTWARE_META_STR)
                .path(CoswidItems.PERSISTENT_ID_STR).asText();
        productFamily = rootNode.path(CoswidItems.SOFTWARE_META_STR)
                .path(CoswidItems.PRODUCT_FAMILY_STR).asText();
        summary = rootNode.path(CoswidItems.SOFTWARE_META_STR).path(CoswidItems.SUMMARY_STR).asText();
        // Payload is a complex structure with a variable number of entries
        payloadNode = rootNode.path(CoswidItems.PAYLOAD_STR);
    }
}
