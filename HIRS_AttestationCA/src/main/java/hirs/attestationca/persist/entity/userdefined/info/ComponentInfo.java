package hirs.attestationca.persist.entity.userdefined.info;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.xml.bind.annotation.XmlElement;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;

/**
 * ComponentInfo is a class to hold Hardware component information
 * such as manufacturer, model, serial number and version.
 */
@Log4j2
@NoArgsConstructor
@Data
@Entity
@DiscriminatorColumn(name = "componentTypeEnum", discriminatorType = DiscriminatorType.STRING)
public class ComponentInfo implements Serializable {

    @Id
    @Column(name = "componentInfo_id")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

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
     * Constructor.
     * @param componentManufacturer Component Manufacturer (must not be null)
     * @param componentModel Component Model (must not be null)
     * @param componentSerial Component Serial Number (can be null)
     * @param componentRevision Component Revision or Version (can be null)
     */
    public ComponentInfo(final String componentManufacturer,
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
        this.componentManufacturer = componentManufacturer.trim();
        this.componentModel = componentModel.trim();
        if (componentSerial != null) {
            this.componentSerial = componentSerial.trim();
        } else {
            this.componentSerial = StringUtils.EMPTY;
        }
        if (componentRevision != null) {
            this.componentRevision = componentRevision.trim();
        } else {
            this.componentRevision = StringUtils.EMPTY;
        }
    }

    /**
     * Constructor.
     * @param componentManufacturer Component Manufacturer (must not be null)
     * @param componentModel Component Model (must not be null)
     * @param componentSerial Component Serial Number (can be null)
     * @param componentRevision Component Revision or Version (can be null)
     * @param componentClass Component Class (can be null)
     */
    public ComponentInfo(final String componentManufacturer,
                         final String componentModel,
                         final String componentSerial,
                         final String componentRevision,
                         final String componentClass) {
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
        this.componentManufacturer = componentManufacturer.trim();
        this.componentModel = componentModel.trim();
        if (componentSerial != null) {
            this.componentSerial = componentSerial.trim();
        } else {
            this.componentSerial = StringUtils.EMPTY;
        }
        if (componentRevision != null) {
            this.componentRevision = componentRevision.trim();
        } else {
            this.componentRevision = StringUtils.EMPTY;
        }

        if (componentClass != null) {
            this.componentClass = componentClass;
        } else {
            this.componentClass = StringUtils.EMPTY;
        }
    }

    /**
     * Determines whether the given properties represent a
     * ComponentInfo that will be useful in validation.
     * Currently, only components which have a non-null
     * manufacturer and model are considered valid.
     *
     * @param componentManufacturer a String containing a component's manufacturer
     * @param componentModel a String representing a component's model
     * @param componentSerial a String representing a component's serial number
     * @param componentRevision a String representing a component's revision
     * @return true if the component is valid, false if not
     */
    public static boolean isComplete(final String componentManufacturer,
                                     final String componentModel,
                                     final String componentSerial,
                                     final String componentRevision) {
        return (StringUtils.isEmpty(componentManufacturer)
                || StringUtils.isEmpty(componentModel));
    }
}
