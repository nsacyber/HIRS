package hirs.attestationca.persist.entity.userdefined.certificate.attributes;

import hirs.attestationca.persist.entity.ArchivableEntity;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * This is tied to the ComponentResult class.  If a component has a mismatched
 * value from what the device has listed, this class represents which attribute
 * of that component mismatched.
 */
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ComponentAttributeResult  extends ArchivableEntity {

    @Getter
    private UUID componentId;
    private String expectedValue;
    private String actualValue;

    /**
     * Default constructor that populates the expected and actual values.
     * @param expectedValue platform certificate value
     * @param actualValue paccor value from the device
     */
    public ComponentAttributeResult(final UUID componentId,
                                    final String expectedValue,
                                    final String actualValue) {
        this.componentId = componentId;
        this.expectedValue = expectedValue;
        this.actualValue = actualValue;
    }

    /**
     * This method is used to check the mismatched status flag for
     * displaying red if there is a failure.
     */
    public boolean checkMatchedStatus() {
        return this.actualValue.equals(this.expectedValue);
    }
}
