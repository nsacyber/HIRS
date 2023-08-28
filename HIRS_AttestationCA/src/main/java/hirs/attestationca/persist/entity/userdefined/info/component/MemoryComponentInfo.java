package hirs.attestationca.persist.entity.userdefined.info.component;

import hirs.attestationca.persist.entity.userdefined.info.ComponentInfo;
import hirs.utils.enums.ComponentType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.NoArgsConstructor;

/**
 * Class to hold memory component information.
 */
@NoArgsConstructor
@Entity
@DiscriminatorValue(value = ComponentType.Values.MEMORY)
public class MemoryComponentInfo extends ComponentInfo {

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