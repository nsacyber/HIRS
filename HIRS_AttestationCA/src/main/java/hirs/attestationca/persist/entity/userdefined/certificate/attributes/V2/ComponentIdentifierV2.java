package hirs.attestationca.persist.entity.userdefined.certificate.attributes.V2;

import hirs.attestationca.persist.entity.userdefined.certificate.attributes.ComponentAddress;
import hirs.attestationca.persist.entity.userdefined.certificate.attributes.ComponentClass;
import hirs.attestationca.persist.entity.userdefined.certificate.attributes.ComponentIdentifier;
import hirs.attestationca.persist.entity.userdefined.certificate.attributes.URIReference;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.bouncycastle.asn1.ASN1Boolean;
import org.bouncycastle.asn1.ASN1Enumerated;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.ASN1UTF8String;
import org.bouncycastle.asn1.DERUTF8String;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Basic class that represents version 2 of the component identifiers from the Version 2
 * Platform Configuration Attribute.
 * <pre>
 * ComponentIdentifier ::= SEQUENCE {
 *      componentManufacturer UTF8String (SIZE (1..STRMAX)),
 *      componentModel UTF8String (SIZE (1..STRMAX)),
 *      componentSerial[0] IMPLICIT UTF8String (SIZE (1..STRMAX)) OPTIONAL,
 *      componentRevision [1] IMPLICIT UTF8String (SIZE (1..STRMAX)) OPTIONAL,
 *      componentManufacturerId [2] IMPLICIT PrivateEnterpriseNumber OPTIONAL,
 *      fieldReplaceable [3] IMPLICIT BOOLEAN OPTIONAL,
 *      componentAddresses [4] IMPLICIT
 *          SEQUENCE(SIZE(1..CONFIGMAX)) OF ComponentAddress OPTIONAL
 *      componentPlatformCert [5] IMPLICIT CertificateIdentifier OPTIONAL,
 *      componentPlatformCertUri [6] IMPLICIT URIReference OPTIONAL,
 *      status [7] IMPLICIT AttributeStatus OPTIONAL }
 * where STRMAX is 256, CONFIGMAX is 32
 * </pre>
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class ComponentIdentifierV2 extends ComponentIdentifier {

    private static final int MANDATORY_ELEMENTS = 3;

    // Additional optional identifiers for version 2
    private static final int COMPONENT_PLATFORM_CERT = 5;

    private static final int COMPONENT_PLATFORM_CERT_URI = 6;

    private static final int ATTRIBUTE_STATUS = 7;

    private ComponentClass componentClass;

    private CertificateIdentifier componentPlatformCert;

    private URIReference componentPlatformCertUri;

    private AttributeStatus attributeStatus;

    /**
     * Default constructor.
     */
    public ComponentIdentifierV2() {
        super();
        componentClass = new ComponentClass();
        componentPlatformCert = null;
        componentPlatformCertUri = null;
        attributeStatus = AttributeStatus.EMPTY_STATUS;
    }

    /**
     * Constructor given the components values.
     *
     * @param componentClass           represent the component type
     * @param componentManufacturer    represents the component manufacturer
     * @param componentModel           represents the component model
     * @param componentSerial          represents the component serial number
     * @param componentRevision        represents the component revision
     * @param componentManufacturerId  represents the component manufacturer ID
     * @param fieldReplaceable         represents if the component is replaceable
     * @param componentAddress         represents a list of addresses
     * @param certificateIdentifier    object representing certificate Id
     * @param componentPlatformCertUri object containing the URI Reference
     * @param attributeStatus          object containing enumerated status
     */
    public ComponentIdentifierV2(final ComponentClass componentClass,
                                 final DERUTF8String componentManufacturer,
                                 final DERUTF8String componentModel,
                                 final DERUTF8String componentSerial,
                                 final DERUTF8String componentRevision,
                                 final ASN1ObjectIdentifier componentManufacturerId,
                                 final ASN1Boolean fieldReplaceable,
                                 final List<ComponentAddress> componentAddress,
                                 final CertificateIdentifier certificateIdentifier,
                                 final URIReference componentPlatformCertUri,
                                 final AttributeStatus attributeStatus) {
        super(componentManufacturer, componentModel, componentSerial,
                componentRevision, componentManufacturerId, fieldReplaceable,
                componentAddress);
        this.componentClass = componentClass;
        // additional optional component identifiers
        this.componentPlatformCert = certificateIdentifier;
        this.componentPlatformCertUri = componentPlatformCertUri;
        this.attributeStatus = attributeStatus;
    }

    /**
     * Constructor given the SEQUENCE that contains Component Identifier.
     *
     * @param sequence containing the component identifier
     * @throws IllegalArgumentException if there was an error on the parsing
     */
    public ComponentIdentifierV2(final ASN1Sequence sequence)
            throws IllegalArgumentException {
        super();
        // Check if it has a valid number of identifiers
        if (sequence.size() < MANDATORY_ELEMENTS) {
            throw new IllegalArgumentException("Component identifier do not have required values.");
        }

        int tag = 0;
        ASN1Sequence componentIdSeq = ASN1Sequence.getInstance(sequence.getObjectAt(tag));
        componentClass = new ComponentClass(componentIdSeq.getObjectAt(tag++).toString(),
                ASN1OctetString.getInstance(componentIdSeq.getObjectAt(tag)).toString());

        // Mandatory values
        this.setComponentManufacturer(
                (DERUTF8String) ASN1UTF8String.getInstance(sequence.getObjectAt(tag++)));
        this.setComponentModel((DERUTF8String) ASN1UTF8String.getInstance(sequence.getObjectAt(tag++)));

        // Continue reading the sequence if it does contain more than 2 values
        for (int i = tag; i < sequence.size(); i++) {
            ASN1TaggedObject taggedObj = ASN1TaggedObject.getInstance(sequence.getObjectAt(i));
            switch (taggedObj.getTagNo()) {
                case COMPONENT_SERIAL:
                    this.setComponentSerial((DERUTF8String) ASN1UTF8String.getInstance(taggedObj, false));
                    break;
                case COMPONENT_REVISION:
                    this.setComponentRevision((DERUTF8String) ASN1UTF8String.getInstance(taggedObj, false));
                    break;
                case COMPONENT_MANUFACTURER_ID:
                    this.setComponentManufacturerId(ASN1ObjectIdentifier
                            .getInstance(taggedObj, false));
                    break;
                case FIELD_REPLACEABLE:
                    this.setFieldReplaceable(ASN1Boolean.getInstance(taggedObj, false));
                    break;
                case COMPONENT_ADDRESS:
                    ASN1Sequence addressesSequence = ASN1Sequence.getInstance(taggedObj, false);
                    this.setComponentAddresses(retrieveComponentAddress(addressesSequence));
                    break;
                case COMPONENT_PLATFORM_CERT:
                    ASN1Sequence ciSequence = ASN1Sequence.getInstance(taggedObj, false);
                    componentPlatformCert = new CertificateIdentifier(ciSequence);
                    break;
                case COMPONENT_PLATFORM_CERT_URI:
                    ASN1Sequence uriSequence = ASN1Sequence.getInstance(taggedObj, false);
                    this.componentPlatformCertUri = new URIReference(uriSequence);
                    break;
                case ATTRIBUTE_STATUS:
                    ASN1Enumerated enumerated = ASN1Enumerated.getInstance(taggedObj, false);
                    this.attributeStatus = AttributeStatus.values()[
                            enumerated.getValue().intValue()];
                    break;
                default:
                    throw new IllegalArgumentException("Component identifier contains "
                            + "invalid tagged object.");
            }
        }
    }


    /**
     * @return indicates the type of platform certificate.
     */
    public boolean isVersion2() {
        return true;
    }

    /**
     * Creates a string representation of the Component Identifier V2 object.
     *
     * @return a string representation of the Component Identifier V2 object.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ComponentIdentifierV2{");
        sb.append("componentClass=").append(componentClass);
        sb.append(", componentManufacturer=").append(getComponentManufacturer()
                .getString());
        sb.append(", componentModel=").append(getComponentModel().getString());
        // Optional not null values
        sb.append(", componentSerial=");
        if (getComponentSerial() != null) {
            sb.append(getComponentSerial().getString());
        }
        sb.append(", componentRevision=");
        if (getComponentRevision() != null) {
            sb.append(getComponentRevision().getString());
        }
        sb.append(", componentManufacturerId=");
        if (getComponentManufacturerId() != null) {
            sb.append(getComponentManufacturerId().getId());
        }
        sb.append(", fieldReplaceable=");
        if (getFieldReplaceable() != null) {
            sb.append(getFieldReplaceable());
        }
        sb.append(", componentAddresses=");
        if (!getComponentAddresses().isEmpty()) {
            sb.append(getComponentAddresses()
                    .stream()
                    .map(Object::toString)
                    .collect(Collectors.joining(",")));
        }
        sb.append(", componentPlatformCert=");
        if (componentPlatformCert != null) {
            sb.append(componentPlatformCert);
        }
        sb.append(", componentPlatformCertUri=");
        if (componentPlatformCertUri != null) {
            sb.append(componentPlatformCertUri);
        }
        sb.append(", status=");
        if (attributeStatus != null) {
            sb.append(attributeStatus.getValue());
        }
        sb.append("}");

        return sb.toString();
    }
}
