package hirs.attestationca.persist.entity.userdefined.certificate.attributes.V2;

import hirs.attestationca.persist.entity.userdefined.certificate.attributes.PlatformProperty;
import hirs.attestationca.persist.entity.userdefined.certificate.attributes.URIReference;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1TaggedObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Basic class that represents the Version 2 Platform Configuration used for the Platform Certificate
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
@AllArgsConstructor
public class PlatformConfigurationV2 {

    private static final int COMPONENT_IDENTIFIER = 0;

    private static final int COMPONENT_IDENTIFIER_URI = 1;

    private static final int PLATFORM_PROPERTIES = 2;

    private static final int PLATFORM_PROPERTIES_URI = 3;

    private List<ComponentIdentifierV2> componentIdentifiers;

    @Getter
    @Setter
    private URIReference componentIdentifiersUri;

    private List<PlatformProperty> platformProperties;

    @Getter
    @Setter
    private URIReference platformPropertiesUri;

    /**
     * Default constructor.
     */
    public PlatformConfigurationV2() {
        componentIdentifiers = new ArrayList<>();
        componentIdentifiersUri = null;
        platformProperties = new ArrayList<>();
        platformPropertiesUri = null;
    }

    /**
     * Constructor given the SEQUENCE that contains version 2 Platform Configuration.
     *
     * @param sequence containing the version 2 Platform Configuration.
     * @throws IllegalArgumentException if there was an error while parsing
     */
    public PlatformConfigurationV2(final ASN1Sequence sequence) throws IllegalArgumentException {
        //Default values
        setComponentIdentifiers(new ArrayList<>());
        setComponentIdentifiersUri(null);
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
                    //Get componentIdentifierURI
                    ASN1Sequence componentUri = ASN1Sequence.getInstance(taggedSequence, false);
                    //Save Component Identifier URI
                    setComponentIdentifiersUri(new URIReference(componentUri));
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

    /**
     * @return a collection of version 2 component identifiers.
     */
    public List<ComponentIdentifierV2> getComponentIdentifiers() {
        return Collections.unmodifiableList(componentIdentifiers);
    }

    /**
     * @param componentIdentifiers list of version 2 component identifiers
     */
    public void setComponentIdentifiers(
            final List<ComponentIdentifierV2> componentIdentifiers) {
        this.componentIdentifiers = new ArrayList<>(componentIdentifiers);
    }


    /**
     * Add function for the component identifier array.
     *
     * @param componentIdentifierV2 object to add
     * @return status of the add, if successful or not
     */
    protected boolean add(final ComponentIdentifierV2 componentIdentifierV2) {
        if (this.componentIdentifiers != null) {
            return this.componentIdentifiers.add(componentIdentifierV2);
        }

        return false;
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
        this.platformProperties = new ArrayList<>(platformProperties);
    }

    /**
     * Add function for the platform property array.
     *
     * @param platformProperty property object to add
     * @return status of the add, if successful or not
     */
    protected boolean add(final PlatformProperty platformProperty) {
        if (this.platformProperties != null) {
            return this.platformProperties.add(platformProperty);
        }

        return false;
    }

    /**
     * Creates a string representation of the Platform Configuration V2 object.
     *
     * @return a string representation of the Platform Configuration V2 object.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("PlatformConfigurationV2{");
        sb.append("componentIdentifiers=");
        if (!getComponentIdentifiers().isEmpty()) {
            sb.append(getComponentIdentifiers()
                    .stream()
                    .map(Object::toString)
                    .collect(Collectors.joining(",")));
        }
        sb.append(", componentIdentifiersUri=");
        if (getComponentIdentifiersUri() != null) {
            sb.append(getComponentIdentifiersUri());
        }
        sb.append(", platformProperties=");
        if (!getPlatformProperties().isEmpty()) {
            sb.append(getPlatformProperties()
                    .stream()
                    .map(Object::toString)
                    .collect(Collectors.joining(",")));
        }
        sb.append(", platformPropertiesUri=");
        if (getPlatformPropertiesUri() != null) {
            sb.append(getPlatformPropertiesUri());
        }
        sb.append("}");

        return sb.toString();
    }
}
