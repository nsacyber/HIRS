package hirs.data.persist.certificate.attributes;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonObject.Member;
import hirs.utils.JsonUtils;

import java.nio.file.FileSystems;
import java.nio.file.Path;

/**
 * <p>
 * This class parses the associated component identifier located in Platform
 * Certificates and maps them to the corresponding string representation found
 * in the associated JSON file. If the value can not be found, either because
 * the provided value is malformed or doesn't exist in the mapping, then values
 * returned will not match what is expected. This class will return Unknown as a
 * category and None as the component which is not a valid mapping. This is
 * because None is a category and Unknown is a component identifier.
 * </p>
 * <pre>
 *   componentClass ::= SEQUENCE {
 *       componentClassRegistry ComponentClassRegistry,
 *       componentClassValue OCTET STRING SIZE(4) ) }
 * </pre>
 */
public class ComponentClass {
    private static final String TCG_COMPONENT_REGISTRY = "2.23.133.18.3.1";
    private static final String SMBIOS_COMPONENT_REGISTRY = "2.23.133.18.3.3";

    private static final Path JSON_PATH = FileSystems.getDefault()
            .getPath("/opt", "hirs", "default-properties", "component-class.json");
    private static final String OTHER_STRING = "Other";
    private static final String UNKNOWN_STRING = "Unknown";
    private static final String NONE_STRING = "None";

    // Used to test bytes associated with just the component
    private static final int COMPONENT_MASK = 0x0000FFFF;
    // Used to test bytes associated with just the category
    private static final int CATEGORY_MASK = 0xFFFF0000;

    // Used to indicate that the component string value provided is erroneous
    private static final int ERROR = -1;
    /**
     * All categories have Other and Unknown as the first 2 values.
     */
    private static final int OTHER = 0;
    private static final int UNKNOWN = 1;

    private String category;
    private String component;
    private String registryType;
    private int componentIdentifier;
    private String classValueString;

    /**
     * Default class constructor.
     */
    public ComponentClass() {
        this("TCG", JSON_PATH, UNKNOWN);
    }

    /**
     * Class Constructor that takes a int representation of the component value.
     *
     * @param componentIdentifier component value
     */
    public ComponentClass(final int componentIdentifier) {
        this(TCG_COMPONENT_REGISTRY, JSON_PATH, componentIdentifier);
    }

    /**
     * Class Constructor that takes a String representation of the component
     * value.
     *
     * @param registryOid the decimal notation for the type of registry
     * @param componentIdentifier component value
     */
    public ComponentClass(final String registryOid, final String componentIdentifier) {
        this(registryOid, JSON_PATH, getComponentIntValue(componentIdentifier));
    }

    /**
     * Class Constructor that takes a String representation of the component
     * value.
     *
     * @param registryOid the decimal notation for the type of registry
     * @param componentClassPath file path for the json
     * @param componentIdentifier component value
     */
    public ComponentClass(final String registryOid,
                          final Path componentClassPath,
                          final String componentIdentifier) {
        this(registryOid, componentClassPath, getComponentIntValue(componentIdentifier));
    }

    /**
     * Class Constructor that takes a String representation of the component
     * value.
     *
     * @param componentClassPath file path for the json
     * @param componentIdentifier component value
     */
    public ComponentClass(final Path componentClassPath, final String componentIdentifier) {
        this(TCG_COMPONENT_REGISTRY, componentClassPath, getComponentIntValue(componentIdentifier));
        if (componentIdentifier != null && componentIdentifier.contains("#")) {
            this.classValueString = componentIdentifier.replaceAll("#", "");
        } else {
            this.classValueString = componentIdentifier;
        }
    }

    /**
     * Main Class Constructor that takes in an integer representation of the
     * component value. Sets main class variables to default values and then
     * matches the value against defined values in the associated JSON file.
     *
     * @param registryOid the decimal notation for the type of registry
     * @param componentClassPath file path for the json
     * @param componentIdentifier component value
     */
    public ComponentClass(final String registryOid,
                          final Path componentClassPath,
                          final int componentIdentifier) {
        this.category = UNKNOWN_STRING;
        this.component = NONE_STRING;
        this.componentIdentifier = componentIdentifier;

        switch (registryOid) {
            case TCG_COMPONENT_REGISTRY:
                registryType = "TCG";
                break;
            case SMBIOS_COMPONENT_REGISTRY:
                registryType = "SMBIOS";
                break;
            default:
                registryType = UNKNOWN_STRING;
        }

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
                getCategory(JsonUtils.getSpecificJsonObject(componentClassPath, registryType));
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
     * Getter for the Component Class Value.
     * @return int value of the component class.
     */
    public final int getValue() {
        return componentIdentifier;
    }

    /**
     * Getter for the Component Class Value as a string.
     * @return String representation of the class.
     */
    public final String getClassValueString() {
        return classValueString;
    }

    /**
     * This is the main way this class will be referenced and how it
     * will be displayed on the portal.
     * @return String combination of category and component.
     */
    @Override
    public String toString() {
        return String.format("%s%n%s - %s", registryType, category, component);
    }

    /**
     * Getter for the Category mapped to the associated value in.
     *
     * @param categories a JSON object associated with mapped categories in file
     * {}@link componentIdentifier}.
     */
    private void getCategory(final JsonObject categories) {
        int componentID;

        if (categories != null) {
            for (String name : categories.names()) {
                componentID = Integer.decode(categories.get(name).asObject().get("ID").asString());
                // check for the correct flag
                if ((componentIdentifier & CATEGORY_MASK) == componentID) {
                    JsonObject componentTypes = categories.get(name)
                            .asObject().get("Types").asObject();
                    category = name;

                    switch (componentIdentifier & COMPONENT_MASK) {
                        case OTHER:
                            component = OTHER_STRING;
                            break;
                        case UNKNOWN:
                            component = UNKNOWN_STRING;
                            break;
                        default:
                            getComponent(componentID, componentTypes);
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
     * @param components JSON Object for the categories components
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
     * This method converts the string representation of the component ID into
     * an integer. Or throws and error if the format is in error.
     *
     * @param component string representation of the component ID
     * @return the int representation of the component
     */
    private static int getComponentIntValue(final String component) {
        int componentValue = ERROR;

        if (component != null) {
            try {
                if (component.contains("x")) {
                    componentValue = Integer.decode(component);
                } else {
                    if (component.contains("#")) {
                        componentValue = Integer.valueOf(
                                component.replace("#", ""),
                                Short.SIZE);
                    } else {
                        componentValue = Integer.valueOf(
                                component, Short.SIZE);
                    }
                }
            } catch (NumberFormatException nfEx) {
                //invalid entry
            }
        }

        return componentValue;
    }
}
