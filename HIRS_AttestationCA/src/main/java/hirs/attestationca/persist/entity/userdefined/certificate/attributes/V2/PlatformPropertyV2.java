package hirs.attestationca.persist.entity.userdefined.certificate.attributes.V2;

import hirs.attestationca.persist.entity.userdefined.certificate.attributes.PlatformProperty;
import lombok.Getter;
import lombok.Setter;
import org.bouncycastle.asn1.ASN1Enumerated;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1UTF8String;

/**
 * Basic class that handles a single property for the platform configuration.
 * <pre>
 * Properties ::= SEQUENCE {
 *      propertyName UTF8String (SIZE (1..STRMAX)),
 *      propertyValue UTF8String (SIZE (1..STRMAX),
 *      status [0] IMPLICIT AttributeStatus OPTIONAL }
 *
 * </pre>
 */
@Setter
@Getter
public class PlatformPropertyV2 extends PlatformProperty {

    private AttributeStatus attributeStatus;

    /**
     * Default constructor.
     */
    public PlatformPropertyV2() {
        super();
        this.attributeStatus = AttributeStatus.EMPTY_STATUS;
    }

    /**
     * Constructor given the name and value for the platform property.
     *
     * @param propertyName    string containing the property name
     * @param propertyValue   string containing the property value
     * @param attributeStatus enumerated object with the status of the property
     */
    public PlatformPropertyV2(final ASN1UTF8String propertyName, final ASN1UTF8String propertyValue,
                              final AttributeStatus attributeStatus) {
        super(propertyName, propertyValue);
        this.attributeStatus = attributeStatus;
    }

    /**
     * Constructor given the SEQUENCE that contains the name and value for the
     * platform property.
     *
     * @param sequence containing the name and value of the platform property
     * @throws IllegalArgumentException if there was an error on the parsing
     */
    public PlatformPropertyV2(final ASN1Sequence sequence) throws IllegalArgumentException {
        // Check if the sequence contains the two values required
        if (sequence.size() < IDENTIFIER_NUMBER) {
            throw new IllegalArgumentException("Platform properties does not contain all "
                    + "the required fields.");
        }

        setPropertyName(ASN1UTF8String.getInstance(sequence.getObjectAt(0)));
        setPropertyValue(ASN1UTF8String.getInstance(sequence.getObjectAt(1)));

        // optional value which is a placeholder for now
        if (sequence.size() > IDENTIFIER_NUMBER
                && sequence.getObjectAt(2) instanceof ASN1Enumerated) {
            ASN1Enumerated enumerated = ASN1Enumerated.getInstance(sequence.getObjectAt(2));
            this.attributeStatus = AttributeStatus.values()[enumerated.getValue().intValue()];
        }
    }

    /**
     * @return true if the property has been modified.
     */
    public final boolean isModified() {
        return getAttributeStatus() == AttributeStatus.MODIFIED;
    }

    /**
     * @return true if the property has been removed.
     */
    public final boolean isRemoved() {
        return getAttributeStatus() != AttributeStatus.REMOVED;
    }


    /**
     * Creates a string representation of the PlatformPropertyV2 object.
     *
     * @return a string representation of the PlatformPropertyV2 object
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("PlatformPropertyV2{");
        sb.append("PropertyName=").append(getPropertyName().getString());
        sb.append(", propertyValue=").append(getPropertyValue().getString());
        if (attributeStatus != null) {
            sb.append(", attributeStatus=").append(attributeStatus);
        }
        sb.append("}");

        return sb.toString();
    }
}
