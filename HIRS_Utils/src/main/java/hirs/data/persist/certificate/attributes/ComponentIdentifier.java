package hirs.data.persist.certificate.attributes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.bouncycastle.asn1.ASN1Boolean;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERUTF8String;

/**
 * Basic class that handle component identifiers from the Platform Configuration
 * Attribute.
 * <pre>
 * ComponentIdentifier ::= SEQUENCE {
 *      componentClass
 *           SEQUENCE(SIZE(1..CONFIGMAX)),
 *      componentManufacturer UTF8String (SIZE (1..STRMAX)),
 *      componentModel UTF8String (SIZE (1..STRMAX)),
 *      componentSerial[0] IMPLICIT UTF8String (SIZE (1..STRMAX)) OPTIONAL,
 *      componentRevision [1] IMPLICIT UTF8String (SIZE (1..STRMAX)) OPTIONAL,
 *      componentManufacturerId [2] IMPLICIT PrivateEnterpriseNumber OPTIONAL,
 *      fieldReplaceable [3] IMPLICIT BOOLEAN OPTIONAL,
 *      componentAddress [4] IMPLICIT
 *          SEQUENCE(SIZE(1..CONFIGMAX)) OF ComponentAddress OPTIONAL }
 * where STRMAX is 256, CONFIGMAX is 32
 * </pre>
 */
public class ComponentIdentifier {

    /**
     * Maximum number of configurations.
     */
    public static final int CONFIGMAX = 32;

    private static final int COMPONENT_SERIAL = 0;
    private static final int COMPONENT_REVISION = 1;
    private static final int COMPONENT_IDENTIFIER = 1;
    private static final int COMPONENT_MANUFACTURER_ID = 2;
    private static final int FIELD_REPLACEABLE = 3;
    private static final int COMPONENT_ADDRESS = 4;
    private static final int COMPONENT_UNK = 5;
    private static final int COMPONENT_URI = 6;
    private static final int COMPONENT_BYTE = 7;

    private String componentClass;
    private DERUTF8String componentManufacturer;
    private DERUTF8String componentModel;
    private DERUTF8String componentSerial;
    private DERUTF8String componentRevision;
    private ASN1ObjectIdentifier componentManufacturerId;
    private ASN1Boolean fieldReplaceable;
    private List<ComponentAddress> componentAddress;

