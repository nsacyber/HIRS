package hirs.utils.rim.unsignedRim.cbor.ietfCoswid;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Validation Class for the json encoded Coswid Configuration/Attribute file.
 */
@SuppressWarnings("VisibilityModifier")
public class  CoswidConfigValidator {
    protected JsonNode configRootNode = null;
    protected Coswid coswidRef = new Coswid();
    protected CoswidItems coswidItems = new CoswidItems();
    protected boolean isValid = true;
    @Getter
    protected String invalidFields = "";
    @Getter
    protected int invalidFieldCount = 0;

    /**
     * Default constructor for CoswidConfigValidator.
     */
    public CoswidConfigValidator() {
    }
    /**
     * Checks the json data for valid rfc-9393 defined item names.
     * @param rootNode node containing the configuration data to check
     * @return true if valid
     */
    public boolean isValid(final JsonNode rootNode) {
        configRootNode = rootNode;
        boolean validity = true;
        List<String> keys = new ArrayList<>();
        getAllKeysUsingJsonNodeFields(rootNode, keys);
        for (String key : keys) {
            if (!isValidKey(key)) {
                validity = false;
            }
        }
        return validity;
    }
    /**
     * Checks a single entry against a set of rfc 9393 define item names.
     * @param key specific item to check
     * @return true if valid
     */
    protected boolean isValidKey(final String key) {
        int index = coswidItems.getIndex(key);
        boolean validity = true;
        if (index == CoswidItems.UNKNOWN_INT) {
            validity = false;
            invalidFields += key + " ";
            invalidFieldCount++;
        }
        return validity;
    }
    /**
     * Converts a set of keys into a List.
     * @param jsonNode data to pull a list from
     * @param keys List to populate
     */
    private void getAllKeysUsingJsonNodeFields(final JsonNode jsonNode, final List<String> keys) {
        //List<String> keys = new ArrayList<>();
        if (jsonNode.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = jsonNode.fields();
            fields.forEachRemaining(field -> {
                keys.add(field.getKey());
                getAllKeysUsingJsonNodeFields((JsonNode) field.getValue(), keys);
            });
        } else if (jsonNode.isArray()) {
            ArrayNode arrayField = (ArrayNode) jsonNode;
            arrayField.forEach(node -> {
                getAllKeysUsingJsonNodeFields(node, keys);
            });
        }
    }
}
