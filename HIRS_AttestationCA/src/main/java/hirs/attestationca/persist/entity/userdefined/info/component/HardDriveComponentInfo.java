package hirs.attestationca.persist.entity.userdefined.info.component;

import hirs.attestationca.persist.entity.userdefined.info.ComponentInfo;
import hirs.utils.enums.ComponentType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.NoArgsConstructor;

/**
 * Class to hold hard drive component information.
 */
@NoArgsConstructor
@Entity
@DiscriminatorValue(value = ComponentType.Values.HARD_DRIVE)
public class HardDriveComponentInfo extends ComponentInfo {

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
