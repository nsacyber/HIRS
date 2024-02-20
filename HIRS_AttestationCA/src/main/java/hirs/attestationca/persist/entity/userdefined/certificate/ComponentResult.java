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

import java.util.LinkedList;
import java.util.List;

/**
 * A component result is a DO to hold the status of a component validation status.  This will
 * also be used to display this common information on the certificate details page.
 */
@EqualsAndHashCode(callSuper=false)
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ComponentResult extends ArchivableEntity {

    // embedded component info
    private String manufacturer;
    private String model;
    private String serialNumber;
    private String revisionNumber;
    private boolean fieldReplaceable;
    // this is a string because component class doesn't inherit serializable.
    private String componentClass;
    private AttributeStatus attributeStatus;
    private String componentAddress;
    private boolean version2 = false;
    private boolean mismatched;
    private String certificateType;

    private String issuerDN;
    private String certificateSerialNumber;
    private String boardSerialNumber;
    private String uniformResourceIdentifier;


    /**
     * Default constructor.
     * @param boardSerialNumber associated platform certificate serial number.
     * @param componentIdentifier object with information from the platform certificate components.
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
        for (ComponentAddress element : componentIdentifier.getComponentAddress()) {
            sb.append(String.format("%s:%s;",element.getAddressTypeValue(),
                    element.getAddressValue().toString()));
        }
        componentAddress = sb.toString();

        // V2 fields
        if (componentIdentifier.isVersion2()
                && componentIdentifier instanceof ComponentIdentifierV2) {
            // this is a downside of findbugs, the code is set up to indicate if a CI is V2 or not
            // but find bugs is throwing a flag because instanceof isn't being used.
            ComponentIdentifierV2 ciV2 = (ComponentIdentifierV2) componentIdentifier;
            this.componentClass = ciV2.getComponentClass().toString();
            this.attributeStatus = ciV2.getAttributeStatus();
            this.version2 = true;
            if (ciV2.getCertificateIdentifier() != null) {
                this.issuerDN = ciV2.getCertificateIdentifier().getIssuerDN().toString();
//                this.certificateSerialNumber = ciV2.getCertificateIdentifier()
//                        .getCertificateSerialNumber().toString();
                if (ciV2.getComponentPlatformUri() != null) {
                    this.uniformResourceIdentifier = ciV2.getComponentPlatformUri()
                            .getUniformResourceIdentifier().toString();
                }
            }
        }
    }

    /**
     * This method is only used by the certificate-details.jsp page. This
     * method splits the compiled string of addresses into the component address
     * object for display on the jsp page.
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
     * The string method for log entries.
     * @return a string for the component result
     */
    public String toString() {
        return String.format("ComponentResult: certificateSerialNumber=[%s] "
                        + "manufacturer=[%s] model=[%s] componentClass=[%s]",
                boardSerialNumber, manufacturer, model, componentClass);
    }
}
