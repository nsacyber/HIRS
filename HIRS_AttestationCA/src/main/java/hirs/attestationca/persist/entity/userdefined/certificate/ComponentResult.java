package hirs.attestationca.persist.entity.userdefined.certificate;

import hirs.attestationca.persist.entity.ArchivableEntity;
import hirs.attestationca.persist.entity.userdefined.certificate.attributes.ComponentAddress;
import hirs.attestationca.persist.entity.userdefined.certificate.attributes.ComponentIdentifier;
import hirs.attestationca.persist.entity.userdefined.certificate.attributes.V2.AttributeStatus;
import hirs.attestationca.persist.entity.userdefined.certificate.attributes.V2.ComponentIdentifierV2;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * A component result is a DO to hold the status of a component validation status.  This will
 * also be used to display this common information on the certificate details page.
 */
@Getter
@Entity
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ComponentResult extends ArchivableEntity {

    /**
     * String value for the Manufacturer title.
     */
    public static final String ATTRIBUTE_MANUFACTURER = "Manufacturer";

    /**
     * String value for the Model title.
     */
    public static final String ATTRIBUTE_MODEL = "Model";

    /**
     * String value for the Serial title.
     */
    public static final String ATTRIBUTE_SERIAL = "Serial";

    /**
     * String value for the revision title.
     */
    public static final String ATTRIBUTE_REVISION = "Revision";

    /**
     * Embedded component info.
     */
    @Setter
    private String manufacturer;

    @Setter
    private String model;

    @Setter
    private String serialNumber;

    @Setter
    private String revisionNumber;

    private boolean fieldReplaceable = false;

    // this is a string because component class doesn't inherit serializable.
    @Setter
    private String componentClassValue;

    private String componentClassRegistry;

    private String componentClassStr;

    private String componentClassType;

    private AttributeStatus attributeStatus;

    private String componentAddress;

    private boolean version2 = false;

    @Setter
    private boolean delta = false;

    @Setter
    private boolean failedValidation;

    private String certificateType;

    private String issuerDN;

    private String certificateSerialNumber;

    private String boardSerialNumber;

    private String uniformResourceIdentifier;

    /**
     * Default constructor.
     *
     * @param boardSerialNumber       associated platform certificate serial number.
     * @param certificateSerialNumber unique number associated with header info.
     * @param certificateType         type of certificate. parameter holds version 1.2 or 2.0.
     * @param componentIdentifier     object with information from the platform certificate components.
     */
    public ComponentResult(final String boardSerialNumber, final String certificateSerialNumber,
                           final String certificateType,
                           final ComponentIdentifier componentIdentifier) {
        this.boardSerialNumber = boardSerialNumber;
        this.certificateSerialNumber = certificateSerialNumber;
        this.certificateType = certificateType;
        this.manufacturer = componentIdentifier.getComponentManufacturer().toString();
        this.model = componentIdentifier.getComponentModel().toString();
        this.serialNumber = componentIdentifier.getComponentSerial().toString();
        this.revisionNumber = componentIdentifier.getComponentRevision().toString();
        if (componentIdentifier.getFieldReplaceable() != null) {
            this.fieldReplaceable = componentIdentifier.getFieldReplaceable().isTrue();
        }

        StringBuilder sb = new StringBuilder();
        for (ComponentAddress element : componentIdentifier.getComponentAddresses()) {
            sb.append(String.format("%s:%s;", element.getAddressTypeValue(),
                    element.getAddressValue().toString()));
        }
        componentAddress = sb.toString();
    }

    /**
     * @param boardSerialNumber       associated platform certificate serial number
     * @param certificateSerialNumber unique number associated with header info
     * @param certificateType         type of certificate. Parameter holds version 1.2 or 2.0.
     * @param componentIdentifierV2   version 2 component identifier
     */
    public ComponentResult(final String boardSerialNumber, final String certificateSerialNumber,
                           final String certificateType,
                           final ComponentIdentifierV2 componentIdentifierV2) {

        this.boardSerialNumber = boardSerialNumber;
        this.certificateSerialNumber = certificateSerialNumber;
        this.certificateType = certificateType;
        this.manufacturer = componentIdentifierV2.getComponentManufacturer().toString();
        this.model = componentIdentifierV2.getComponentModel().toString();
        this.serialNumber = componentIdentifierV2.getComponentSerial().toString();
        this.revisionNumber = componentIdentifierV2.getComponentRevision().toString();
        if (componentIdentifierV2.getFieldReplaceable() != null) {
            this.fieldReplaceable = componentIdentifierV2.getFieldReplaceable().isTrue();
        }

        StringBuilder sb = new StringBuilder();
        for (ComponentAddress element : componentIdentifierV2.getComponentAddresses()) {
            sb.append(String.format("%s:%s;", element.getAddressTypeValue(),
                    element.getAddressValue().toString()));
        }
        componentAddress = sb.toString();

        this.componentClassValue = componentIdentifierV2.getComponentClass().getComponentIdentifier();
        this.componentClassRegistry = componentIdentifierV2.getComponentClass().getRegistryOid();
        this.componentClassStr = componentIdentifierV2.getComponentClass().toString();
        this.componentClassType = componentIdentifierV2.getComponentClass().getRegistryType();
        this.attributeStatus = componentIdentifierV2.getAttributeStatus();
        this.version2 = true;
        if (componentIdentifierV2.getComponentPlatformCert() != null) {
            this.issuerDN = componentIdentifierV2.getComponentPlatformCert().getIssuerDN().toString();
            if (componentIdentifierV2.getComponentPlatformCertUri() != null) {
                this.uniformResourceIdentifier = componentIdentifierV2.getComponentPlatformCertUri()
                        .getUniformResourceIdentifier().toString();
            }
        }
    }

    /**
     * This method is only used by the certificate-details.jsp page. This
     * method splits the compiled string of addresses into the component address
     * object for display on the jsp page.
     *
     * @return a collection of component addresses.
     */
    public List<ComponentAddress> getComponentAddresses() {
        List<ComponentAddress> addresses = new LinkedList<>();
        ComponentAddress address;
        if (componentAddress != null && !componentAddress.isEmpty()) {
            for (String s : componentAddress.split(";", 0)) {
                address = new ComponentAddress();
                address.setAddressTypeString(s.split(":")[0]);
                address.setAddressValueString(s.split(":")[1]);
                addresses.add(address);
            }
        }
        return addresses;
    }

    /**
     * Returns a hash code that is associated with common fields for components.
     *
     * @return int value of the elements
     */
    public int hashCommonElements() {
        return Objects.hash(manufacturer,
                model, serialNumber, revisionNumber, componentClassValue, componentClassRegistry);
    }

    /**
     * The string method for log entries.
     *
     * @return a string for the component result
     */
    public String toString() {
        return String.format("ComponentResult: certificateSerialNumber=[%s] "
                        + "manufacturer=[%s] model=[%s] componentClass=[%s]",
                boardSerialNumber, manufacturer, model, componentClassValue, componentClassRegistry);
    }
}
