package hirs.data.persist.certificate;

import hirs.data.persist.AbstractEntity;

import javax.persistence.Entity;
import java.util.Objects;
import java.util.UUID;

/**
 *
 */
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

    /**
     * Default constructor that initializes the parameters and mismatched flag
     * is set based on expected vs actual.
     * @param certificateId associated certificate
     * @param componentHash int value of the component hash
     * @param expected the string for the expected
     * @param actual the string for the actual
     */
    public ComponentResult(final UUID certificateId, final int componentHash,
                           final String expected, final String actual) {
        this.certificateId = certificateId;
        this.componentHash = componentHash;
        this.expected = expected;
        this.actual = actual;
        this.mismatched = Objects.equals(expected, actual);
    }

    /**
     * Getter for the associated certificate UUID.
     * @return the UUID idea value
     */
    public UUID getCertificateId() {
        return certificateId;
    }

    /**
     * Getter for the component hash.
     * @return int value hash
     */
    public int getComponentHash() {
        return componentHash;
    }

    /**
     * Getter for the expected string.
     * @return the expected value
     */
    public String getExpected() {
        return expected;
    }

    /**
     * Getter for the actual string.
     * @return the actual value
     */
    public String getActual() {
        return actual;
    }

    /**
     * The flag for the actual and expected matching vs or not.
     * @return the flag for the values
     */
    public boolean isMismatched() {
        return mismatched;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ComponentResult that = (ComponentResult) o;
        return componentHash == that.componentHash
                && mismatched == that.mismatched
                && Objects.equals(certificateId, that.certificateId)
                && Objects.equals(expected, that.expected)
                && Objects.equals(actual, that.actual);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), certificateId, componentHash,
                expected, actual, mismatched);
    }

    /**
     * A string format of the expected and actual.
     * @return a formatted string
     */
    public String toString() {
        return String.format("ComponentResult[%d]: expected=[%s] actual=[%s]",
                componentHash, expected, actual);
    }
}
