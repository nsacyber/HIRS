package hirs.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Class for testing the HexUtils class.
 */
public class HexUtilsTest {

    /**
     * Tests that we can convert a hex string to a byte array.
     */
    @Test
    public void testHexStringToByteArray() {
        final String testString = "abcd1234";
        final byte[] expectedBytes = {-85, -51, 18, 52};
        final byte[] actualBytes = HexUtils.hexStringToByteArray(testString);
        assertArrayEquals(expectedBytes, actualBytes);
    }

    /**
     * Tests that we can convert a hex string to a byte array.
     */
    @Test
    public void testByteArrayToHexString() {
        final byte[] byteArray = {-85, -51, 18, 52};
        final String expectedString = "abcd1234";
        final String actualString = HexUtils.byteArrayToHexString(byteArray);
        assertEquals(expectedString, actualString);
    }

    /**
     * Tests that we can convert a hex string to a byte array.
     */
    @Test
    public void testByteArrayToHexStringConditional() {
        final byte[] byteArray = {-85, -51, 1, 0};
        final String expectedHexString = "abcd0100";
        final String actualHexString = HexUtils.byteArrayToHexString(byteArray);
        assertEquals(expectedHexString, actualHexString);
    }

    /**
     * Tests that a hex string can be converted to an integer.
     */
    @Test
    public void testHexToInt() {
        final String testString = "ff";
        final int expectedInt = HexUtils.FF_BYTE;
        final Integer actualInt = HexUtils.hexToInt(testString);
        assertEquals(expectedInt, (int) actualInt);
    }

    /**
     * Tests the subarray utility method.
     */
    @Test
    public void testSubarray() {
        final byte[] b = {-85, -51, 18, 52};
        final byte[] expectedSubArray = {-51, 18};
        final byte[] actualSubArray = HexUtils.subarray(b, 1, 2);
        assertArrayEquals(expectedSubArray, actualSubArray);
    }


}
