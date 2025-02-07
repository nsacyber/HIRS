package hirs.attestationca.persist.entity.userdefined.info;

import hirs.attestationca.persist.entity.ArchivableEntity;
import hirs.attestationca.persist.entity.userdefined.certificate.attributes.ComponentIdentifier;
import hirs.utils.enums.DeviceInfoEnums;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.xml.bind.annotation.XmlElement;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

/**
 * ComponentInfo is a class to hold Hardware component information
 * such as manufacturer, model, serial number and version.
 */
@Log4j2
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(callSuper = false)
@DiscriminatorColumn(name = "componentTypeEnum", discriminatorType = DiscriminatorType.STRING)
public class ComponentInfo extends ArchivableEntity {

//    @Id
//    @Column(name = "componentInfo_id")
//    @GeneratedValue(strategy = GenerationType.AUTO)
//    private Long id;

    @Column(nullable = false)
    private String deviceName;

    @XmlElement
    @Column(nullable = false)
    private String componentManufacturer;

    @XmlElement
    @Column(nullable = false)
    private String componentModel;

    @XmlElement
    @Column
    private String componentSerial;

    @XmlElement
    @Column
    private String componentRevision;

    @XmlElement
    @Column
    private String componentClassValue;

    @XmlElement
    @Column
    private String componentClassRegistry;

    /**
     * Constructor.
     *
     * @param componentManufacturer Component Manufacturer (must not be null)
     * @param componentModel        Component Model (must not be null)
     * @param componentSerial       Component Serial Number (can be null)
     * @param componentRevision     Component Revision or Version (can be null)
     */
    public ComponentInfo(final String componentManufacturer,
                         final String componentModel,
                         final String componentSerial,
                         final String componentRevision) {
        this(DeviceInfoEnums.NOT_SPECIFIED, componentManufacturer, componentModel,
                componentSerial, componentRevision);
    }

    /**
     * Constructor.
     *
     * @param deviceName            the host machine associated with this component. (must not be null)
     * @param componentManufacturer Component Manufacturer (must not be null)
     * @param componentModel        Component Model (must not be null)
     * @param componentSerial       Component Serial Number (can be null)
     * @param componentRevision     Component Revision or Version (can be null)
     */
    public ComponentInfo(final String deviceName,
                         final String componentManufacturer,
                         final String componentModel,
                         final String componentSerial,
                         final String componentRevision) {

        if ((StringUtils.isEmpty(componentManufacturer)
                || StringUtils.isEmpty(componentModel))) {

            log.error("Component Info's manufacturer and/or "
                    + "model can not be null");
            throw new NullPointerException("ComponentInfo: manufacturer and/or "
                    + "model can not be null");
        }

        this.deviceName = deviceName;
        this.componentManufacturer = componentManufacturer.trim();
        this.componentModel = componentModel.trim();

        if (componentSerial != null) {
            this.componentSerial = componentSerial.trim();
        } else {
            this.componentSerial = ComponentIdentifier.NOT_SPECIFIED_COMPONENT;
        }

        if (componentRevision != null) {
            this.componentRevision = componentRevision.trim();
        } else {
            this.componentRevision = ComponentIdentifier.NOT_SPECIFIED_COMPONENT;
        }
    }

    /**
     * Constructor.
     *
     * @param deviceName             the host machine associated with this component.
     * @param componentManufacturer  Component Manufacturer (must not be null)
     * @param componentModel         Component Model (must not be null)
     * @param componentSerial        Component Serial Number (can be null)
     * @param componentRevision      Component Revision or Version (can be null)
     * @param componentClassValue    Component Class Value (can be null)
     * @param componentClassRegistry Component Class Registry (can be null)
     */
    public ComponentInfo(final String deviceName,
                         final String componentManufacturer,
                         final String componentModel,
                         final String componentSerial,
                         final String componentRevision,
                         final String componentClassValue,
                         final String componentClassRegistry) {
        this(deviceName, componentManufacturer, componentModel,
                componentSerial, componentRevision);

        this.componentClassValue = Objects.requireNonNullElse(componentClassValue, StringUtils.EMPTY);
        this.componentClassRegistry = Objects.requireNonNullElse(componentClassRegistry, StringUtils.EMPTY);
    }


    /**
     * Returns a hash code that is associated with common fields for components.
     *
     * @return int value of the elements
     */
    public int hashCommonElements() {
        return Objects.hash(componentManufacturer, componentModel,
                componentSerial, componentRevision, componentClassValue, componentClassRegistry);
    }
}
