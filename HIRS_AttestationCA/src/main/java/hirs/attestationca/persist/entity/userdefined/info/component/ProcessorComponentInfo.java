package hirs.attestationca.persist.entity.userdefined.info.component;

import hirs.attestationca.persist.entity.userdefined.info.ComponentInfo;
import hirs.utils.enums.ComponentType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.NoArgsConstructor;

/**
 * Class to hold processor component information.
 */
@NoArgsConstructor
@Entity
@DiscriminatorValue(value = ComponentType.Values.PROCESSOR)
public class ProcessorComponentInfo extends ComponentInfo {

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
