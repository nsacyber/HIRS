package hirs.data.persist.certificate.attributes;

import hirs.data.persist.DeviceInfoReport;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1UTF8String;

/**
 *
 * Basic class that handles a single property for the platform configuration.
 * <pre>
 * Properties ::= SEQUENCE {
 *      propertyName UTF8String (SIZE (1..STRMAX)),
 *      propertyValue UTF8String (SIZE (1..STRMAX) }
 *
 * </pre>
 */
public class PlatformProperty {

    /**
     * Number of identifiers for version 1.
     */
    protected static final int IDENTIFIER_NUMBER = 2;

    private ASN1UTF8String propertyName;
    private ASN1UTF8String propertyValue;

    /**
     * Default constructor.
     */
    public PlatformProperty() {
        this.propertyName = ASN1UTF8String.getInstance(DeviceInfoReport.NOT_SPECIFIED);
        this.propertyValue = ASN1UTF8String.getInstance(DeviceInfoReport.NOT_SPECIFIED);
    }

    /**
     * Constructor given the name and value for the platform property.
     *
     * @param propertyName string containing the property name
     * @param propertyValue string containing the property value
     */
    public PlatformProperty(final ASN1UTF8String propertyName, final ASN1UTF8String propertyValue) {
        this.propertyName = propertyName;
        this.propertyValue = propertyValue;
    }

    /**
     * Constructor given the SEQUENCE that contains the name and value for the
     * platform property.
     *
     * @param sequence containing the name and value of the platform property
     * @throws IllegalArgumentException if there was an error on the parsing
     */
    public PlatformProperty(final ASN1Sequence sequence) throws IllegalArgumentException {
        // Check if the sequence contains the two values required
        if (sequence.size() != IDENTIFIER_NUMBER) {
            throw new IllegalArgumentException("Platform properties does not contain all "
                    + "the required fields.");
        }

        this.propertyName = ASN1UTF8String.getInstance(sequence.getObjectAt(0));
        this.propertyValue = ASN1UTF8String.getInstance(sequence.getObjectAt(1));
    }

    /**
     * @return the propertyName
     */
    public ASN1UTF8String getPropertyName() {
        return propertyName;
    }

    /**
     * @param propertyName the propertyName to set
     */
    public void setPropertyName(final ASN1UTF8String propertyName) {
        this.propertyName = propertyName;
    }

    /**
     * @return the propertyValue
     */
    public ASN1UTF8String getPropertyValue() {
        return propertyValue;
    }

    /**
     * @param propertyValue the propertyValue to set
     */
    public void setPropertyValue(final ASN1UTF8String propertyValue) {
        this.propertyValue = propertyValue;
    }

    @Override
    public String toString() {
        return "PlatformProperty{"
                + "propertyName=" + propertyName.getString()
                + ", propertyValue=" + propertyValue.getString()
                + "}";
    }
}
