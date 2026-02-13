package hirs.utils.rim.unsignedRim.cbor.ietfCoswid;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Validation Class for the json encoded Coswid Configuration/Attribute file.
 */
@NoArgsConstructor
public class CoswidConfigValidator {
    protected JsonNode configRootNode = null;
    protected Coswid coswidRef = new Coswid();
    protected CoswidItems coswidItems = new CoswidItems();
    protected boolean isValid = true;
    @Getter
    protected String invalidFields = "";
    @Getter
    protected int invalidFieldCount = 0;

    /**
     * Checks the json data for valid rfc-9393 defined item names.
     *
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
     *
     * @param key specific item to check
     * @return true if valid
     */
    protected boolean isValidKey(final String key) {
        int index = CoswidItems.getIndex(key);
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
     *
     * @param jsonNode data to pull a list from
     * @param keys     List to populate
     */
    private void getAllKeysUsingJsonNodeFields(final JsonNode jsonNode, final List<String> keys) {
        if (jsonNode.isObject()) {
            Set<Map.Entry<String, JsonNode>> fields = jsonNode.properties();
            for (Map.Entry<String, JsonNode> field : fields) {
                keys.add(field.getKey());
                getAllKeysUsingJsonNodeFields(field.getValue(), keys);
            }
        } else if (jsonNode.isArray()) {
            ArrayNode arrayField = (ArrayNode) jsonNode;
            arrayField.forEach(node -> {
                getAllKeysUsingJsonNodeFields(node, keys);
            });
        }
    }
}
