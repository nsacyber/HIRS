package hirs.attestationca.persist.entity.userdefined.certificate.attributes;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonObject.Member;
import hirs.utils.JsonUtils;
import lombok.Getter;

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
 *
 * A note for the future.
 */
public class ComponentClass {
    private static final String TCG_COMPONENT_REGISTRY = "2.23.133.18.3.1";
    private static final String SMBIOS_COMPONENT_REGISTRY = "2.23.133.18.3.3";
    private static final Path WINDOWS_JSON_PATH = FileSystems.getDefault().getPath(
                "C:/", "ProgramData", "hirs", "aca", "default-properties", "component-class.json");
    private static final Path JSON_PATH = WINDOWS_JSON_PATH.toFile().exists() ? WINDOWS_JSON_PATH :
            FileSystems.getDefault().getPath(
                "/etc", "hirs", "aca", "default-properties", "component-class.json");

    private static final String OTHER_STRING = "Other";
    private static final String UNKNOWN_STRING = "Unknown";
    private static final String NONE_STRING = "None";

    // Used to indicate that the component string value provided is erroneous
    private static final String ERROR = "-1";
    private static final int MID_INDEX = 4;
    /**
     * All TCG categories have Other and Unknown as the first 2 values.
     */
    private static final String OTHER = "0000";
    private static final String UNKNOWN = "0001";

    @Getter
    private String category, categoryStr;
    @Getter
    private String component, componentStr;
    private String registryType;
    @Getter
    private String componentIdentifier;

    /**
     * Default class constructor.
     */
    public ComponentClass() {
        this("TCG", JSON_PATH, UNKNOWN);
    }

    /**
     * Class Constructor that takes a String representation of the component
     * value.
     *
     * @param registryOid the decimal notation for the type of registry
     * @param componentIdentifier component value
     */
    public ComponentClass(final String registryOid, final String componentIdentifier) {
        this(registryOid, JSON_PATH, componentIdentifier);
    }

    /**
     * Class Constructor that takes a String representation of the component
     * value.
     *
     * @param componentClassPath file path for the json
     * @param componentIdentifier component value
     */
    public ComponentClass(final Path componentClassPath, final String componentIdentifier) {
        this(TCG_COMPONENT_REGISTRY, componentClassPath, componentIdentifier);
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
                          final String componentIdentifier) {
        this.category = OTHER;
        this.component = NONE_STRING;
        if (componentIdentifier == null || componentIdentifier.isEmpty()) {
            this.componentIdentifier = "";
        } else {
            this.componentIdentifier = verifyComponentValue(componentIdentifier);
        }

        switch (registryOid) {
            case TCG_COMPONENT_REGISTRY -> registryType = "TCG";
            case SMBIOS_COMPONENT_REGISTRY -> registryType = "SMBIOS";
            default -> registryType = UNKNOWN_STRING;
        }

        switch (this.componentIdentifier) {
            case OTHER:
                this.categoryStr = NONE_STRING;
                this.component = OTHER;
                this.componentStr = OTHER_STRING;
                break;
            case UNKNOWN:
            case "":
                this.categoryStr = NONE_STRING;
                this.component = UNKNOWN;
                this.componentStr = UNKNOWN_STRING;
                break;
            case ERROR:
                // Number Format Exception
                break;
            default:
                this.category = this.componentIdentifier.substring(0, MID_INDEX) + this.category;
                this.component = OTHER + this.componentIdentifier.substring(MID_INDEX);
                findStringValues(JsonUtils.getSpecificJsonObject(componentClassPath, registryType));
                break;
        }
    }

    /**
     * This is the main way this class will be referenced and how it
     * will be displayed on the portal.
     * @return String combination of category and component.
     */
    @Override
    public String toString() {
        String resultString;
        if (componentStr.equals(UNKNOWN_STRING) || component.equals(OTHER_STRING)) {
            resultString = String.format("%s%n%s", registryType, categoryStr);
        } else {
            resultString = String.format("%s%n%s - %s", registryType, categoryStr, componentStr);
        }
        return resultString;
    }

    /**
     * Getter for the Category mapped to the associated value in.
     *
     * @param categories a JSON object associated with mapped categories in file
     * {}@link componentIdentifier}.
     */
    private void findStringValues(final JsonObject categories) {
        String categoryID;
        String componentMask;
        boolean found = false;

        if (categories != null) {
            for (String name : categories.names()) {
                categoryID = verifyComponentValue(categories.get(name)
                        .asObject().get("ID").asString());
                componentMask = componentIdentifier.substring(MID_INDEX);
                // check for the correct flag
                if (categoryMatch(componentIdentifier.substring(0, MID_INDEX),
                        categoryID.substring(0, MID_INDEX))) {
                    found = true;
                    JsonObject componentTypes = categories.get(name)
                            .asObject().get("Types").asObject();
                    categoryStr = name;

                    switch (componentMask) {
                        case OTHER -> componentStr = OTHER_STRING;
                        case UNKNOWN -> componentStr = UNKNOWN_STRING;
                        default -> getComponent(componentTypes);
                    }
                }
            }
        }

        if (!found) {
            this.categoryStr = NONE_STRING;
            this.componentStr = UNKNOWN_STRING;
        }
    }

    /**
     * Returns the value of the comparison between a category and the what's in the id.
     * @param category the category to compare
     * @param componentId the id value to compare
     * @return true if they match
     */
    public boolean categoryMatch(final String category, final String componentId) {
        return category.equals(componentId);
    }

    /**
     * Getter for the component associated with the component JSON Object mapped
     * in the JSON file.
     *
     * @param components JSON Object for the categories components
     */
    private void getComponent(final JsonObject components) {
        String typeID;

        if (components != null) {
            for (Member member : components) {
                typeID = verifyComponentValue(member.getName());

                if (component.equalsIgnoreCase(typeID)) {
                    componentStr = member.getValue().asString();
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
    private static String verifyComponentValue(final String component) {
        String componentValue = ERROR;

        if (component != null) {
            try {
                if (component.contains("x")) {
                    componentValue = component.substring(component.indexOf("x") + 1);
                } else {
                    if (component.contains("#")) {
                        componentValue = component.replace("#", "");
                    } else {
                        return component;
                    }
                }
            } catch (NumberFormatException nfEx) {
                //invalid entry
            }
        }

        return componentValue;
    }
}
