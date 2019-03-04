package hirs.data.persist.certificate.attributes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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
public abstract class PlatformConfiguration {

    private List<ComponentIdentifier> componentIdentifier;
    private URIReference componentIdentifierUri;
    private List<PlatformProperty> platformProperties;
    private URIReference platformPropertiesUri;

    /**
     * Default constructor.
     */
    public PlatformConfiguration() {
        this.componentIdentifier = new ArrayList<>();
        this.componentIdentifierUri = null;
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
     * Constructor given the Platform Configuration values for V2 configuration.
     *
     * @param componentIdentifier list containing all the components inside the
     *          Platform Configuration.
     * @param componentIdentifierUri object containing the URI Reference
     * @param platformProperties list containing all the properties inside the
     *          Platform Configuration.
     * @param platformPropertiesUri object containing the URI Reference
     */
    public PlatformConfiguration(final List<ComponentIdentifier> componentIdentifier,
                        final URIReference componentIdentifierUri,
                        final List<PlatformProperty> platformProperties,
                        final URIReference platformPropertiesUri) {
        this.componentIdentifier = componentIdentifier;
        this.componentIdentifierUri = componentIdentifierUri;
        this.platformProperties = platformProperties;
        this.platformPropertiesUri = platformPropertiesUri;
    }


    /**
     * @return the componentIdentifier
     */
    public List<ComponentIdentifier> getComponentIdentifier() {
        return Collections.unmodifiableList(componentIdentifier);
    }

    /**
     * Add function for the component identifier array.
     * @param componentIdentifier
     * @return status of the add, if successful or not
     */
    protected boolean add(final ComponentIdentifier componentIdentifier) {
        if (this.componentIdentifier != null) {
            return this.componentIdentifier.add(componentIdentifier);
        }

        return false;
    }

    /**
     * @param componentIdentifier the componentIdentifier to set
     */
    public void setComponentIdentifier(final List<ComponentIdentifier> componentIdentifier) {
        this.componentIdentifier = componentIdentifier;
    }

    /**
     * @return the componentIdentifierUri
     */
    public URIReference getComponentIdentifierUri() {
        return componentIdentifierUri;
    }

    /**
     * @param componentIdentifierUri the componentIdentifierUri to set
     */
    public void setComponentIdentifierUri(final URIReference componentIdentifierUri) {
        this.componentIdentifierUri = componentIdentifierUri;
    }

    /**
     * @return the platformProperties
     */
    public List<PlatformProperty> getPlatformProperties() {
        return Collections.unmodifiableList(platformProperties);
    }

    /**
     * Add function for the platform property array.
     * @param platformProperty
     * @return status of the add, if successful or not
     */
    protected boolean add(final PlatformProperty platformProperty) {
        if (this.platformProperties != null) {
            return this.platformProperties.add(platformProperty);
        }

        return false;
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
