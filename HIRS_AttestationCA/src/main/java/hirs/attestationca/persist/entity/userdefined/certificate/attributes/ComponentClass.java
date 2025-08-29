package hirs.attestationca.persist.entity.userdefined.certificate.attributes;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonObject.Member;
import hirs.utils.JsonUtils;
import hirs.utils.PciIds;
import lombok.Getter;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.List;

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
 * <p>
 * A note for the future.
 */
@Getter
public class
ComponentClass {
    private static final String TCG_COMPONENT_REGISTRY = "2.23.133.18.3.1";

    private static final String SMBIOS_COMPONENT_REGISTRY = "2.23.133.18.3.3";

    private static final String PCIE_BASED_COMPONENT_REGISTRY = "2.23.133.18.3.4";

    private static final String STORAGE_COMPONENT_REGISTRY = "2.23.133.18.3.5";

    private static final Path WINDOWS_JSON_PATH = FileSystems.getDefault().getPath(
            "C:/", "ProgramData", "hirs", "aca", "default-properties", "component-class.json");

    private static final Path JSON_PATH = WINDOWS_JSON_PATH.toFile().exists() ? WINDOWS_JSON_PATH
            : FileSystems.getDefault().getPath(
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

    private final String registryType;
    private final String registryOid;
    private final String componentIdentifier;
    private String category;
    private String categoryStr;
    private String component;
    private String componentStr;

    /**
     * This field will contain the component class complete information that will be displayed as
     * a tooltip for each component. At the moment it will be used to display each PCIE component's
     * complete information.
     */
    private String componentToolTipStr;

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
     * @param registryOid         the decimal notation for the type of registry
     * @param componentIdentifier component value
     */
    public ComponentClass(final String registryOid, final String componentIdentifier) {
        this(registryOid, JSON_PATH, componentIdentifier);
    }

    /**
     * Class Constructor that takes a String representation of the component
     * value.
     *
     * @param componentClassPath  file path for the json
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
     * @param registryOid         the decimal notation for the type of registry
     * @param componentClassPath  file path for the json
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

        this.registryOid = registryOid;

        this.registryType = switch (registryOid) {
            case TCG_COMPONENT_REGISTRY -> "TCG";
            case SMBIOS_COMPONENT_REGISTRY -> "SMBIOS";
            case PCIE_BASED_COMPONENT_REGISTRY -> "PCIE";
            case STORAGE_COMPONENT_REGISTRY -> "STORAGE";
            default -> UNKNOWN_STRING;
        };

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

                // if the registry type is of type PCIE, attempt to use the included library
                // to parse the string values
                if (this.registryType.equals("PCIE")) {
                    this.findComponentValuesForPCIERegistry();
                } else {
                    this.findComponentValuesForAllOtherRegistryTypes(
                            JsonUtils.getSpecificJsonObject(componentClassPath, registryType));
                }
                break;
        }
    }

    /**
     * This method converts the string representation of the component ID into
     * an integer. Or throws and error if the format is in error.
     *
     * @param component string representation of the component ID
     * @return the int representation of the component
     */
    private String verifyComponentValue(final String component) {
        String componentValue = ERROR;

        if (component != null) {
            try {
                if (component.contains("x")) {
                    componentValue = component.substring(component.indexOf("x") + 1);
                } else if (component.contains("#")) {
                    componentValue = component.replace("#", "");
                } else {
                    return component;
                }
            } catch (NumberFormatException nfEx) {
                //invalid entry
            }
        }

        return componentValue;
    }

    /**
     * This is the main way this class will be referenced and how it
     * will be displayed on the portal.
     *
     * @return String combination of category and component.
     */
    @Override
    public String toString() {
        if (componentStr.equals(UNKNOWN_STRING) || component.equals(OTHER_STRING)) {
            return String.format("%s%n%s", registryType, categoryStr);
        }
        return String.format("%s%n%s - %s", registryType, categoryStr, componentStr);
    }

    /**
     * Helper method that attempts to find and set the category and component string using the PCI IDs
     * library. This method will be used only for the PCIE registry types.
     */
    private void findComponentValuesForPCIERegistry() {
        if (PciIds.DB.isReady()) {
            // remove the first two digits from the component value
            final String classCode = this.componentIdentifier.substring(2);
            final List<String> translateClassCode = PciIds.translateDeviceClass(classCode);

            // grab the component's device class from the first element
            // and if the PCI Ids DB did not return a number, set the category string to the
            // translated device class
            this.categoryStr = translateClassCode.get(0).matches("\\d+")
                    ? UNKNOWN_STRING : translateClassCode.get(0);

            // grab the component's device subclass from the second element
            // and if the PCI Ids DB did not return a number, set the component string to the
            // translated device subclass
            this.componentStr = translateClassCode.get(1).matches("\\d+")
                    ? NONE_STRING : translateClassCode.get(1);

            // grab the component's programming interface from the third element
            // and if the PCI Ids DB did not return a number, return an empty string
            final String programmingInterface =
                    translateClassCode.get(2).matches("\\d+") ? "" : translateClassCode.get(2);

            // create a string that represents the component's complete information that can be
            // displayed as tooltip (currently this is being used for just PCIE components)
            this.componentToolTipStr = "Class: " + this.categoryStr + " | "
                    + "\nSubclass: " + this.componentStr + " | "
                    + "\nProgramming Interface: " + programmingInterface;
        }
    }

    /**
     * Helper method that attempts to find and set the category and component string using the provided
     * JSON object. This method will typically be used for the SMBIOS, STORAGE-BASED, and TCG registry types.
     *
     * @param categories a JSON object associated with mapped categories in file.
     */
    private void findComponentValuesForAllOtherRegistryTypes(final JsonObject categories) {
        String categoryID;
        String componentMask;
        boolean found = false;

        if (categories != null) {
            for (String name : categories.names()) {
                categoryID = verifyComponentValue(categories.get(name)
                        .asObject().get("ID").asString());
                componentMask = componentIdentifier.substring(MID_INDEX);
                // check for the correct flag
                if (componentIdentifier.substring(0, MID_INDEX).equals(categoryID.substring(0, MID_INDEX))) {
                    found = true;
                    JsonObject componentTypes = categories.get(name)
                            .asObject().get("Types").asObject();
                    this.categoryStr = name;

                    if (componentMask.equals(OTHER)) {
                        this.componentStr = OTHER_STRING;
                    } else if (componentMask.equals(UNKNOWN)) {
                        this.componentStr = UNKNOWN_STRING;
                    } else {
                        setComponentString(componentTypes);
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
     * Sets the component string value based on the provided JSON object's components.
     *
     * @param components JSON Object components
     */
    private void setComponentString(final JsonObject components) {
        String typeID;

        if (components != null) {
            for (Member member : components) {
                typeID = verifyComponentValue(member.getName());

                if (component.equalsIgnoreCase(typeID)) {
                    componentStr = member.getValue().asString();
                }
            }
        }

        // if the component string is still null after doing a lookup
        if (componentStr == null) {
            componentStr = UNKNOWN_STRING;
        }
    }
}
