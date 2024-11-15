package hirs.attestationca.persist.entity.userdefined.certificate.attributes;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1UTF8String;

/**
 * Basic class that handle component addresses from the component identifier.
 * <pre>
 * componentAddress ::= SEQUENCE {
 *      addressType AddressType,
 *      addressValue UTF8String (SIZE (1..STRMAX)) }
 * where STRMAX is 256
 * </pre>
 */
@Getter
@Setter
@AllArgsConstructor
public class ComponentAddress {

    /**
     * Number of identifiers that a component address must have.
     */
    public static final int IDENTIFIER_NUMBER = 2;

    private static final String ETHERNET_MAC = "2.23.133.17.1";

    private static final String WLAN_MAC = "2.23.133.17.2";

    private static final String BLUETOOTH_MAC = "2.23.133.17.3";

    private ASN1ObjectIdentifier addressType;

    private ASN1UTF8String addressValue;

    private String addressTypeString;

    private String addressValueString;

    /**
     * Default constructor.
     */
    public ComponentAddress() {
        addressType = null;
        addressValue = null;
    }

    /**
     * Constructor given the SEQUENCE that contains the type and value for the
     * component address.
     *
     * @param sequence containing the type and value for the component address
     * @throws IllegalArgumentException if there was an error on the parsing
     */
    public ComponentAddress(final ASN1Sequence sequence) throws IllegalArgumentException {
        //Check if the sequence contains the two values required
        if (sequence.size() != IDENTIFIER_NUMBER) {
            throw new IllegalArgumentException("Component address does not contain "
                    + "all the required fields.");
        }
        addressType = ASN1ObjectIdentifier.getInstance(sequence.getObjectAt(0));
        addressValue = ASN1UTF8String.getInstance(sequence.getObjectAt(1));
    }

    /**
     * Get the string value for the address type.
     *
     * @return the string value for the address type
     */
    public String getAddressTypeValue() {
        return switch (this.addressType.getId()) {
            case ETHERNET_MAC -> "ethernet mac";
            case WLAN_MAC -> "wlan mac";
            case BLUETOOTH_MAC -> "bluetooth mac";
            default -> "unknown mac";
        };
    }


    /**
     * Creates a string representation of the Component Address object.
     *
     * @return a string representation of the Component Address object.
     */
    @Override
    public String toString() {
        return "ComponentAddress{"
                + "addressType=" + addressType.getId()
                + ", addressValue=" + addressValue.getString()
                + '}';
    }
}
