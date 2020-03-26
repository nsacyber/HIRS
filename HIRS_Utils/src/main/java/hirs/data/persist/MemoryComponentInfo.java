package hirs.data.persist;

import hirs.data.persist.enums.ComponentType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Class to hold memory component information.
 */
@Entity
@DiscriminatorValue(value = ComponentType.Values.MEMORY)
public class MemoryComponentInfo extends ComponentInfo {
    /**
     * Default constructor required by Hibernate.
     */
    public MemoryComponentInfo() {
    }

    /**
     * Constructor.
     *
     * @param componentManufacturer Component Manufacturer (must not be null)
     * @param componentModel        Component Model (must not be null)
     * @param componentSerial       Component Serial Number (can be null)
     * @param componentRevision     Component Revision or Version (can be null)
     */
    public MemoryComponentInfo(final String componentManufacturer,
                               final String componentModel,
                               final String componentSerial,
                               final String componentRevision) {
        super(componentManufacturer, componentModel,
                componentSerial, componentRevision);
    }
}
