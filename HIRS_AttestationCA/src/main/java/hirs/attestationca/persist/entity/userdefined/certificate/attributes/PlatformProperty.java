package hirs.attestationca.persist.entity.userdefined.certificate.attributes;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
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
@Getter
@Setter
@AllArgsConstructor
public class PlatformProperty {

    private static final String NOT_SPECIFIED = "Not Specified";

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
        this.propertyName = ASN1UTF8String.getInstance(NOT_SPECIFIED);
        this.propertyValue = ASN1UTF8String.getInstance(NOT_SPECIFIED);
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

    @Override
    public String toString() {
        return "PlatformProperty{"
                + "propertyName=" + propertyName.getString()
                + ", propertyValue=" + propertyValue.getString()
                + "}";
    }
}
