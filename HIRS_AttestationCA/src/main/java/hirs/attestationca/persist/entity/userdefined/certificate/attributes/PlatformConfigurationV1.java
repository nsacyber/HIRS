package hirs.attestationca.persist.entity.userdefined.certificate.attributes;

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
 * Basic class that represents the Version 1 Platform Configuration used for the Platform Certificate
 * Attribute.
 * <pre>
 * PlatformConfiguration ::= SEQUENCE {
 *      componentIdentifiers [0] IMPLICIT SEQUENCE(SIZE(1..CONFIGMAX)) OF
 *           ComponentIdentifier OPTIONAL,
 *      platformProperties [1] IMPLICIT SEQUENCE(SIZE(1..CONFIGMAX)) OF Properties OPTIONAL,
 *      platformPropertiesUri [2] IMPLICIT URIReference OPTIONAL }
 * </pre>
 */
@AllArgsConstructor
public class PlatformConfigurationV1 {

    private static final int COMPONENT_IDENTIFIER = 0;

    private static final int PLATFORM_PROPERTIES = 1;

    private static final int PLATFORM_PROPERTIES_URI = 2;

    private List<ComponentIdentifier> componentIdentifiers;

    private List<PlatformProperty> platformProperties;

    @Getter
    @Setter
    private URIReference platformPropertiesUri;

    /**
     * Default constructor.
     */
    public PlatformConfigurationV1() {
        componentIdentifiers = new ArrayList<>();
        platformProperties = new ArrayList<>();
        platformPropertiesUri = null;
    }

    /**
     * Constructor given the SEQUENCE that contains Platform Configuration.
     *
     * @param sequence containing the Platform Configuration.
     * @throws IllegalArgumentException if there was an error on the parsing
     */
    public PlatformConfigurationV1(final ASN1Sequence sequence) throws IllegalArgumentException {

        //Default values
        setComponentIdentifiers(new ArrayList<>());
        setPlatformProperties(new ArrayList<>());
        setPlatformPropertiesUri(null);

        for (int i = 0; i < sequence.size(); i++) {
            ASN1TaggedObject taggedSequence
                    = ASN1TaggedObject.getInstance(sequence.getObjectAt(i));
            //Set information based on the set tagged
            switch (taggedSequence.getTagNo()) {
                case COMPONENT_IDENTIFIER:
                    //Get componentIdentifiers
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

    /**
     * @return list of version 1 component identifiers
     */
    public List<ComponentIdentifier> getComponentIdentifiers() {
        return Collections.unmodifiableList(componentIdentifiers);
    }

    /**
     * @param componentIdentifiers list of version 1 component identifiers
     */
    public void setComponentIdentifiers(final List<ComponentIdentifier> componentIdentifiers) {
        this.componentIdentifiers = new ArrayList<>(componentIdentifiers);
    }

    /**
     * Add function for the version 1 component identifier array.
     *
     * @param componentIdentifier object to add
     * @return status of the add, if successful or not
     */
    protected boolean add(final ComponentIdentifier componentIdentifier) {
        if (this.componentIdentifiers != null) {
            return this.componentIdentifiers.add(componentIdentifier);
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
     * Creates a string representation of the Platform Configuration V1 object.
     *
     * @return a string representation of the Platform Configuration V1 object.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("PlatformConfiguration{");
        sb.append("componentIdentifiers=");
        if (!getComponentIdentifiers().isEmpty()) {
            sb.append(getComponentIdentifiers()
                    .stream()
                    .map(Object::toString)
                    .collect(Collectors.joining(",")));
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
