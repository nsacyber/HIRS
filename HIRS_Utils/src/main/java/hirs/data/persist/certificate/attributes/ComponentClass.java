package hirs.data.persist.certificate.attributes;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonObject.Member;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * <p>
 * This class parses the associated component identifier located in Platform
 * Certificates and maps them to the corresponding string representation found
 * in the associated JSON file.  If the value can not be found, either because
 * the provided value is malformed or doesn't exist in the mapping, then values
 * returned will not match what is expected.  This class will return Unknown as
 * a category and None as the component which is not a valid mapping.  This is
 * because None is a category and Unknown is a component identifier.
 * </p>
 * <pre>
 *   componentClass ::= SEQUENCE {
 *       OCTET STRING (SIZE (1..CONFIGMAX)) }
 *   where CONFIGMAX is 32
 *  </pre>
 */
public class ComponentClass {

    private static final String JSON_FILE = "/home/cyrus/Documents/ComponentClass.json";
    private static final String OTHER_STRING = "Other";
    private static final String UNKNOWN_STRING = "Unknown";
    private static final String NONE_STRING = "None";

    // Used to test bytes associated with just the component
    private static final int COMPONENT_MASK = 0x0000FFFF;

    // Used to indicate that the component string value provided is erroneous
    private static final int ERROR = -1;
    /**
     * All categories have Other and Unknown as the first 2 values.
     */
    private static final int OTHER = 0;
    private static final int UNKNOWN = 1;

    private String category;
    private String component;
    private int componentIdentifier;

    /**
     * Class Constructor that takes a String representation of the component
     * value.
     *
     * @param componentIdentifier component value
     */
    public ComponentClass(final String componentIdentifier) {
        this(getComponentIntValue(componentIdentifier));
    }

    /**
     * Main Class Constructor that takes in an integer representation of the
     * component value.  Sets main class variables to default values and
     * then matches the value against defined values in the associated JSON
     * file.
     *
     * @param componentIdentifier component value
     */
    public ComponentClass(final int componentIdentifier) {
        this.category = UNKNOWN_STRING;
        this.component = NONE_STRING;
        this.componentIdentifier = componentIdentifier;

        switch (componentIdentifier) {
            case OTHER:
                this.category = NONE_STRING;
                this.component = OTHER_STRING;
                break;
            case UNKNOWN:
                this.category = NONE_STRING;
                this.component = UNKNOWN_STRING;
                break;
            case ERROR:
                // Number Format Exception
                break;
            default:
                getCategory(getJsonObject());
                break;
        }
    }

    /**
     * Getter for the Category type.
     *
     * @return string value of the category
     */
    public final String getCategory() {
        return category;
    }

    /**
     * Getter for the Component type.
     *
     * @return string value of the component
     */
    public final String getComponent() {
        return component;
    }

    /**
     * Getter for the JSON Object that is associated with the component value
     * mapped in the associated JSON file.
     *
     * @return a JSON Object
     */
    private JsonObject getJsonObject() {
        // find the file and load it
        JsonObject components;

        try {
            InputStream inputStream = new FileInputStream(JSON_FILE);
            JsonObject object = Json.parse(new InputStreamReader(inputStream, "UTF-8")).asObject();
            components = object.get("Components").asObject();
        } catch (IOException ex) {
            // add log file thing here indication issue with JSON File
            components = null;
        }

        return components;
    }

    /**
     * Getter for the Category mapped to the associated value in.
     *
     * @param categories a JSON object associated with mapped categories in file
     * @componentIndentifier.
     */
    private void getCategory(final JsonObject categories) {
        int componentID;

        if (categories != null) {
            for (String name : categories.names()) {
                componentID = Integer.decode(categories.get(name).asObject().get("ID").asString());
                // check for the correct flag
                if ((componentIdentifier & componentID) == componentID) {
                    JsonObject subObjects = categories.get(name).asObject().get("Types").asObject();
                    category = name;

                    switch (componentIdentifier & COMPONENT_MASK) {
                        case OTHER:
                            component = OTHER_STRING;
                            break;
                        case UNKNOWN:
                            component = UNKNOWN_STRING;
                            break;
                        default:
                            getComponent(componentID, subObjects);
                    }
                }
            }
        }
    }

    /**
     * Getter for the component associated with the component JSON Object mapped
     * in the JSON file.
     *
     * @param componentID the ID associated with the category
     * @param components  JSON Object for the categories components
     */
    private void getComponent(final int componentID, final JsonObject components) {
        int typeID, testID;

        if (components != null) {
            for (Member member : components) {
                typeID = Integer.decode(member.getName());
                testID = componentID + typeID;

                if (componentIdentifier == testID) {
                    component = member.getValue().asString();
                }
            }
        }

    }

    /**
     * This method converts the string representation of the component ID
     * into an integer.  Or throws and error if the format is in error.
     *
     * @param component string representation of the component ID
     * @return the int representation of the component
     */
    private static int getComponentIntValue(final String component) {
        int componentIdentifier = -1;

        if (component != null) {
            try {
                if (component.contains("x")) {
                    componentIdentifier = Integer.decode(component);
                } else {
                    componentIdentifier = Integer.valueOf(component, Short.SIZE);
                }
            } catch (NumberFormatException nfEx) {
                //invalid entry
            }
        }

        return componentIdentifier;
    }
}