    /**
     * Default constructor.
     */
    public ComponentIdentifier() {
        componentClass = null;
        componentManufacturer = null;
        componentModel = null;
        componentSerial = null;
        componentRevision = null;
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
    public ComponentIdentifier(final DERUTF8String componentManufacturer,
            final DERUTF8String componentModel,
            final DERUTF8String componentSerial,
            final DERUTF8String componentRevision,
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
        //Check if it have a valid number of identifers
        if (sequence.size() < ComponentAddress.IDENTIFIER_NUMBER) {
            throw new IllegalArgumentException("Component identifier do not have required values.");
        }

        ASN1Sequence componentIdSeq;
        int tag = 0;

        if (sequence.getObjectAt(tag) instanceof ASN1Sequence) {
            componentIdSeq = ASN1Sequence.getInstance(sequence.getObjectAt(tag++));
            ComponentClass cc = new ComponentClass(DEROctetString.getInstance(
                    componentIdSeq.getObjectAt(COMPONENT_IDENTIFIER))
                    .toString());
            componentClass = cc.toString();
        } else if (sequence.getObjectAt(tag) instanceof DEROctetString) {
            componentClass = sequence.getObjectAt(tag++).toString();
        }

        //Mandatory values
        componentManufacturer = DERUTF8String.getInstance(sequence.getObjectAt(tag++));
        componentModel = DERUTF8String.getInstance(sequence.getObjectAt(tag++));

        //Optional values (default to null or empty)
        componentSerial = null;
        componentRevision = null;
        componentManufacturerId = null;
        fieldReplaceable = null;
        componentAddress = new ArrayList<>();

        //Continue reading the sequence if it does contain more than 2 values
        for (int i = tag; i < sequence.size(); i++) {
            ASN1TaggedObject taggedObj = ASN1TaggedObject.getInstance(sequence.getObjectAt(i));
            switch (taggedObj.getTagNo()) {
                case COMPONENT_SERIAL:
                    componentSerial = DERUTF8String.getInstance(taggedObj, false);
                    break;
                case COMPONENT_REVISION:
                    componentRevision = DERUTF8String.getInstance(taggedObj, false);
                    break;
                case COMPONENT_MANUFACTURER_ID:
                    componentManufacturerId = ASN1ObjectIdentifier.getInstance(taggedObj, false);
                    break;
                case FIELD_REPLACEABLE:
                    fieldReplaceable = ASN1Boolean.getInstance(taggedObj, false);
                    break;
                case COMPONENT_ADDRESS:
                    ASN1Sequence addressesSequence = ASN1Sequence.getInstance(taggedObj, false);
                    componentAddress = retriveComponentAddress(addressesSequence);
                    break;
                case COMPONENT_UNK:
                case COMPONENT_URI:
                case COMPONENT_BYTE:
                    break;
                default:
                    throw new IllegalArgumentException("Component identifier contains "
                            + "invalid tagged object.");
            }
        }
    }

    /**
     * @return the componentClass
     */
    public String getComponentClass() {
        return componentClass;
    }

    /**
     * @param componentClass the componentClass to set
     */
    public void setComponentClass(final String componentClass) {
        this.componentClass = componentClass;
    }

    /**
     * @return the componentManufacturer
     */
    public DERUTF8String getComponentManufacturer() {
        return componentManufacturer;
    }

    /**
     * @param componentManufacturer the componentManufacturer to set
     */
    public void setComponentManufacturer(final DERUTF8String componentManufacturer) {
        this.componentManufacturer = componentManufacturer;
    }

    /**
     * @return the componentModel
     */
    public DERUTF8String getComponentModel() {
        return componentModel;
    }

    /**
     * @param componentModel the componentModel to set
     */
    public void setComponentModel(final DERUTF8String componentModel) {
        this.componentModel = componentModel;
    }

    /**
     * @return the componentSerial
     */
    public DERUTF8String getComponentSerial() {
        return componentSerial;
    }

    /**
     * @param componentSerial the componentSerial to set
     */
    public void setComponentSerial(final DERUTF8String componentSerial) {
        this.componentSerial = componentSerial;
    }

    /**
     * @return the componentRevision
     */
    public DERUTF8String getComponentRevision() {
        return componentRevision;
    }

    /**
     * @param componentRevision the componentRevision to set
     */
    public void setComponentRevision(final DERUTF8String componentRevision) {
        this.componentRevision = componentRevision;
    }

    /**
     * @return the componentManufacturerId
     */
    public ASN1ObjectIdentifier getComponentManufacturerId() {
        return componentManufacturerId;
    }

    /**
     * @param componentManufacturerId the componentManufacturerId to set
     */
    public void setComponentManufacturerId(final ASN1ObjectIdentifier componentManufacturerId) {
        this.componentManufacturerId = componentManufacturerId;
    }

    /**
     * @return the fieldReplaceable
     */
    public ASN1Boolean getFieldReplaceable() {
        return fieldReplaceable;
    }

    /**
     * @param fieldReplaceable the fieldReplaceable to set
     */
    public void setFieldReplaceable(final ASN1Boolean fieldReplaceable) {
        this.fieldReplaceable = fieldReplaceable;
    }

    /**
     * @return the componentAddress
     */
    public final List<ComponentAddress> getComponentAddress() {
        return Collections.unmodifiableList(componentAddress);
    }

    /**
     * @param componentAddress the componentAddress to set
     */
    public void setComponentAddress(final List<ComponentAddress> componentAddress) {
        this.componentAddress = componentAddress;
    }

    /**
     * Get all the component addresses inside the sequence.
     *
     * @param sequence that contains the component addresses.
     * @return list of component addresses inside the sequence
     * @throws IllegalArgumentException if there was an error on the parsing
     */
    public static List<ComponentAddress> retriveComponentAddress(final ASN1Sequence sequence)
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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ComponentIdentifier{");
        sb.append("componentClass=").append(componentClass);
        sb.append(", componentManufacturer=")
                .append(componentManufacturer.getString());
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
        sb.append("}");

        return sb.toString();
    }
}
