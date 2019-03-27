package hirs.data.persist.certificate.attributes.V2;

import hirs.data.persist.certificate.attributes.PlatformProperty;
import org.bouncycastle.asn1.ASN1Enumerated;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERUTF8String;

/**
 *
 * Basic class that handles a single property for the platform configuration.
 * <pre>
 * Properties ::= SEQUENCE {
 *      propertyName UTF8String (SIZE (1..STRMAX)),
 *      propertyValue UTF8String (SIZE (1..STRMAX),
 *      status [0] IMPLICIT AttributeStatus OPTIONAL }
 *
 * </pre>
 */
public class PlatformPropertyV2 extends PlatformProperty {

    /**
     * Number of identifiers for version 2.
     */
    private static final int IDENTIFIER_NUMBER = 2;

    private AttributeStatus attributeStatus;

    /**
     * Default constructor.
     */
    public PlatformPropertyV2() {
        super();
        this.attributeStatus = AttributeStatus.NOT_SPECIFIED;
    }

    /**
     * Constructor given the name and value for the platform property.
     *
     * @param propertyName string containing the property name
     * @param propertyValue string containing the property value
     * @param attributeStatus enumerated object with the status of the property
     */
    public PlatformPropertyV2(final DERUTF8String propertyName, final DERUTF8String propertyValue,
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
         //Check if the sequence contains the three values required
        if (sequence.size() < IDENTIFIER_NUMBER) {
            throw new IllegalArgumentException("Platform properties does not contain all "
                    + "the required fields.");
        }

        setPropertyName(DERUTF8String.getInstance(sequence.getObjectAt(0)));
        setPropertyValue(DERUTF8String.getInstance(sequence.getObjectAt(1)));

        // optional value which is a placeholder for now
        if (sequence.size() > IDENTIFIER_NUMBER) {
            ASN1Enumerated enumerated = ASN1Enumerated.getInstance(sequence.getObjectAt(2));
            this.attributeStatus = AttributeStatus.values()[enumerated.getValue().intValue()];
        }
    }

    /**
     * Getter for the enumerated status.
     * @return added, modified, removed status
     */
    public AttributeStatus getAttributeStatus() {
        return attributeStatus;
    }

    /**
     * Setter for the enumerated status.
     * @param attributeStatus enumerated object with the status of the property
     */
    public void setAttributeStatus(final AttributeStatus attributeStatus) {
        this.attributeStatus = attributeStatus;
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

    @Override
    public String toString() {
        return "PlatformProperty{"
                + "propertyName=" + getPropertyName().getString()
                + ", propertyValue=" + getPropertyValue().getString()
                + ", attributeStatus=" + attributeStatus.toString()
                + "}";
    }
}
