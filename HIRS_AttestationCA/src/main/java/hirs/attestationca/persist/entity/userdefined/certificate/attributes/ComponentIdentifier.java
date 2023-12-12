package hirs.attestationca.persist.entity.userdefined.certificate.attributes;


import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.asn1.ASN1Boolean;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.ASN1UTF8String;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Basic class that handle component identifiers from the Platform Configuration
 * Attribute.
 * <pre>
 * ComponentIdentifier ::= SEQUENCE {
 *      componentManufacturer UTF8String (SIZE (1..STRMAX)),
 *      componentModel UTF8String (SIZE (1..STRMAX)),
 *      componentSerial[0] IMPLICIT UTF8String (SIZE (1..STRMAX)) OPTIONAL,
 *      componentRevision [1] IMPLICIT UTF8String (SIZE (1..STRMAX)) OPTIONAL,
 *      componentManufacturerId [2] IMPLICIT PrivateEnterpriseNumber OPTIONAL,
 *      fieldReplaceable [3] IMPLICIT BOOLEAN OPTIONAL,
 *      componentAddress [4] IMPLICIT
 *          SEQUENCE(SIZE(1..CONFIGMAX)) OF ComponentAddress OPTIONAL}
 * where STRMAX is 256, CONFIGMAX is 32
 * </pre>
 */
@Getter
@Setter
@AllArgsConstructor
@EqualsAndHashCode
public class ComponentIdentifier {

    /**
     * Variable for components that aren't set.
     */
    public static final String EMPTY_COMPONENT = "[Empty]";
    /**
     * Variable for components that aren't set.
     */
    public static final String NOT_SPECIFIED_COMPONENT = "Not Specified";
    /**
     * Maximum number of configurations.
     */
    public static final int CONFIGMAX = 32;

    private static final int MANDATORY_ELEMENTS = 2;
    // optional sequence objects
    /**
     * Static variable indicated array position for the serial number.
     */
    protected static final int COMPONENT_SERIAL = 0;
    /**
     * Static variable indicated array position for the revision info.
     */
    protected static final int COMPONENT_REVISION = 1;
    /**
     * Static variable indicated array position for the manufacturer id.
     */
    protected static final int COMPONENT_MANUFACTURER_ID = 2;
    /**
     * Static variable indicated array position for the field replaceable value.
     */
    protected static final int FIELD_REPLACEABLE = 3;
    /**
     * Static variable indicated array position for the component address.
     */
    protected static final int COMPONENT_ADDRESS = 4;

    private ASN1UTF8String componentManufacturer;
    private ASN1UTF8String componentModel;
    private ASN1UTF8String componentSerial;
    private ASN1UTF8String componentRevision;
    private ASN1ObjectIdentifier componentManufacturerId;
    private ASN1Boolean fieldReplaceable;
    private List<ComponentAddress> componentAddress;
    private boolean validationResult = true;

    /**
     * Default constructor.
     */
    public ComponentIdentifier() {
        componentManufacturer = ASN1UTF8String.getInstance(NOT_SPECIFIED_COMPONENT);
        componentModel = ASN1UTF8String.getInstance(NOT_SPECIFIED_COMPONENT);
        componentSerial = ASN1UTF8String.getInstance(StringUtils.EMPTY);
        componentRevision = ASN1UTF8String.getInstance(StringUtils.EMPTY);
        componentManufacturerId = null;
        fieldReplaceable = null;
        componentAddress = new ArrayList<>();
    }

    /**
     * Constructor given the components values.
     *
     * @param componentManufacturer represents the component manufacturer
     * @param componentModel represents the component model
     * @param componentSerial  represents the component serial number
     * @param componentRevision represents the component revision
     * @param componentManufacturerId represents the component manufacturer ID
     * @param fieldReplaceable represents if the component is replaceable
     * @param componentAddress represents a list of addresses
     */
    public ComponentIdentifier(final ASN1UTF8String componentManufacturer,
                               final ASN1UTF8String componentModel,
                               final ASN1UTF8String componentSerial,
                               final ASN1UTF8String componentRevision,
                               final ASN1ObjectIdentifier componentManufacturerId,
                               final ASN1Boolean fieldReplaceable,
                               final List<ComponentAddress> componentAddress) {
        this.componentManufacturer = componentManufacturer;
        this.componentModel = componentModel;
        this.componentSerial = componentSerial;
        this.componentRevision = componentRevision;
        this.componentManufacturerId = componentManufacturerId;
        this.fieldReplaceable = fieldReplaceable;
        this.componentAddress = componentAddress;
    }

