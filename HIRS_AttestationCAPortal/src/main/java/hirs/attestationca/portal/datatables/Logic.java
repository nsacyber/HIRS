package hirs.attestationca.portal.datatables;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@AllArgsConstructor
@ToString
public enum Logic {
    CONTAINS("contains"),
    EQUALS("equals"),
    DOES_NOT_CONTAIN("does not contain"),
    DOES_NOT_EQUAL("does not equal"),
    NOT_EMPTY("not empty"),
    STARTS("starts"),
    ENDS("ends"),
    EMPTY("empty");

    private final String value;

    /**
     * Converts the string to a corresponding enum value
     */

    public static Logic fromString(final String value) {
        for (Logic logic : Logic.values()) {
            if (logic.getValue().equalsIgnoreCase(value.trim())) {
                return logic;
            }
        }
        throw new IllegalArgumentException("Invalid logic value: " + value);
    }
}
