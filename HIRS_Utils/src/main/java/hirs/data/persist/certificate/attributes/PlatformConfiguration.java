package hirs.data.persist.certificate.attributes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1TaggedObject;

/**
 * Basic class that handle Platform Configuration for the Platform Certificate
 * Attribute.
 * <pre>
 * PlatformConfiguration ::= SEQUENCE {
 *      componentIdentifiers [0] IMPLICIT SEQUENCE(SIZE(1..CONFIGMAX)) OF
 *           ComponentIdentifier OPTIONAL,
 *      platformProperties [1] IMPLICIT SEQUENCE(SIZE(1..CONFIGMAX)) OF Properties OPTIONAL,
 *      platformPropertiesUri [2] IMPLICIT URIReference OPTIONAL }
 * </pre>
 */
public class PlatformConfiguration {

    private static final int COMPONENT_IDENTIFIER = 0;
    private static final int PLATFORM_PROPERTIES = 1;
    private static final int PLATFORM_PROPERTIES_URI = 2;

    private List<ComponentIdentifier> componentIdentifier;
    private List<PlatformProperty> platformProperties;
    private URIReference platformPropertiesUri;

    /**
     * Default constructor.
     */
    public PlatformConfiguration() {
        this.componentIdentifier = new ArrayList<>();
        this.platformProperties = new ArrayList<>();
        this.platformPropertiesUri = null;
    }

    /**
     * Constructor given the Platform Configuration values.
     *
     * @param componentIdentifier list containing all the components inside the
     *          Platform Configuration.
     * @param platformProperties list containing all the properties inside the
     *          Platform Configuration.
     * @param platformPropertiesUri object containing the URI Reference
     */
    public PlatformConfiguration(final List<ComponentIdentifier> componentIdentifier,
                        final List<PlatformProperty> platformProperties,
                        final URIReference platformPropertiesUri) {
        this.componentIdentifier = componentIdentifier;
        this.platformProperties = platformProperties;
        this.platformPropertiesUri = platformPropertiesUri;
    }

    /**
     * Constructor given the SEQUENCE that contains Platform Configuration.
     * @param sequence containing the the Platform Configuration.
     * @throws IllegalArgumentException if there was an error on the parsing
     */
    public PlatformConfiguration(final ASN1Sequence sequence) throws IllegalArgumentException {

        //Default values
        this.componentIdentifier = new ArrayList<>();
        this.platformProperties = new ArrayList<>();
        this.platformPropertiesUri = null;

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
                        this.componentIdentifier.add(new ComponentIdentifier(component));
                    }
                    break;
                case PLATFORM_PROPERTIES:
                    //Get platformProperties
                    ASN1Sequence properties = ASN1Sequence.getInstance(taggedSequence, false);

                    //Get and set all the properties values
                    for (int j = 0; j < properties.size(); j++) {
                        //DERSequence with the components
                        ASN1Sequence property = ASN1Sequence.getInstance(properties.getObjectAt(j));
                        this.platformProperties.add(new PlatformProperty(property));
                    }
                    break;
                case PLATFORM_PROPERTIES_URI:
                    //Get platformPropertiesURI
                    ASN1Sequence propertiesUri = ASN1Sequence.getInstance(taggedSequence, false);
                    //Save properties URI
                    this.platformPropertiesUri = new URIReference(propertiesUri);
                    break;
                default:
                    break;
            }
        }
    }


    /**
     * @return the componentIdentifier
     */
    public List<ComponentIdentifier> getComponentIdentifier() {
        return Collections.unmodifiableList(componentIdentifier);
    }

    /**
     * @param componentIdentifier the componentIdentifier to set
     */
    public void setComponentIdentifier(final List<ComponentIdentifier> componentIdentifier) {
        this.componentIdentifier = componentIdentifier;
    }

    /**
     * @return the platformProperties
     */
    public List<PlatformProperty> getPlatformProperties() {
        return Collections.unmodifiableList(platformProperties);
    }

    /**
     * @param platformProperties the platformProperties to set
     */
    public void setPlatformProperties(final List<PlatformProperty> platformProperties) {
        this.platformProperties = platformProperties;
    }

    /**
     * @return the platformPropertiesUri
     */
    public URIReference getPlatformPropertiesUri() {
        return platformPropertiesUri;
    }

    /**
     * @param platformPropertiesUri the platformPropertiesUri to set
     */
    public void setPlatformPropertiesUri(final URIReference platformPropertiesUri) {
        this.platformPropertiesUri = platformPropertiesUri;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("PlatformConfiguration{");
        sb.append("componentIdentifier=");
        if (componentIdentifier.size() > 0) {
            sb.append(componentIdentifier
                        .stream()
                        .map(Object::toString)
                        .collect(Collectors.joining(",")));
        }
        sb.append(", platformProperties=");
        if (platformProperties.size() > 0) {
            sb.append(platformProperties
                        .stream()
                        .map(Object::toString)
                        .collect(Collectors.joining(",")));
        }
        sb.append(", platformPropertiesUri=");
        if (platformPropertiesUri != null) {
            sb.append(platformPropertiesUri.toString());
        }
        sb.append("}");

        return sb.toString();
    }
}
