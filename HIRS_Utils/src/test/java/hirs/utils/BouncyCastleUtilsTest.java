package hirs.utils;

import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.util.Strings;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Tests methods in the (@link BouncyCastleUtils) utility class.
 */
@Log4j2
public class BouncyCastleUtilsTest {

    private static final String VALID_RDN_STRING = "OU=PCTest,O=example.com,C=US";

    private static final String VALID_RDN_STRING_SWITCHED = "C=US,OU=PCTest,O=example.com";
    private static final String VALID_RDN_STRING_UPPERCASE = "OU=PCTEST,O=EXAMPLE.COM,C=US";
    private static final String UNEQUAL_RDN_STRING = "OU=PCTest,O=example1.com,C=US";
    private static final String MALFORMED_RDN_STRING = "OU=PCTest,OZ=example1.com,C=US";

    /**
     * True Test of x500NameCompare method, of class BouncyCastleUtils.
     */
    @Test
    public void testX500NameCompareTrue() {
        assertTrue(BouncyCastleUtils.x500NameCompare(
                VALID_RDN_STRING, VALID_RDN_STRING_SWITCHED));
        assertTrue(BouncyCastleUtils.x500NameCompare(
                VALID_RDN_STRING, VALID_RDN_STRING_UPPERCASE));
    }

    /**
     * False Test of x500NameCompare method, of class BouncyCastleUtils.
     */
    @Test
    public void testX500NameCompareFalse() {
        assertFalse(BouncyCastleUtils.x500NameCompare(
                VALID_RDN_STRING, UNEQUAL_RDN_STRING));
        // Error that aren't thrown but logged
        assertFalse(BouncyCastleUtils.x500NameCompare(VALID_RDN_STRING, Strings.EMPTY));
        assertFalse(BouncyCastleUtils.x500NameCompare(Strings.EMPTY, VALID_RDN_STRING));
        assertFalse(BouncyCastleUtils.x500NameCompare(Strings.EMPTY, Strings.EMPTY));
        assertFalse(BouncyCastleUtils.x500NameCompare(
                VALID_RDN_STRING, MALFORMED_RDN_STRING));
        assertFalse(BouncyCastleUtils.x500NameCompare(
                MALFORMED_RDN_STRING, VALID_RDN_STRING));
        assertFalse(BouncyCastleUtils.x500NameCompare(
                MALFORMED_RDN_STRING, MALFORMED_RDN_STRING));
    }
}