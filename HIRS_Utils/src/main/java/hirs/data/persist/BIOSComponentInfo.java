package hirs.data.persist;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Class to hold BIOS/UEFI Component information.
 */
@Entity
@DiscriminatorValue(value = ComponentInfo.ComponentTypeEnum.Values.BIOS_UEFI)
public class BIOSComponentInfo extends ComponentInfo {
    /**
     * Default constructor required by Hibernate.
     */
    public BIOSComponentInfo() {
    }

    /**
     * Constructor.
     *
     * @param componentManufacturer Component Manufacturer (must not be null)
     * @param componentModel        Component Model (must not be null)
     * @param componentRevision     Component Revision or Version (can be null)
     */
    public BIOSComponentInfo(final String componentManufacturer,
                             final String componentModel,
                             final String componentRevision) {
        super(componentManufacturer, componentModel, null,
                componentRevision);
    }
}
