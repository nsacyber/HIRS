package hirs.utils;

import org.bouncycastle.util.Arrays;

import java.math.BigInteger;

/**
 * Utilities for working with hex strings and byte arrays.
 */
public final class HexUtils {

    private HexUtils() { }

    /**
     * Needs to be removed.
     * @param in  byte array to reverse
     * @return reversed byte array
     */
   public static byte[] leReverseByte(final byte[] in) {
        return Arrays.reverse(in);
    }

    /**
     * Needs to be removed.
     * @param in  byte array to reverse
     * @return integer that represents the reversed byte array
     */
    public static int leReverseInt(final byte[] in) {
        byte[] finished = leReverseByte(in);
        return new BigInteger(finished).intValue();
    }
}
