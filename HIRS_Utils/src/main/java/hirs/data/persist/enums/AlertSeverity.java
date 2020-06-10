package hirs.data.persist.enums;

import javax.xml.bind.annotation.XmlType;

/**
 * The 'severity' of the <code>Alert</code>, which is a string enumeration
 * representing the predicted importance of the problem identified.
 *
 * A constructor with the enum is used to set a criticality number for each
 * severity level. Severity levels can be compared against each other by using
 * the getCriticality method.
 *
 */
@XmlType(name = "AlertSeverity")
public enum AlertSeverity {

    /**
     * Used for situations where Severity remains to be implemented or the exact
     * level has not been determined for a specific use case.
     */
    UNSPECIFIED(5),
    /**
     * Equivalent to "Ignore" or "Quiet". This is not used for general logging,
     * but for Alert level messages that, in specific cases, are not applicable
     * or can be or need to be ignored.
     */
    INFO(10),
    /**
     * Applies to a non-system critical file or condition.
     */
    LOW(15),
    /**
     * Involves a stable or system-critical file or a stable PCR value.
     */
    HIGH(25),
    /**
     * Equivalent to "Fatal". Involves Alerts so clearly indicative of malicious
     * intent that an automated response, such as network disconnection, is
     * warranted.
     */
    SEVERE(30);

    /**
     * Criticality number assigned to a severity level.
     */
    private int criticality;

    /**
     * Constructor used to set the criticality level.
     *
     * @param c criticality level
     */
    AlertSeverity(final int c) {
        criticality = c;
    }

    /**
     * Return criticality level assigned to severity level.
     *
     * @return criticality level
     */
    public int getCriticality() {
        return criticality;
    }
}
