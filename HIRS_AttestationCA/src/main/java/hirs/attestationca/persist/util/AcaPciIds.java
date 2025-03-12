package hirs.attestationca.persist.util;

import hirs.attestationca.persist.entity.userdefined.certificate.ComponentResult;
import hirs.attestationca.persist.entity.userdefined.certificate.attributes.ComponentIdentifier;
import hirs.attestationca.persist.entity.userdefined.certificate.attributes.V2.ComponentIdentifierV2;
import lombok.extern.log4j.Log4j2;
import org.bouncycastle.asn1.DERUTF8String;

import java.util.ArrayList;
import java.util.List;

import static hirs.utils.PciIds.translateDevice;
import static hirs.utils.PciIds.translateVendor;

/**
 * Provide Java access to PCI IDs.
 */
@Log4j2
public final class AcaPciIds {

    /**
     * The Component Class TCG Registry OID.
     */
    public static final String COMPCLASS_TCG_OID = "2.23.133.18.3.1";

    /**
     * The Component Class Value mask for NICs.
     */
    public static final String COMPCLASS_TCG_CAT_NIC = "00090000";

    /**
     * The Component Class Value mask for GFX cards.
     */
    public static final String COMPCLASS_TCG_CAT_GFX = "00050000";

    /**
     * Private constructor created to silence checkstyle error.
     */
    private AcaPciIds() {
    }

    /**
     * Iterate through all components and translate PCI hardware IDs as necessary.  It will only
     * translate ComponentIdentifierV2+ objects as it relies on Component Class information.
     *
     * @param components List of ComponentIdentifiers.
     * @return the translated list of ComponentIdentifiers.
     */
    public static List<ComponentIdentifier> translate(
            final List<ComponentIdentifier> components) {
        List<ComponentIdentifier> newList = new ArrayList<>();
        if (components != null && !components.isEmpty()) {
            for (final ComponentIdentifier component : components) {
                // V2 components should not be found alongside V1 components
                // they pass through just in case
                if (component.isVersion2()) {
                    newList.add(translate((ComponentIdentifierV2) component));
                } else {
                    newList.add(component);
                }
            }
        }
        return newList;
    }

    /**
     * Iterate through all components and translate PCI hardware IDs as necessary.  It will only
     * translate ComponentResults objects as it relies on Component Class information.
     *
     * @param componentResults List of ComponentResults.
     * @return the translated list of ComponentResults.
     */
    public static List<ComponentResult> translateResults(final List<ComponentResult> componentResults) {
        List<ComponentResult> newList = new ArrayList<>();
        if (componentResults != null && !componentResults.isEmpty()) {
            for (final ComponentResult componentResult : componentResults) {
                newList.add(translateResult(componentResult));
            }
        }

        return newList;
    }

    /**
     * Translate Vendor and Device IDs, if found, in ComponentIdentifierV2 objects.
     * It will only translate ID values, any other value will pass through.
     *
     * @param component ComponentIdentifierV2 object.
     * @return the translated ComponentIdentifierV2 object.
     */
    public static ComponentIdentifierV2 translate(final ComponentIdentifierV2 component) {
        ComponentIdentifierV2 newComponent = null;
        if (component != null) {
            newComponent = component;
            // This can be updated as we get more accurate component class registries and values
            // Component Class Registry not accessible: TCG assumed
            final String compClassValue = component.getComponentClass().getCategory();
            if (compClassValue.equals(COMPCLASS_TCG_CAT_NIC)
                    || compClassValue.equals(COMPCLASS_TCG_CAT_GFX)) {
                DERUTF8String manufacturer = (DERUTF8String) translateVendor(
                        component.getComponentManufacturer());
                DERUTF8String model = (DERUTF8String) translateDevice(
                        component.getComponentManufacturer(),
                        component.getComponentModel());

                newComponent = new ComponentIdentifierV2(component.getComponentClass(),
                        manufacturer,
                        model,
                        component.getComponentSerial(),
                        component.getComponentRevision(),
                        component.getComponentManufacturerId(),
                        component.getFieldReplaceable(),
                        component.getComponentAddresses(),
                        component.getComponentPlatformCert(),
                        component.getComponentPlatformCertUri(),
                        component.getAttributeStatus());
            }

        }
        return newComponent;
    }

    /**
     * Translate Vendor and Device IDs, if found, in ComponentResult objects.
     * It will only translate ID values, any other value will pass through.
     *
     * @param componentResult ComponentResult object.
     * @return the translated ComponentResult object.
     */
    public static ComponentResult translateResult(final ComponentResult componentResult) {
        ComponentResult newComponent = null;
        if (componentResult != null) {
            newComponent = componentResult;
            newComponent.setManufacturer(translateVendor(componentResult.getManufacturer()));
            newComponent.setModel(translateDevice(componentResult.getManufacturer(),
                    componentResult.getModel()));
        }
        return newComponent;
    }
}
