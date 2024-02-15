package hirs.attestationca.persist.entity.userdefined.certificate;

import hirs.attestationca.persist.entity.AbstractEntity;
import hirs.attestationca.persist.entity.userdefined.certificate.attributes.ComponentIdentifier;
import hirs.attestationca.persist.entity.userdefined.certificate.attributes.V2.AttributeStatus;
import hirs.attestationca.persist.entity.userdefined.certificate.attributes.V2.ComponentIdentifierV2;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigInteger;

/**
 * A component result is a DO to hold the status of a component validation status.  This will
 * also be used to display this common information on the certificate details page.
 */
@EqualsAndHashCode(callSuper=false)
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ComponentResult extends AbstractEntity {

    private BigInteger certificateSerialNumber;
    private String expected;
    private String actual;
    private boolean mismatched;

    // embedded component info
    private String manufacturer;
    private String model;
    private String serialNumber;
    private String revisionNumber;
    private boolean fieldReplaceable;
    // this is a string because component class doesn't inherit serializable.
    private String componentClass;
    private AttributeStatus attributeStatus;


    /**
     * Default constructor.
     * @param certificateSerialNumber associated platform certificate serial number.
     * @param componentIdentifier object with information from the platform certificate components.
     */
    public ComponentResult(final BigInteger certificateSerialNumber,
                           final ComponentIdentifier componentIdentifier) {
        this.certificateSerialNumber = certificateSerialNumber;
        this.manufacturer = componentIdentifier.getComponentManufacturer().toString();
        this.model = componentIdentifier.getComponentModel().toString();
        this.serialNumber = componentIdentifier.getComponentSerial().toString();
        this.revisionNumber = componentIdentifier.getComponentRevision().toString();
        this.fieldReplaceable = componentIdentifier.getFieldReplaceable().isTrue();
        // V2 fields
        if (componentIdentifier.isVersion2()) {
            ComponentIdentifierV2 ciV2 = (ComponentIdentifierV2) componentIdentifier;
            this.componentClass = ciV2.getComponentClass().toString();
            this.attributeStatus = ciV2.getAttributeStatus();
        }
    }

    /**
     * The string method for log entries.
     * @return a string for the component result
     */
    public String toString() {
        if (mismatched) {
            return String.format("ComponentResult: expected=[%s] actual=[%s]",
                    expected, actual);
        } else {
            return String.format("ComponentResult: certificateSerialNumber=[%s] "
                            + "manufacturer=[%s] model=[%s] componentClass=[%s]",
                    certificateSerialNumber.toString(), manufacturer, model, componentClass);
        }
    }
}
