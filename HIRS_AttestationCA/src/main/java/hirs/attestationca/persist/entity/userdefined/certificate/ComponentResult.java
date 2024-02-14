package hirs.attestationca.persist.entity.userdefined.certificate;

import hirs.attestationca.persist.entity.AbstractEntity;
import hirs.attestationca.persist.entity.userdefined.certificate.attributes.ComponentClass;
import hirs.attestationca.persist.entity.userdefined.certificate.attributes.V2.AttributeStatus;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;
import java.util.UUID;

/**
 * A component result is a DO to hold the status of a component validation status.  This will
 * also be used to display this common information on the certificate details page.
 */
@EqualsAndHashCode(callSuper=false)
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ComponentResult extends AbstractEntity {

    private UUID certificateId;
    private String expected;
    private String actual;
    private boolean mismatched;

    // embedded component info
    private String manufacturer;
    private String model;
    private String serialNumber;
    private String revisionNumber;
    private boolean fieldReplaceable;
    private ComponentClass componentClass;
    private AttributeStatus attributeStatus;

    /**
     * default constructor.
     * @param certificateId
     * @param expected
     * @param actual
     * @param manufacturer
     * @param model
     * @param serialNumber
     * @param revisionNumber
     * @param fieldReplaceable
     * @param componentClass
     * @param attributeStatus
     */
    public ComponentResult(final UUID certificateId,
                           final String expected, final String actual,
                           final String manufacturer, final String model,
                           final String serialNumber, final String revisionNumber,
                           final boolean fieldReplaceable, final ComponentClass componentClass,
                           final AttributeStatus attributeStatus) {
        this.certificateId = certificateId;
        this.expected = expected;
        this.actual = actual;
        this.mismatched = Objects.equals(expected, actual);
        this.manufacturer = manufacturer;
        this.model = model;
        this.serialNumber = serialNumber;
        this.revisionNumber = revisionNumber;
        this.fieldReplaceable = fieldReplaceable;
        this.componentClass = componentClass;
        this.attributeStatus = attributeStatus;
    }

    /**
     * default constructor.
     * @param certificateId
     * @param expected
     * @param actual
     */
    public ComponentResult(final UUID certificateId,
                           final String expected, final String actual) {
        this.certificateId = certificateId;
        this.expected = expected;
        this.actual = actual;
        this.mismatched = Objects.equals(expected, actual);
    }

    /**
     * The string method for log entries.
     * @return a string for the component result
     */
    public String toString() {
        return String.format("ComponentResult: expected=[%s] actual=[%s]",
                expected, actual);
    }
}
