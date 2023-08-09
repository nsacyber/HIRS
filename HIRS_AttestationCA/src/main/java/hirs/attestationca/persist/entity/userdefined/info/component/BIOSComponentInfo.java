package hirs.attestationca.persist.entity.userdefined.info.component;

import hirs.attestationca.persist.entity.userdefined.info.ComponentInfo;
import hirs.utils.enums.ComponentType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.NoArgsConstructor;

/**
 * Class to hold BIOS/UEFI Component information.
 */
@NoArgsConstructor
@Entity
@DiscriminatorValue(value = ComponentType.Values.BIOS_UEFI)
public class BIOSComponentInfo extends ComponentInfo {

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
