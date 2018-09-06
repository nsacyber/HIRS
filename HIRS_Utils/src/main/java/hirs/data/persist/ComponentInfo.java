package hirs.data.persist;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.DiscriminatorOptions;
import org.springframework.util.Assert;

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.xml.bind.annotation.XmlElement;
import java.io.Serializable;
import java.util.Objects;

/**
 * ComponentInfo is a class to hold Hardware component information
 * such as manufacturer, model, serial number and version.
 */
@Entity
@DiscriminatorColumn(name = "componentTypeEnum", discriminatorType = DiscriminatorType.STRING)
@DiscriminatorOptions(force = true)
public class ComponentInfo implements Serializable {

    /**
     * Identifies the type of component.
     */
    public enum ComponentTypeEnum {
        /**
         * Baseboard.
         */
        BASEBOARD(Values.BASEBOARD),

        /**
         * BIOS or UEFI.
         */
        BIOS_UEFI(Values.BIOS_UEFI),

        /**
         * Chassis.
         */
        CHASSIS(Values.CHASSIS),

        /**
         * Hard Drive.
         */
        HARD_DRIVE(Values.HARD_DRIVE),

        /**
         * Memory.
         */
        MEMORY(Values.MEMORY),

        /**
         * Network Interface Card.
         */
        NIC(Values.NIC),

        /**
         * Processor.
         */
        PROCESSOR(Values.PROCESSOR);

        /**
         * Constructor.
         * @param val string value
         */
        ComponentTypeEnum(final String val) {
            if (!this.name().equals(val)) {
                throw new IllegalArgumentException("Incorrect use of ComponentTypeEnum");
            }
        }

        /**
         * String values for use in {@link ComponentTypeEnum}.
         */
        public static class Values {
            /**
             * Baseboard.
             */
            public static final String BASEBOARD = "BASEBOARD";

            /**
             * BIOS or UEFI.
             */
            public static final String BIOS_UEFI = "BIOS_UEFI";

            /**
             * Chassis.
             */
            public static final String CHASSIS = "CHASSIS";

            /**
             * Hard Drive.
             */
            public static final String HARD_DRIVE = "HARD_DRIVE";

            /**
             * Memory.
             */
            public static final String MEMORY = "MEMORY";

            /**
             * Network Interface Card.
             */
            public static final String NIC = "NIC";

            /**
             * Processor.
             */
            public static final String PROCESSOR = "PROCESSOR";
        }
    }

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

    /**
     * Get the Component's Manufacturer.
     * @return the Component's Manufacturer
     */
    public String getComponentManufacturer() {
        return componentManufacturer;
    }

    /**
     * Get the Component's Model.
     * @return the Component's Model
     */
    public String getComponentModel() {
        return componentModel;
    }

    /**
     * Get the Component's Serial Number.
     * @return the Component's Serial Number
     */
    public String getComponentSerial() {
        return componentSerial;
    }

    /**
     * Get the Component's Revision.
     * @return the Component's Revision
     */
    public String getComponentRevision() {
        return componentRevision;
    }

    /**
     * Default constructor required by Hibernate.
     */
    public ComponentInfo() {
    }

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
        Assert.state(isComplete(
                componentManufacturer,
                componentModel,
                componentSerial,
                componentRevision
        ));
        this.componentManufacturer = componentManufacturer.trim();
        this.componentModel = componentModel.trim();
        if (componentSerial != null) {
            this.componentSerial = componentSerial.trim();
        }
        if (componentRevision != null) {
            this.componentRevision = componentRevision.trim();
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
        return !(
                StringUtils.isEmpty(componentManufacturer)  || StringUtils.isEmpty(componentModel)
        );
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ComponentInfo that = (ComponentInfo) o;
        return Objects.equals(id, that.id)
                && Objects.equals(componentManufacturer, that.componentManufacturer)
                && Objects.equals(componentModel, that.componentModel)
                && Objects.equals(componentSerial, that.componentSerial)
                && Objects.equals(componentRevision, that.componentRevision);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, componentManufacturer, componentModel,
                componentSerial, componentRevision);
    }

    @Override
    public String toString() {
        return "ComponentInfo{"
                + "componentManufacturer='" + componentManufacturer + '\''
                + ", componentModel='" + componentModel + '\''
                + ", componentSerial='" + componentSerial + '\''
                + ", componentRevision='" + componentRevision + '\''
                + '}';
    }
}
