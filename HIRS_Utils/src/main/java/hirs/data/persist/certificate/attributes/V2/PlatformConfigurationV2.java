package hirs.data.persist.certificate.attributes.V2;

import hirs.data.persist.certificate.attributes.PlatformConfiguration;
import hirs.data.persist.certificate.attributes.PlatformProperty;
import hirs.data.persist.certificate.attributes.URIReference;
import java.util.ArrayList;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1TaggedObject;

/**
 * Basic class that handle Platform Configuration for the Platform Certificate
 * Attribute.
 * <pre>
 * PlatformConfiguration ::= SEQUENCE {
 *      componentIdentifier [0] IMPLICIT SEQUENCE(SIZE(1..CONFIGMAX)) OF
 *           ComponentIdentifier OPTIONAL,
 *      componentIdentifiersUri [1] IMPLICIT URIReference OPTIONAL
 *      platformProperties [2] IMPLICIT SEQUENCE(SIZE(1..CONFIGMAX)) OF Properties OPTIONAL,
 *      platformPropertiesUri [3] IMPLICIT URIReference OPTIONAL }
 * </pre>
 */
public class PlatformConfigurationV2 extends PlatformConfiguration {

    private static final int COMPONENT_IDENTIFIER = 0;
    private static final int COMPONENT_IDENTIFIER_URI = 1;
    private static final int PLATFORM_PROPERTIES = 2;
    private static final int PLATFORM_PROPERTIES_URI = 3;

    /**
     * Constructor given the SEQUENCE that contains Platform Configuration.
     * @param sequence containing the the Platform Configuration.
     * @throws IllegalArgumentException if there was an error on the parsing
     */
    public PlatformConfigurationV2(final ASN1Sequence sequence) throws IllegalArgumentException {

        //Default values
        setComponentIdentifier(new ArrayList<>());
        setComponentIdentifierUri(null);
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
                        add(new ComponentIdentifierV2(component));
                    }
                    break;
                case COMPONENT_IDENTIFIER_URI:
                    //Get platformPropertiesURI
                    ASN1Sequence componentUri = ASN1Sequence.getInstance(taggedSequence, false);
                    //Save properties URI
                    setComponentIdentifierUri(new URIReference(componentUri));
                    break;
                case PLATFORM_PROPERTIES:
                    //Get platformProperties
                    ASN1Sequence properties = ASN1Sequence.getInstance(taggedSequence, false);

                    //Get and set all the properties values
                    for (int j = 0; j < properties.size(); j++) {
                        //DERSequence with the components
                        ASN1Sequence property = ASN1Sequence.getInstance(properties.getObjectAt(j));
                        add(new PlatformPropertyV2(property));
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
}