    /**
     * Constructor given the SEQUENCE that contains Component Identifier.
     * @param sequence containing the the component identifier
     * @throws IllegalArgumentException if there was an error on the parsing
     */
    public ComponentIdentifier(final ASN1Sequence sequence) throws IllegalArgumentException {
        // set all optional values to default in case they aren't set.
        this();
        //Check if it have a valid number of identifiers
        if (sequence.size() < MANDATORY_ELEMENTS) {
            throw new IllegalArgumentException("Component identifier do not have required values.");
        }

        //Mandatory values
        componentManufacturer = ASN1UTF8String.getInstance(sequence.getObjectAt(0));
        componentModel = ASN1UTF8String.getInstance(sequence.getObjectAt(1));

        //Continue reading the sequence if it does contain more than 2 values
        for (int i = 2; i < sequence.size(); i++) {
            ASN1TaggedObject taggedObj = ASN1TaggedObject.getInstance(sequence.getObjectAt(i));
            switch (taggedObj.getTagNo()) {
                case COMPONENT_SERIAL:
                    componentSerial = ASN1UTF8String.getInstance(taggedObj, false);
                    break;
                case COMPONENT_REVISION:
                    componentRevision = ASN1UTF8String.getInstance(taggedObj, false);
                    break;
                case COMPONENT_MANUFACTURER_ID:
                    componentManufacturerId = ASN1ObjectIdentifier.getInstance(taggedObj, false);
                    break;
                case FIELD_REPLACEABLE:
                    fieldReplaceable = ASN1Boolean.getInstance(taggedObj, false);
                    break;
                case COMPONENT_ADDRESS:
                    ASN1Sequence addressesSequence = ASN1Sequence.getInstance(taggedObj, false);
                    componentAddress = retrieveComponentAddress(addressesSequence);
                    break;
                default:
                    throw new IllegalArgumentException("Component identifier contains "
                            + "invalid tagged object.");
            }
        }
    }

    /**
     * Get all the component addresses inside the sequence.
     *
     * @param sequence that contains the component addresses.
     * @return list of component addresses inside the sequence
     * @throws IllegalArgumentException if there was an error on the parsing
     */
    public static List<ComponentAddress> retrieveComponentAddress(final ASN1Sequence sequence)
            throws IllegalArgumentException {
        List<ComponentAddress> addresses;
        addresses = new ArrayList<>();

        if (sequence.size() > CONFIGMAX) {
            throw new IllegalArgumentException("Component identifier contains invalid number "
                    + "of component addresses.");
        }
        //Get the components
        for (int i = 0; i < sequence.size(); i++) {
            ASN1Sequence address = ASN1Sequence.getInstance(sequence.getObjectAt(i));
            addresses.add(new ComponentAddress(address));
        }

        return Collections.unmodifiableList(addresses);
    }

    /**
     * @return indicates the type of platform certificate
     */
    public boolean isVersion2() {
        return false;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ComponentIdentifier{");
        sb.append("componentManufacturer=").append(componentManufacturer.getString());
        sb.append(", componentModel=").append(componentModel.getString());
        //Optional not null values
        sb.append(", componentSerial=");
        if (componentSerial != null) {
            sb.append(componentSerial.getString());
        }
        sb.append(", componentRevision=");
        if (componentRevision != null) {
            sb.append(componentRevision.getString());
        }
        sb.append(", componentManufacturerId=");
        if (componentManufacturerId != null) {
            sb.append(componentManufacturerId.getId());
        }
        sb.append(", fieldReplaceable=");
        if (fieldReplaceable != null) {
            sb.append(fieldReplaceable.toString());
        }
        sb.append(", componentAddress=");
        if (componentAddress.size() > 0) {
            sb.append(componentAddress
                    .stream()
                    .map(Object::toString)
                    .collect(Collectors.joining(",")));
        }
        sb.append(", certificateIdentifier=");
        sb.append("}");

        return sb.toString();
    }
}
