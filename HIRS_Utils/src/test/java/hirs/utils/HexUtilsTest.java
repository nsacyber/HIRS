package hirs.utils;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Class for testing the HexUtils class.
 */
public class HexUtilsTest {

    /**
     * Tests that we can convert a hex string to a byte array.
     */
    @Test
    public void testHexStringToByteArray() {
        String s = "abcd1234";
        byte[] target = {-85, -51, 18, 52};

        byte[] b = HexUtils.hexStringToByteArray(s);
        Assert.assertEquals(b, target);
    }

    /**
     * Tests that we can convert a hex string to a byte array.
     */
    @Test
    public void testByteArrayToHexString() {
        String target = "abcd1234";
        byte[] b = {-85, -51, 18, 52};

        String s = HexUtils.byteArrayToHexString(b);
        Assert.assertEquals(s, target);
    }

    /**
     * Tests that we can convert a hex string to a byte array.
     */
    @Test
    public void testByteArrayToHexStringConditional() {
        String target = "abcd0100";
        byte[] b = {-85, -51, 1, 0};

        String s = HexUtils.byteArrayToHexString(b);
        Assert.assertEquals(s, target);
    }

    /**
     * Tests that a hex string can be converted to an integer.
     */
    @Test
    public void testHexToInt() {
        String s = "ff";
        Integer i = HexUtils.hexToInt(s);
        Assert.assertEquals((int) i, HexUtils.FF_BYTE);
    }

    /**
     * Tests the subarray utility method.
     */
    @Test
    public void testSubarray() {
        byte[] b = {-85, -51, 18, 52};
        byte[] target = {-51, 18};

        byte[] sub = HexUtils.subarray(b, 1, 2);

        Assert.assertEquals(sub, target);
    }



}
