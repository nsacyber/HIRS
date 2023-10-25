package hirs.attestationca.persist.entity.userdefined.certificate;

import hirs.utils.AbstractEntity;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;
import java.util.UUID;

@EqualsAndHashCode(callSuper=false)
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ComponentResult extends AbstractEntity {

    private UUID certificateId;
    private int componentHash;
    private String expected;
    private String actual;
    private boolean mismatched;

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
