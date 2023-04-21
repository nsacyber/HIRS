package hirs.attestationca.persist.entity.userdefined.certificate;

import hirs.attestationca.persist.entity.AbstractEntity;
import jakarta.persistence.Entity;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Objects;
import java.util.UUID;

@EqualsAndHashCode(callSuper=false)
@Getter
@Entity
public class ComponentResult extends AbstractEntity {

    private UUID certificateId;
    private int componentHash;
    private String expected;
    private String actual;
    private boolean mismatched;

    /**
     * Hibernate default constructor
     */
    protected ComponentResult() {
    }

    public ComponentResult(final UUID certificateId, final int componentHash,
                           final String expected, final String actual) {
        this.certificateId = certificateId;
        this.componentHash = componentHash;
        this.expected = expected;
        this.actual = actual;
        this.mismatched = Objects.equals(expected, actual);
    }

    public String toString() {
        return String.format("ComponentResult[%d]: expected=[%s] actual=[%s]",
                componentHash, expected, actual);
    }
}
