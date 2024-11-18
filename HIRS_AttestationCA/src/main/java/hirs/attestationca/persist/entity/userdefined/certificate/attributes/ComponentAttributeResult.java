package hirs.attestationca.persist.entity.userdefined.certificate.attributes;

import hirs.attestationca.persist.entity.ArchivableEntity;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * This is tied to the ComponentResult class.  If a component has a mismatched
 * value from what the device has listed, this class represents which attribute
 * of that component mismatched.
 * <p>
 * If this is a delta issue, the component ID would be set to null if the
 * remove or modified don't exist.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ComponentAttributeResult extends ArchivableEntity {

    private UUID componentId;

    @Setter
    private UUID provisionSessionId;

    // this is used to identify Revision for the ignore policy
    @Setter
    private String attribute;

    private String expectedValue;

    private String actualValue;

    /**
     * Default constructor that populates the expected and actual values.
     *
     * @param componentId   id associated with component result
     * @param expectedValue platform certificate value
     * @param actualValue   paccor value from the device
     */
    public ComponentAttributeResult(final UUID componentId,
                                    final String expectedValue,
                                    final String actualValue) {
        this.componentId = componentId;
        this.expectedValue = expectedValue;
        this.actualValue = actualValue;
    }

    /**
     * Default constructor that populates the expected and actual values.
     *
     * @param componentId        id associated with component result
     * @param provisionSessionId an id for the associated provision
     * @param expectedValue      platform certificate value
     * @param actualValue        paccor value from the device
     */
    public ComponentAttributeResult(final UUID componentId,
                                    final UUID provisionSessionId,
                                    final String expectedValue,
                                    final String actualValue) {
        this.componentId = componentId;
        this.expectedValue = expectedValue;
        this.actualValue = actualValue;
    }

    /**
     * This method is used to check the mismatched status flag for
     * displaying red if there is a failure.
     *
     * @return true if there is status match, false otherwise
     */
    public boolean checkMatchedStatus() {
        return this.actualValue.equals(this.expectedValue);
    }

    /**
     * For the state of the object, this shouldn't be negative.
     *
     * @return the string value of the attribute name
     */
    public String getAttribute() {
        if (attribute == null) {
            attribute = "";
        }

        return attribute;
    }
}
