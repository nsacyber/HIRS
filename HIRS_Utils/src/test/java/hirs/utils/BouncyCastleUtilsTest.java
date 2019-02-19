package hirs.utils;

import org.apache.logging.log4j.util.Strings;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Tests methods in the (@link BouncyCastleUtils) utility class.
 */
public class BouncyCastleUtilsTest {

    private static final String VALID_RDN_STRING = "OU=PCTest,O=example.com,C=US";

    private static final String VALID_RDN_STRING_SWITCHED = "C=US,OU=PCTest,O=example.com";
    private static final String VALID_RDN_STRING_UPPERCASE = "OU=PCTEST,O=EXAMPLE.COM,C=US";
    private static final String VALID_RDN_STRING_PLUS = "OU=PCTest+O=example.com+C=US";
    private static final String UNEQUAL_RDN_STRING = "OU=PCTest,O=example1.com,C=US";
    private static final String MALFORMED_RDN_STRING = "OU=PCTest,OZ=example1.com,C=US";

    /**
     * True Test of x500NameCompare method, of class BouncyCastleUtils.
     */
    @Test
    public void testX500NameCompareTrue() {
        Assert.assertTrue(BouncyCastleUtils.x500NameCompare(
                VALID_RDN_STRING, VALID_RDN_STRING_SWITCHED));
        Assert.assertTrue(BouncyCastleUtils.x500NameCompare(
                VALID_RDN_STRING, VALID_RDN_STRING_UPPERCASE));
        Assert.assertTrue(BouncyCastleUtils.x500NameCompare(
                VALID_RDN_STRING, VALID_RDN_STRING_PLUS));
    }

    /**
     * False Test of x500NameCompare method, of class BouncyCastleUtils.
     */
    @Test
    public void testX500NameCompareFalse() {
        Assert.assertFalse(BouncyCastleUtils.x500NameCompare(
                VALID_RDN_STRING, UNEQUAL_RDN_STRING));
        // Error that aren't thrown but logged
        Assert.assertFalse(BouncyCastleUtils.x500NameCompare(VALID_RDN_STRING, Strings.EMPTY));
        Assert.assertFalse(BouncyCastleUtils.x500NameCompare(Strings.EMPTY, VALID_RDN_STRING));
        Assert.assertFalse(BouncyCastleUtils.x500NameCompare(Strings.EMPTY, Strings.EMPTY));
        Assert.assertFalse(BouncyCastleUtils.x500NameCompare(
                VALID_RDN_STRING, MALFORMED_RDN_STRING));
        Assert.assertFalse(BouncyCastleUtils.x500NameCompare(
                MALFORMED_RDN_STRING, VALID_RDN_STRING));
        Assert.assertFalse(BouncyCastleUtils.x500NameCompare(
                MALFORMED_RDN_STRING, MALFORMED_RDN_STRING));
    }

    /**
     * Null String Error Test of x500NameCompare method, of class
     * BouncyCastleUtils.
     */
    @Test
    public void testX500NameCompareNullError() {
        try {
            BouncyCastleUtils.x500NameCompare(VALID_RDN_STRING, null);
            Assert.fail("No IllegalArgumentException thrown.");
        } catch (Exception ex) {
            Assert.assertEquals(ex.getMessage(), "Provided DN string is null.");
        }

        try {
            BouncyCastleUtils.x500NameCompare(null, VALID_RDN_STRING);
            Assert.fail("No IllegalArgumentException thrown.");
        } catch (Exception ex) {
            Assert.assertEquals(ex.getMessage(), "Provided DN string is null.");
        }

        try {
            BouncyCastleUtils.x500NameCompare(null, null);
            Assert.fail("No IllegalArgumentException thrown.");
        } catch (Exception ex) {
            Assert.assertEquals(ex.getMessage(), "Provided DN string is null.");
        }
    }
}
