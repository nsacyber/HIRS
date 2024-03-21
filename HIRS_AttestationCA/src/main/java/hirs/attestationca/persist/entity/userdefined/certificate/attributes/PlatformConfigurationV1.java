package hirs.attestationca.persist.entity.userdefined.certificate.attributes;

import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1TaggedObject;

import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * Basic class that handle Platform Configuration for the Platform Certificate
 * Attribute.
 * <pre>
 * PlatformConfiguration ::= SEQUENCE {
 *      componentIdentifier [0] IMPLICIT SEQUENCE(SIZE(1..CONFIGMAX)) OF
 *           ComponentIdentifier OPTIONAL,
 *      platformProperties [1] IMPLICIT SEQUENCE(SIZE(1..CONFIGMAX)) OF Properties OPTIONAL,
 *      platformPropertiesUri [2] IMPLICIT URIReference OPTIONAL }
 * </pre>
 */
public class PlatformConfigurationV1 extends PlatformConfiguration {

    private static final int COMPONENT_IDENTIFIER = 0;
    private static final int PLATFORM_PROPERTIES = 1;
    private static final int PLATFORM_PROPERTIES_URI = 2;

    /**
     * Constructor given the SEQUENCE that contains Platform Configuration.
     * @param sequence containing the Platform Configuration.
     * @throws IllegalArgumentException if there was an error on the parsing
     */
    public PlatformConfigurationV1(final ASN1Sequence sequence) throws IllegalArgumentException {

        //Default values
        setComponentIdentifier(new ArrayList<>());
        setPlatformProperties(new ArrayList<>());
        setPlatformPropertiesUri(null);

        for (int i = 0; i < sequence.size(); i++) {
            ASN1TaggedObject taggedSequence
                    = ASN1TaggedObject.getInstance(sequence.getObjectAt(i));
            //Set information based on the set tagged
            switch (taggedSequence.getTagNo()) {
                case COMPONENT_IDENTIFIER:
                    //Get componentIdentifier
                    ASN1Sequence componentConfiguration
                            = ASN1Sequence.getInstance(taggedSequence, false);

                    //Get and set all the component values
                    for (int j = 0; j < componentConfiguration.size(); j++) {
                        //DERSequence with the components
                        ASN1Sequence component
                                = ASN1Sequence.getInstance(componentConfiguration.getObjectAt(j));
                        add(new ComponentIdentifier(component));
                    }
                    break;
                case PLATFORM_PROPERTIES:
                    //Get platformProperties
                    ASN1Sequence properties = ASN1Sequence.getInstance(taggedSequence, false);

                    //Get and set all the properties values
                    for (int j = 0; j < properties.size(); j++) {
                        //DERSequence with the components
                        ASN1Sequence property = ASN1Sequence.getInstance(properties.getObjectAt(j));
                        add(new PlatformProperty(property));
                    }
                    break;
                case PLATFORM_PROPERTIES_URI:
                    //Get platformPropertiesURI
                    ASN1Sequence propertiesUri = ASN1Sequence.getInstance(taggedSequence, false);
                    //Save properties URI
                    setPlatformPropertiesUri(new URIReference(propertiesUri));
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("PlatformConfiguration{");
        sb.append("componentIdentifier=");
        if (getComponentIdentifier().size() > 0) {
            sb.append(getComponentIdentifier()
                    .stream()
                    .map(Object::toString)
                    .collect(Collectors.joining(",")));
        }
        sb.append(", platformProperties=");
        if (getPlatformProperties().size() > 0) {
            sb.append(getPlatformProperties()
                    .stream()
                    .map(Object::toString)
                    .collect(Collectors.joining(",")));
        }
        sb.append(", platformPropertiesUri=");
        if (getPlatformPropertiesUri() != null) {
            sb.append(getPlatformPropertiesUri().toString());
        }
        sb.append("}");

        return sb.toString();
    }
}
