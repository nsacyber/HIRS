package hirs.data.persist.info;

import hirs.data.persist.enums.ComponentType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Class to hold processor component information.
 */
@Entity
@DiscriminatorValue(value = ComponentType.Values.PROCESSOR)
public class ProcessorComponentInfo extends ComponentInfo {
    /**
     * Default constructor required by Hibernate.
     */
    public ProcessorComponentInfo() {
    }

    /**
     * Constructor.
     *
     * @param componentManufacturer Component Manufacturer (must not be null)
     * @param componentModel        Component Model (must not be null)
     * @param componentSerial       Component Serial Number (can be null)
     * @param componentRevision     Component Revision or Version (can be null)
     */
    public ProcessorComponentInfo(final String componentManufacturer,
                                  final String componentModel,
                                  final String componentSerial,
                                  final String componentRevision) {
        super(componentManufacturer, componentModel,
                componentSerial, componentRevision);
    }
}
