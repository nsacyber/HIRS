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
    private String componentClass;

    /**
     * Base constructor for children.
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
        if (isComplete(
                componentManufacturer,
                componentModel,
                componentSerial,
                componentRevision)) {
            log.error("ComponentInfo: manufacturer and/or "
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
     * @param deviceName            the host machine associated with this component.
     * @param componentManufacturer Component Manufacturer (must not be null)
     * @param componentModel        Component Model (must not be null)
     * @param componentSerial       Component Serial Number (can be null)
     * @param componentRevision     Component Revision or Version (can be null)
     * @param componentClass        Component Class (can be null)
     */
    public ComponentInfo(final String deviceName,
                         final String componentManufacturer,
                         final String componentModel,
                         final String componentSerial,
                         final String componentRevision,
                         final String componentClass) {
        this(deviceName, componentManufacturer, componentModel,
                componentSerial, componentRevision);

        this.componentClass = Objects.requireNonNullElse(componentClass, StringUtils.EMPTY);
    }

    /**
     * Determines whether the given properties represent a
     * ComponentInfo that will be useful in validation.
     * Currently, only components which have a non-null
     * manufacturer and model are considered valid.
     *
     * @param componentManufacturer a String containing a component's manufacturer
     * @param componentModel        a String representing a component's model
     * @param componentSerial       a String representing a component's serial number
     * @param componentRevision     a String representing a component's revision
     * @return true if the component is valid, false if not
     */
    public static boolean isComplete(final String componentManufacturer,
                                     final String componentModel,
                                     final String componentSerial,
                                     final String componentRevision) {
        return (StringUtils.isEmpty(componentManufacturer)
                || StringUtils.isEmpty(componentModel));
    }

    /**
     * Equals for the component info that just uses this classes attributes.
     *
     * @param object the object to compare
     * @return the boolean result
     */
    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }

        ComponentInfo that = (ComponentInfo) object;
        return Objects.equals(deviceName, that.deviceName)
                && Objects.equals(componentManufacturer,
                that.componentManufacturer)
                && Objects.equals(componentModel, that.componentModel)
                && Objects.equals(componentSerial, that.componentSerial)
                && Objects.equals(componentRevision, that.componentRevision)
                && Objects.equals(componentClass, that.componentClass);
    }

    /**
     * Returns a hash code that is associated with common fields for components.
     *
     * @return int value of the elements
     */
    public int hashCommonElements() {
        return Objects.hash(componentManufacturer, componentModel,
                componentSerial, componentRevision, componentClass);
    }

    /**
     * Hash method for the attributes of this class.
     *
     * @return int value that represents this class
     */
    @Override
    public int hashCode() {
        return Objects.hash(deviceName, componentManufacturer,
                componentModel, componentSerial, componentRevision,
                componentClass);
    }
}
