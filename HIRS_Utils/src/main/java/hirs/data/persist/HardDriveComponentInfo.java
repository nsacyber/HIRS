package hirs.data.persist;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Class to hold hard drive component information.
 */
@Entity
@DiscriminatorValue(value = ComponentInfo.ComponentTypeEnum.Values.HARD_DRIVE)
public class HardDriveComponentInfo extends ComponentInfo {
    /**
     * Default constructor required by Hibernate.
     */
    public HardDriveComponentInfo() {
    }

    /**
     * Constructor.
     *
     * @param componentManufacturer Component Manufacturer (must not be null)
     * @param componentModel        Component Model (must not be null)
     * @param componentSerial       Component Serial Number (can be null)
     * @param componentRevision     Component Revision or Version (can be null)
     */
    public HardDriveComponentInfo(final String componentManufacturer,
                                  final String componentModel,
                                  final String componentSerial,
                                  final String componentRevision) {
        super(componentManufacturer, componentModel,
                componentSerial, componentRevision);
    }
}
