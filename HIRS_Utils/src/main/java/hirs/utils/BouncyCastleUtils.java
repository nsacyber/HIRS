package hirs.utils;

import lombok.extern.log4j.Log4j2;
import org.bouncycastle.asn1.x500.X500Name;

/**
 * Utilities class specific for additional Bouncy Castle functionality.
 */
@Log4j2
public final class BouncyCastleUtils {

    private static final String SEPARATOR_COMMA = ",";
    private static final String SEPARATOR_PLUS = "+";

    private BouncyCastleUtils() {
        // intentionally blank, should never be instantiated
    }

    /**
     * This method can be used to compare the distinguished names given from
     * certificates. This compare uses X500Name class in bouncy castle, which
     * compares the RDNs and not the string itself. The method will check for
     * '+' and replace them, X500Name doesn't do this.
     *
     * @param nameValue1 first general name to be used
     * @param nameValue2 second general name to be used
     * @return true if the values match based on the RDNs, false if not
     */
    public static boolean x500NameCompare(final String nameValue1, final String nameValue2) {
        if (nameValue1 == null || nameValue2 == null) {
            log.warn("Provided DN string is null.");
            return true;
        }

        boolean result = false;
        X500Name x500Name1;
        X500Name x500Name2;

        try {
            x500Name1 = new X500Name(nameValue1);
            x500Name2 = new X500Name(nameValue2);
            result = x500Name1.equals(x500Name2);
        } catch (IllegalArgumentException iaEx) {
            log.error(iaEx.toString());
        }

        return result;
    }
}
